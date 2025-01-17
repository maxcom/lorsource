/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.group

import org.joda.time.{DateTime, Duration}
import org.springframework.stereotype.Service
import ru.org.linux.auth.{AnySession, AuthorizedSession}
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.{PreparedTopic, Topic, TopicPermissionService}
import ru.org.linux.user.{User, UserPermissionService}

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.annotation.Nullable
import scala.jdk.OptionConverters.RichOptional

@Service
object GroupPermissionService {
  private val DeletePeriod = Duration.standardDays(3)
  private val EditPeriod = Duration.standardDays(14)
  private val CreateTagScore = 200
}

@Service
class GroupPermissionService(sectionService: SectionService, deleteInfoDao: DeleteInfoDao) {
  import GroupPermissionService.*
  /**
    * Проверка может ли пользователь удалить топик
    *
    * @param user пользователь удаляющий сообщение
    * @return признак возможности удаления
    */
  private def isDeletableByUser(topic: Topic, user: User, section: Section): Boolean = {
    if (topic.authorUserId != user.getId) {
      false
    } else if (topic.draft) {
      true
    } else if (section.isPremoderated && topic.commited) {
      false
    } else {
      val deleteDeadline = new DateTime(topic.postdate).plus(DeletePeriod)

      deleteDeadline.isAfterNow && topic.commentCount == 0
    }
  }

  def isUndeletable(topic: Topic)(implicit user: AnySession): Boolean = {
    if (!topic.deleted || !user.moderator) {
      false
    } else {
      if (user.administrator) {
        true
      } else if (!topic.expired) {
        true
      } else {
        deleteInfoDao.getDeleteInfo(topic.id).toScala
          .filter(_.delDate != null)
          .map(_.delDate.toInstant)
          .exists(_.isAfter(Instant.now.minus(14, ChronoUnit.DAYS)))
      }
    }
  }

  private def effectivePostscore(group: Group) = {
    val section = sectionService.getSection(group.sectionId)

    Math.max(group.topicRestriction, section.getTopicsRestriction)
  }

  def enableAllowAnonymousCheckbox(group: Group)(implicit currentUser: AnySession): Boolean = {
    currentUser.authorized && !group.premoderated &&
      Math.max(group.commentsRestriction,
        Section.getCommentPostscore(group.sectionId))<TopicPermissionService.POSTSCORE_REGISTERED_ONLY
  }

  def isTopicPostingAllowed(section: Section)(implicit currentUser: AnySession): Boolean =
    isTopicPostingAllowed(section.getTopicsRestriction, currentUser.userOpt.orNull)

  def isTopicPostingAllowed(group: Group)(implicit currentUser: AnySession): Boolean =
    isTopicPostingAllowed(effectivePostscore(group), currentUser.userOpt.orNull)

  private def isTopicPostingAllowed(restriction: Int, @Nullable currentUser: User): Boolean = {
    if (currentUser!=null && (currentUser.isBlocked || currentUser.isFrozen)) {
      false
    } else if (restriction == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      true
    } else if (currentUser == null || currentUser.isAnonymous) {
      false
    } else if (restriction == TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      currentUser.isModerator
    } else if (restriction == TopicPermissionService.POSTSCORE_NO_COMMENTS ||
        restriction == TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
      false
    } else {
      currentUser.getScore >= restriction
    }
  }

  def isImagePostingAllowed(section: Section)(implicit currentUser: AnySession): Boolean = {
    if (section.isImagepost) {
      true
    } else if (currentUser.authorized &&
        (currentUser.moderator || currentUser.corrector || currentUser.userOpt.exists(_.getScore >= 50))) {
      section.isImageAllowed
    } else {
      false
    }
  }

  def additionalImageLimit(section: Section)(implicit currentUser: AnySession): Int = {
    if (isImagePostingAllowed(section) &&
        section.getId == Section.SECTION_GALLERY &&
        currentUser.userOpt.exists(_.getScore >= 400)) {
      3
    } else {
      0
    }
  }

