/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.topic

import com.google.common.base.Preconditions
import org.springframework.stereotype.Service
import org.springframework.validation.{Errors, MapBindingResult}
import ru.org.linux.auth.{AccessViolationException, AnySession, AuthorizedSession}
import ru.org.linux.comment.{Comment, CommentReadService}
import ru.org.linux.group.Group
import ru.org.linux.markup.MarkupType
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.rights.{AddCommentChecker, SlowModeChecker}
import ru.org.linux.site.{DeleteInfo, MessageNotFoundException}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicPermissionService.{POSTSCORE_HIDE_COMMENTS, ViewDeletedScore}
import ru.org.linux.user.{User, UserConstants, UserPermissionService}
import ru.org.linux.warning.WarningService.TopicMaxWarnings

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import javax.annotation.Nullable
import scala.jdk.CollectionConverters.MapHasAsJava

object TopicPermissionService {
  // константы используются в jsp!
  val POSTSCORE_MOD_AUTHOR = 9999
  val POSTSCORE_UNRESTRICTED: Int = -9999
  val POSTSCORE_MODERATORS_ONLY = 10000 // при смене номера поправить GroupListDao
  val POSTSCORE_NO_COMMENTS = 10001 // запрещает новые, но оставляет старые
  val POSTSCORE_HIDE_COMMENTS = 10002 // Запрещает новые, скрывает старые. Работает только при явной установке на топике
  val POSTSCORE_REGISTERED_ONLY: Int = -50

  private val LinkFollowMinScore = 100
  private val ViewDeletedScore = 200
  private val DeletePeriod = Duration.ofHours(3)
  private val ViewAfterDeleteDays = 14
}

