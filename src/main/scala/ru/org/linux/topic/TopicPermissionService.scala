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
import org.joda.time.{DateTime, Duration}
import org.springframework.stereotype.Service
import org.springframework.validation.{Errors, MapBindingResult}
import ru.org.linux.auth.{AccessViolationException, AnySession, AuthorizedSession}
import ru.org.linux.comment.{Comment, CommentReadService}
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.markup.MarkupType
import ru.org.linux.section.Section
import ru.org.linux.site.{DeleteInfo, MessageNotFoundException}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.TopicPermissionService.{POSTSCORE_HIDE_COMMENTS, POSTSCORE_UNRESTRICTED}
import ru.org.linux.user.{User, UserPermissionService, UserService}
import ru.org.linux.warning.WarningService.TopicMaxWarnings

import java.time.Instant
import java.time.temporal.ChronoUnit
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
  private val ViewDeletedScore = 100
  private val DeletePeriod = Duration.standardHours(3)
  private val ViewAfterDeleteDays = 14

  def getPostScoreInfo(postscore: Int): String = postscore match {
    case POSTSCORE_UNRESTRICTED =>
      ""
    case 50 =>
      "Закрыто добавление комментариев для недавно зарегистрированных пользователей (со score < 50)"
    case 100 | 200 | 300 | 400 | 500 =>
      "<b>Ограничение на отправку комментариев</b>: " + User.getStars(postscore, postscore, true)
    case POSTSCORE_MOD_AUTHOR =>
      "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора"
    case POSTSCORE_MODERATORS_ONLY =>
      "<b>Ограничение на отправку комментариев</b>: только для модераторов"
    case POSTSCORE_NO_COMMENTS =>
      "<b>Ограничение на отправку комментариев</b>: комментарии запрещены"
    case POSTSCORE_HIDE_COMMENTS =>
      "<b>Ограничение на отправку комментариев</b>: без комментариев"
    case POSTSCORE_REGISTERED_ONLY =>
      "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей"
    case _ =>
      "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore
  }

  private def getCommentCountRestriction(topic: Topic) = {
    if (!topic.sticky) {
      val commentCount = topic.commentCount

      if (commentCount > 3000) {
        200
      } else if (commentCount > 2000) {
        100
      } else if (commentCount > 1000) {
        50
      } else {
        POSTSCORE_UNRESTRICTED
      }
    } else {
      POSTSCORE_UNRESTRICTED
    }
  }
}