  def getPostScoreInfo(group: Group): String = {
    val postscore = effectivePostscore(group)

    postscore match {
      case TopicPermissionService.POSTSCORE_UNRESTRICTED =>
        ""
      case 100 | 200 | 300 | 400 | 500 =>
        s"<b>Ограничение на добавление сообщений</b>: ${User.getStars(postscore, postscore, true)}"
      case TopicPermissionService.POSTSCORE_MODERATORS_ONLY =>
        "<b>Ограничение на добавление сообщений</b>: только для модераторов"
      case TopicPermissionService.POSTSCORE_REGISTERED_ONLY =>
        "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей"
      case _ =>
        s"<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей, score>=$postscore"
    }
  }

  def isDeletable(topic: Topic)(implicit user: AuthorizedSession): Boolean = {
    if (user.administrator) {
      true
    } else {
      val section = sectionService.getSection(topic.sectionId)

      val deletableByUser = isDeletableByUser(topic, user.user, section)

      if (!deletableByUser && user.moderator) {
        isDeletableByModerator(topic, user.user, section)
      } else {
        deletableByUser
      }
    }
  }

  /**
    * Проверка, может ли модератор удалить топик
    *
    * @param moderator пользователь удаляющий сообщение
    * @return признак возможности удаления
    */
  private def isDeletableByModerator(topic: Topic, moderator: User, section: Section) = {
    val deleteDeadline = new DateTime(topic.postdate).plusMonths(1)

    if (section.isPremoderated && !topic.commited) {
      true
    } else if (section.isPremoderated && topic.commited && deleteDeadline.isAfterNow) {
      true
    } else if (!section.isPremoderated) {
      true
    } else {
      false
    }
  }

  /**
    * Можно ли редактировать сообщения полностью
    *
    * @param topic тема
    * @return true если можно, false если нет
    */
  def isEditable(topic: PreparedTopic)(implicit session: AnySession): Boolean = {
    val by = session.userOpt.orNull

    val message = topic.message
    val section = topic.section
    val author = topic.author

    if (message.deleted) {
      false
    } else if (by == null || by.isAnonymous || by.isBlocked || by.isFrozen) {
      false
    } else if (by.isAdministrator) {
      true
    } else if (message.expired && !message.draft) {
      false
    } else if (!UserPermissionService.allowedFormatsJava(by).contains(topic.markupType)) {
      false
    } else if (by.isModerator) {
      true
    } else if (by.canCorrect && section.isPremoderated) {
      true
    } else if (by.getId == author.getId && !message.commited) {
      if (message.sticky) {
        true
      } else if (section.isPremoderated) {
        true
      } else if (message.draft) {
        true
      } else {
        val editDeadline = new DateTime(message.postdate).plus(EditPeriod)

        editDeadline.isAfterNow
      }
    } else if (by.getId == author.getId && message.commited && section.getId == Section.SECTION_ARTICLES) {
      val editDeadline = new DateTime(message.commitDate).plus(EditPeriod)

      editDeadline.isAfterNow
    } else {
      false
    }
  }

  /**
    * Можно ли редактировать теги сообщения
    *
    * @param topic тема
    * @return true если можно, false если нет
    */
  def isTagsEditable(topic: PreparedTopic)(implicit session: AnySession): Boolean = {
    val by = session.userOpt.orNull

    val message = topic.message
    val section = topic.section
    val author = topic.author

    if (message.deleted) {
      false
    } else if (by == null || by.isAnonymous || by.isBlocked || by.isFrozen) {
      false
    } else if (by.isAdministrator) {
      true
    } else if (by.isModerator) {
      true
    } else if (by.canCorrect) {
      true
    } else if (by.getId == author.getId && !message.commited) {
      if (message.sticky) {
        true
      } else if (message.draft) {
        true
      } else if (section.isPremoderated) {
        true
      } else {
        val editDeadline = new DateTime(message.postdate).plus(EditPeriod)

        editDeadline.isAfterNow
      }
    } else if (by.getId == author.getId && message.commited && section.getId == Section.SECTION_ARTICLES) {
      val editDeadline = new DateTime(message.commitDate).plus(EditPeriod)

      editDeadline.isAfterNow
    } else {
      false
    }
  }

  def canCreateTag(section: Section)(implicit session: AnySession): Boolean = {
    val user = session.userOpt.orNull

    if (section.isPremoderated && user!=null && !user.isAnonymous) {
      true
    } else {
      user != null && user.getScore >= CreateTagScore
    }
  }

  def canCommit(topic: Topic)(implicit session: AnySession): Boolean =
    session.userOpt.exists(user => user.isModerator || (user.canCorrect && topic.authorUserId != user.getId))
}