@Service
class TopicPermissionService(commentService: CommentReadService, siteConfig: SiteConfig,
                             deleteInfoDao: DeleteInfoDao, slowModeChecker: SlowModeChecker,
                             addCommentChecker: AddCommentChecker) {
  def allowViewAllDeletedComments(message: Topic)(using currentUser: AnySession): Boolean = {
    if !currentUser.moderator then
      val topicForbidden = message.expired || message.draft ||
          message.postscore == TopicPermissionService.POSTSCORE_MODERATORS_ONLY ||
          message.postscore == TopicPermissionService.POSTSCORE_NO_COMMENTS ||
          message.postscore == POSTSCORE_HIDE_COMMENTS

      val userAllowed = currentUser.userOpt.exists(u => !u.anonymous && !u.isFrozen && u.score >= ViewDeletedScore)

      def topicAllowedByScoreLoss = deleteInfoDao.scoreLoss(message.id) < 150
      def userSlowMode = currentUser.userOpt.exists(u => slowModeChecker.check(u).restricted)

      !topicForbidden && userAllowed && topicAllowedByScoreLoss && !userSlowMode
    else
      true
  }

  @throws[MessageNotFoundException]
  @throws[AccessViolationException]
  def checkView(group: Group, message: Topic, topicAuthor: User, showDeleted: Boolean)
               (using session: AnySession): Unit = {
    Preconditions.checkArgument(message.groupId == group.id)
    Preconditions.checkArgument(message.authorUserId == topicAuthor.id)

    if (!session.moderator) {
      val currentUser = session.userOpt.orNull

      if (showDeleted && !allowViewAllDeletedComments(message)) {
        throw new AccessViolationException("вы не можете смотреть удаленные комментарии")
      }

      val viewByAuthor = currentUser != null && currentUser.id == message.authorUserId

      if (message.deleted) {
        if (message.expired) {
          throw new MessageNotFoundException(message.id, "нельзя посмотреть устаревшие удаленные сообщения")
        }

        if (!session.authorized) {
          throw new MessageNotFoundException(message.id, "Сообщение удалено")
        }

        if (!viewByAuthor) {
          val deleteExpire = deleteInfoDao.getDeleteInfo(message.id).map(_.delDate).map(_.toInstant)
            .forall(_.isBefore(Instant.now.minus(TopicPermissionService.ViewAfterDeleteDays, ChronoUnit.DAYS)))

          if (deleteExpire) {
            throw new MessageNotFoundException(message.id, "нельзя посмотреть устаревшие удаленные сообщения")
          }

          if (currentUser.isFrozen) {
            throw new AccessViolationException("Сообщение удалено")
          }

          if (currentUser.score < TopicPermissionService.ViewDeletedScore || slowModeChecker.check(currentUser).restricted) {
            throw new MessageNotFoundException(message.id, "Сообщение удалено")
          }

          if (topicAuthor.canmod) {
            throw new MessageNotFoundException(message.id, "Сообщение удалено")
          }
        }
      }

      if (message.draft) {
        if (message.expired) {
          throw new MessageNotFoundException(message.id, "Черновик устарел")
        }

        if (!viewByAuthor) {
          throw new MessageNotFoundException(message.id, "Нельзя посмотреть чужой черновик")
        }
      }

      if (!session.authorized && message.openWarnings > TopicMaxWarnings) {
        throw new MessageNotFoundException(message.id, "Сообщение скрыто")
      }
    }
  }

  /**
   * Проверка на права редактирования комментария.
   */
  def checkCommentsEditingAllowed(comment: Comment, topic: Topic, errors: Errors, markup: MarkupType)
                                 (using session: AnySession): Unit = {
    Preconditions.checkNotNull(comment)
    Preconditions.checkNotNull(topic)

    val haveAnswers = commentService.hasAnswers(comment)

    checkCommentEditableNow(comment, session.userOpt.orNull, haveAnswers, topic, errors, markup)
  }

  def getEditDeadline(comment: Comment): Option[Instant] = 
    if siteConfig.getCommentExpireMinutesForEdit != 0 then
      val editDeadline = comment.postdate.toInstant.plus(Duration.ofMinutes(siteConfig.getCommentExpireMinutesForEdit.toLong))

      Some.apply(editDeadline)
    else 
      Option.empty

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   * @return результат
   */
  def isCommentEditableNow(comment: Comment, haveAnswers: Boolean, topic: Topic,
                           markup: MarkupType)(using anySession: AnySession): Boolean = {
    val errors = new MapBindingResult(Map.empty.asJava, "obj")

    addCommentChecker.checkCommentPosting(topic).checkOrError(errors)
    checkCommentEditableNow(comment, anySession.userOpt.orNull, haveAnswers, topic, errors, markup)

    !errors.hasErrors
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   */
  private def checkCommentEditableNow(comment: Comment, @Nullable currentUser: User, haveAnswers: Boolean, topic: Topic,
                                      errors: Errors, markup: MarkupType): Unit = {
    if (comment.deleted || topic.deleted) {
      errors.reject(null, "Тема или комментарий удалены")
    }

    if (currentUser == null || currentUser.anonymous) {
      errors.reject(null, "Анонимный пользователь")
    }

    val editByAuthor = currentUser != null && (currentUser.id == comment.userid)

    /* Проверка на то, что пользователь модератор */
    val editable = currentUser != null && (currentUser.isModerator && siteConfig.isModeratorAllowedToEditComments)

    if (editable || editByAuthor) {
      /* проверка на то, что время редактирования не вышло */
      val maybeDeadline = getEditDeadline(comment)

      if (maybeDeadline.isDefined && maybeDeadline.get.isBefore(Instant.now)) {
        errors.reject(null, "Истек срок редактирования")
      }

      /* Проверка на то, что у комментария нет ответов */
      if (!siteConfig.isCommentEditingAllowedIfAnswersExists && haveAnswers) {
        errors.reject(null, "Редактирование комментариев с ответами запрещено")
      }

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      if (currentUser.score < siteConfig.getCommentScoreValueForEditing) {
        errors.reject(null, "У вас недостаточно прав для редактирования этого комментария")
      }

      if (!UserPermissionService.legacyEditableFormats(currentUser).contains(markup)) {
        errors.reject(null, "Вы не можете редактировать тексты данного формата")
      }
    } else {
      errors.reject(null, "У вас недостаточно прав для редактирования этого комментария")
    }
  }

  /**
   * Проверяем можно ли удалять комментарий на текущий момент
   *
   * @param haveAnswers у комментрия есть ответы?
   * @return резултат
   */
  def isCommentDeletableNow(comment: Comment, topic: Topic, haveAnswers: Boolean)
                           (using session: AnySession): Boolean = {
    val currentUser = session.userOpt.orNull

    if (comment.deleted || topic.deleted) {
      return false
    }

    if (currentUser == null || currentUser.anonymous) {
      return false
    }

    val deleteByAuthor = currentUser.id == comment.userid

    val deleteDeadline = comment.postdate.toInstant.plus(TopicPermissionService.DeletePeriod)

    currentUser.isModerator || (!topic.expired && deleteByAuthor && !haveAnswers && deleteDeadline.isAfter(Instant.now))
  }

  /**
   * Follow для ссылок автора
   *
   * @param author автор сообщения содержащего ссылку
   * @return true обычная ссылка, false - добавить rel=nofollow
   */
  def followAuthorLinks(author: User): Boolean = {
    if (author.blocked || author.anonymous || author.isFrozen) {
      false
    } else {
      author.getScore >= TopicPermissionService.LinkFollowMinScore
    }
  }

  /**
   * follow топиков которые подтверждены и у которых автор не заблокирован и
   * score > LinkFollowMinScore
   */
  def followInTopic(topic: Topic, author: User): Boolean = topic.commited || followAuthorLinks(author)

  def isUserCastAllowed(author: User): Boolean = author.getScore >= 0

  def isUndeletable(topic: Topic, comment: Comment, deleteInfo: Option[DeleteInfo])
                   (using session: AnySession): Boolean = {
    if (!session.authorized) {
      false
    } else if (topic.deleted || !comment.deleted || !session.moderator || topic.expired) {
      false
    } else if (comment.userid == deleteInfo.map(_.userid).getOrElse(0)) {
      false
    } else {
      true
    }
  }

  def isTopicSearchable(msg: Topic, group: Group): Boolean = {
    Preconditions.checkArgument(msg.groupId == group.id)

    !msg.deleted && !msg.draft && !msg.isCommentsHidden &&
      (!group.premoderated || msg.commited || msg.authorUserId != UserConstants.ANONYMOUS_ID)
  }

  def canViewHistory(msg: Topic)(using session: AnySession): Boolean = {
    val viewer = session.userOpt.orNull

    if (viewer != null && viewer.canmod) {
      return true
    }

    if (viewer != null && msg.authorUserId == viewer.id) {
      return true
    }

    if (viewer != null && !msg.expired) {
      return true
    }

    false
  }

  def canPostWarning(topic: Topic, comment: Option[Comment])(using currentUserOpt: AnySession): Boolean = {
    !topic.deleted && !topic.expired && !topic.draft && comment.forall(!_.deleted) && currentUserOpt.opt.exists { user =>
      user.user.getScore >= 50 && !user.user.isFrozen
    }
  }

  def canViewDeletedComment(topic: Topic, comment: Comment, deleteInfo: DeleteInfo)
                           (using currentUser: AuthorizedSession): Boolean = {
    assert(comment.topicId == topic.id)
    assert(comment.deleted)

    allowViewAllDeletedComments(topic) ||
      (currentUser.user.id == comment.userid && !currentUser.user.isFrozen &&
        deleteInfo.delDate.toInstant.isAfter(Instant.now.minus(TopicPermissionService.ViewAfterDeleteDays, ChronoUnit.DAYS)))
  }
}