@Service
class TopicPermissionService(commentService: CommentReadService, siteConfig: SiteConfig, groupDao: GroupDao,
                             deleteInfoDao: DeleteInfoDao, userService: UserService) {
  def allowViewDeletedComments(message: Topic)(implicit currentUser: AnySession): Boolean = {
    if (!currentUser.moderator) {
      val topicForbidden = message.expired || message.draft ||
          message.postscore == TopicPermissionService.POSTSCORE_MODERATORS_ONLY ||
          message.postscore == TopicPermissionService.POSTSCORE_NO_COMMENTS ||
          message.postscore == POSTSCORE_HIDE_COMMENTS

      val userAllowed = currentUser.userOpt.exists(u => !u.isAnonymous && !u.isFrozen && u.getScore >= 100)

      !topicForbidden && userAllowed && (deleteInfoDao.scoreLoss(message.id) < 150)
    } else {
      true
    }
  }

  @throws[MessageNotFoundException]
  @throws[AccessViolationException]
  def checkView(group: Group, message: Topic, topicAuthor: User, showDeleted: Boolean)
               (implicit session: AnySession): Unit = {
    Preconditions.checkArgument(message.groupId == group.id)
    Preconditions.checkArgument(message.authorUserId == topicAuthor.getId)

    if (!session.moderator) {
      val currentUser = session.userOpt.orNull

      if (showDeleted && !allowViewDeletedComments(message)) {
        throw new MessageNotFoundException(message.id, "вы не можете смотреть удаленные комментарии")
      }

      val viewByAuthor = currentUser != null && currentUser.getId == message.authorUserId

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
            throw new MessageNotFoundException(message.id, "Сообщение удалено")
          }

          if (currentUser.getScore < TopicPermissionService.ViewDeletedScore) {
            throw new MessageNotFoundException(message.id, "Сообщение удалено")
          }

          if (topicAuthor.isModerator) {
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

      val viewByCorrector = currentUser != null && currentUser.canCorrect

      if (group.premoderated && !message.commited && topicAuthor.isAnonymous && !viewByCorrector) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть")
      }
    }
  }

  def checkCommentsAllowed(topic: Topic, errors: Errors)(implicit anySession: AnySession): Unit = {
    if (topic.deleted) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению")
    }

    if (topic.draft) {
      errors.reject(null, "Нельзя добавлять комментарии к черновику")
    }

    if (topic.expired) {
      errors.reject(null, "Сообщение уже устарело")
    }

    val group = groupDao.getGroup(topic.groupId)

    if (!isCommentsAllowed(group, topic)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему")
    }
  }

  private def getAllowAnonymousPostscore(topic: Topic) =
    if (topic.allowAnonymous) {
      TopicPermissionService.POSTSCORE_UNRESTRICTED
    } else {
      TopicPermissionService.POSTSCORE_REGISTERED_ONLY
    }

  private def getScoreLossPostscore(topic: Topic): Int = {
    if (!topic.sticky && !topic.expired) {
      val scoreLoss = deleteInfoDao.scoreLoss(topic.id)

      if (scoreLoss >= 150) {
        100
      } else if (scoreLoss >= 100) {
        50
      } else {
        POSTSCORE_UNRESTRICTED
      }
    } else {
      POSTSCORE_UNRESTRICTED
    }
  }

  private def getOpenWarningsPostscore(topic: Topic): Int =
    if (topic.openWarnings > TopicMaxWarnings) {
      100
    } else {
      POSTSCORE_UNRESTRICTED
    }

  def getPostscore(group: Group, topic: Topic): Int = Seq(topic.postscore, group.commentsRestriction,
    Section.getCommentPostscore(topic.sectionId), TopicPermissionService.getCommentCountRestriction(topic),
    getAllowAnonymousPostscore(topic), getScoreLossPostscore(topic), getOpenWarningsPostscore(topic)).max

  def getPostscore(topic: Topic): Int = {
    val group = groupDao.getGroup(topic.groupId)

    getPostscore(group, topic)
  }

  def isCommentsAllowed(group: Group, topic: Topic)(implicit anySession: AnySession): Boolean =
    isCommentsAllowedByUser(group, topic, anySession.userOpt, ignoreFrozen = false)

  def isCommentsAllowedByUser(group: Group, topic: Topic, user: Option[User], ignoreFrozen: Boolean): Boolean = {
    if (topic.deleted || topic.expired || topic.draft) {
      return false
    }

    val effectiveUser = user.getOrElse(userService.getAnonymous)

    if (effectiveUser.isBlocked || (!ignoreFrozen && effectiveUser.isFrozen)) {
      return false
    }

    val score = getPostscore(group, topic)

    if (score == TopicPermissionService.POSTSCORE_NO_COMMENTS || score == POSTSCORE_HIDE_COMMENTS) {
      return false
    }

    if (score == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      return true
    }

    if (effectiveUser.isAnonymous) {
      false
    } else {
      if (user.get.isModerator) {
        return true
      }

      if (score == TopicPermissionService.POSTSCORE_REGISTERED_ONLY) {
        return true
      }

      if (score == TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
        return false
      }

      val viewByAuthor = user.get.getId == topic.authorUserId

      if (score == TopicPermissionService.POSTSCORE_MOD_AUTHOR) {
        return viewByAuthor
      }

      viewByAuthor || user.get.getScore >= score
    }
  }

  /**
   * Проверка на права редактирования комментария.
   */
  def checkCommentsEditingAllowed(comment: Comment, topic: Topic, errors: Errors, markup: MarkupType)
                                 (implicit session: AnySession): Unit = {
    Preconditions.checkNotNull(comment)
    Preconditions.checkNotNull(topic)

    val haveAnswers = commentService.hasAnswers(comment)

    checkCommentEditableNow(comment, session.userOpt.orNull, haveAnswers, topic, errors, markup)
  }

  def getEditDeadline(comment: Comment): Option[DateTime] = {
    if (siteConfig.getCommentExpireMinutesForEdit != null && siteConfig.getCommentExpireMinutesForEdit != 0) {
      val editDeadline = new DateTime(comment.postdate).plusMinutes(siteConfig.getCommentExpireMinutesForEdit)

      Some.apply(editDeadline)
    } else {
      Option.empty
    }
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   * @return результат
   */
  def isCommentEditableNow(comment: Comment, haveAnswers: Boolean, topic: Topic,
                           markup: MarkupType)(implicit anySession: AnySession): Boolean = {
    val errors = new MapBindingResult(Map.empty.asJava, "obj")

    checkCommentsAllowed(topic, errors)
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

    if (currentUser == null || currentUser.isAnonymous) {
      errors.reject(null, "Анонимный пользователь")
    }

    val editByAuthor = currentUser != null && (currentUser.getId == comment.userid)

    /* Проверка на то, что пользователь модератор */
    val editable = currentUser != null && (currentUser.isModerator && siteConfig.isModeratorAllowedToEditComments)

    if (editable || editByAuthor) {
      /* проверка на то, что время редактирования не вышло */
      val maybeDeadline = getEditDeadline(comment)

      if (maybeDeadline.isDefined && maybeDeadline.get.isBeforeNow) {
        errors.reject(null, "Истек срок редактирования")
      }

      /* Проверка на то, что у комментария нет ответов */
      if (!siteConfig.isCommentEditingAllowedIfAnswersExists && haveAnswers) {
        errors.reject(null, "Редактирование комментариев с ответами запрещено")
      }

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      if (currentUser.getScore < siteConfig.getCommentScoreValueForEditing) {
        errors.reject(null, "У вас недостаточно прав для редактирования этого комментария")
      }

      if (!UserPermissionService.allowedFormatsJava(currentUser).contains(markup)) {
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
                           (implicit session: AnySession): Boolean = {
    val currentUser = session.userOpt.orNull

    if (comment.deleted || topic.deleted) {
      return false
    }

    if (currentUser == null || currentUser.isAnonymous) {
      return false
    }

    val deleteByAuthor = currentUser.getId == comment.userid

    val deleteDeadline = new DateTime(comment.postdate).plus(TopicPermissionService.DeletePeriod)

    currentUser.isModerator || (!topic.expired && deleteByAuthor && !haveAnswers && deleteDeadline.isAfterNow)
  }

  /**
   * Follow для ссылок автора
   *
   * @param author автор сообщения содержащего ссылку
   * @return true обычная ссылка, false - добавить rel=nofollow
   */
  def followAuthorLinks(author: User): Boolean = {
    if (author.isBlocked || author.isAnonymous || author.isFrozen) {
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
                   (implicit session: AnySession): Boolean = {
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
      (!group.premoderated || msg.commited || msg.authorUserId != User.ANONYMOUS_ID)
  }

  def canViewHistory(msg: Topic)(implicit session: AnySession): Boolean = {
    val viewer = session.userOpt.orNull

    if (viewer != null && viewer.isModerator) {
      return true
    }

    if (viewer != null && msg.authorUserId == viewer.getId) {
      return true
    }

    if (viewer != null && !msg.expired) {
      return true
    }

    false
  }

  def canPostWarning(topic: Topic, comment: Option[Comment])(implicit currentUserOpt: AnySession): Boolean = {
    !topic.deleted && !topic.expired && !topic.draft && comment.forall(!_.deleted) && currentUserOpt.opt.exists { user =>
      user.user.getScore >= 50 && !user.user.isFrozen
    }
  }

  def canViewDeletedComment(comment: Comment, deleteInfo: DeleteInfo)(implicit currentUser: AuthorizedSession): Boolean = {
    currentUser.moderator ||
      (currentUser.user.getId == comment.userid && !currentUser.user.isFrozen &&
        deleteInfo.delDate.toInstant.isAfter(Instant.now.minus(TopicPermissionService.ViewAfterDeleteDays, ChronoUnit.DAYS)))
  }
}
