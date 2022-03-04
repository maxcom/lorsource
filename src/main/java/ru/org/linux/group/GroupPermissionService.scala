/*
 * Copyright 1998-2022 Linux.org.ru
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

import java.time.Instant
import java.time.temporal.ChronoUnit

import javax.annotation.Nullable
import org.joda.time.{DateTime, Duration}
import org.springframework.stereotype.Service
import ru.org.linux.markup.MarkupPermissions
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.{PreparedTopic, Topic, TopicPermissionService}
import ru.org.linux.user.User

@Service
object GroupPermissionService {
  private val EditSelfAlwaysScore = 200
  private val DeletePeriod = Duration.standardDays(3)
  private val EditPeriod = Duration.standardDays(14)
  private val CreateTagScore = 400
}

@Service
class GroupPermissionService(sectionService: SectionService, deleteInfoDao: DeleteInfoDao) {
  import GroupPermissionService._
  /**
    * Проверка может ли пользователь удалить топик
    *
    * @param user пользователь удаляющий сообщение
    * @return признак возможности удаления
    */
  private def isDeletableByUser(topic: Topic, user: User): Boolean = {
    if (topic.getUid != user.getId) {
      false
    } else if (topic.isDraft) {
      true
    } else {
      val deleteDeadline = new DateTime(topic.getPostdate).plus(DeletePeriod)

      deleteDeadline.isAfterNow && topic.getCommentCount == 0
    }
  }

  def isUndeletable(topic: Topic, user: User): Boolean = {
    if (!topic.isDeleted || !user.isModerator) {
      false
    } else {
      if (user.isAdministrator) {
        true
      } else if (!topic.isExpired) {
        true
      } else {
        Option(deleteInfoDao.getDeleteInfo(topic.getId))
          .filter(_.delDate != null)
          .map(_.delDate.toInstant)
          .exists(_.isAfter(Instant.now.minus(14, ChronoUnit.DAYS)))
      }
    }
  }

  private def effectivePostscore(group: Group) = {
    val section = sectionService.getSection(group.getSectionId)

    Math.max(group.getTopicRestriction, section.getTopicsRestriction)
  }

  def enableAllowAnonymousCheckbox(group: Group, @Nullable currentUser: User): Boolean = {
    currentUser!=null && !group.isPremoderated &&
      Math.max(group.getCommentsRestriction,
        Section.getCommentPostscore(group.getSectionId))<TopicPermissionService.POSTSCORE_REGISTERED_ONLY
  }

  def isTopicPostingAllowed(group: Group, @Nullable currentUser: User): Boolean = {
    val restriction = effectivePostscore(group)

    if (restriction == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      true
    } else if (currentUser == null || currentUser.isAnonymous) {
      false
    } else if (currentUser.isBlocked) {
      false
    } else if (restriction == TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      currentUser.isModerator
    } else if (restriction == TopicPermissionService.POSTSCORE_NO_COMMENTS) {
      false
    } else {
      currentUser.getScore >= restriction
    }
  }

  def isImagePostingAllowed(section: Section, @Nullable currentUser: User): Boolean = {
    if (section.isImagepost) {
      true
    } else if (currentUser != null && (currentUser.isModerator || currentUser.isCorrector || currentUser.getScore >= 50)) {
      section.isImageAllowed
    } else {
      false
    }
  }

  def getPostScoreInfo(group: Group): String = {
    val postscore = effectivePostscore(group)

    postscore match {
      case TopicPermissionService.POSTSCORE_UNRESTRICTED =>
        ""
      case 100 | 200 | 300 | 400 | 500 =>
        "<b>Ограничение на добавление сообщений</b>: " + User.getStars(postscore, postscore, true)
      case TopicPermissionService.POSTSCORE_MODERATORS_ONLY =>
        "<b>Ограничение на добавление сообщений</b>: только для модераторов"
      case TopicPermissionService.POSTSCORE_MODERATORS_ONLY =>
        "<b>Ограничение на добавление сообщений</b>: комментарии запрещены"
      case TopicPermissionService.POSTSCORE_REGISTERED_ONLY =>
        "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей"
      case _ =>
        s"<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей, score>=$postscore"
    }
  }

  def isDeletable(topic: Topic, user: User): Boolean = {
    if (user.isAdministrator) {
      true
    } else {
      val deletableByUser = isDeletableByUser(topic, user)

      if (!deletableByUser && user.isModerator) {
        isDeletableByModerator(topic, user)
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
  private def isDeletableByModerator(topic: Topic, moderator: User) = {
    val deleteDeadline = new DateTime(topic.getPostdate).plusMonths(1)

    val section = sectionService.getSection(topic.getSectionId)

    if (section.isPremoderated && !topic.isCommited) {
      true
    } else if (section.isPremoderated && topic.isCommited && deleteDeadline.isAfterNow) {
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
    * @param by    редактор
    * @return true если можно, false если нет
    */
  def isEditable(topic: PreparedTopic, @Nullable by: User): Boolean = {
    val message = topic.getMessage
    val section = topic.getSection
    val author = topic.getAuthor

    if (message.isDeleted) {
      false
    } else if (by == null || by.isAnonymous || by.isBlocked) {
      false
    } else if (message.isExpired) {
      false
    } else if (by.isAdministrator) {
      true
    } else if (!MarkupPermissions.allowedFormatsJava(by).contains(topic.getMarkupType)) {
      false
    } else if (by.isModerator) {
      true
    } else if (by.canCorrect && section.isPremoderated) {
      true
    } else if (by.getId == author.getId && !message.isCommited) {
      if (message.isSticky) {
        true
      } else if (section.isPremoderated) {
        true
      } else if (message.isDraft) {
        true
      } else if (author.getScore >= EditSelfAlwaysScore) {
        true
      } else {
        val editDeadline = new DateTime(message.getPostdate).plus(EditPeriod)

        editDeadline.isAfterNow
      }
    } else {
      false
    }
  }

  /**
    * Можно ли редактировать теги сообщения
    *
    * @param topic тема
    * @param by    редактор
    * @return true если можно, false если нет
    */
  def isTagsEditable(topic: PreparedTopic, @Nullable by: User): Boolean = {
    val message = topic.getMessage
    val section = topic.getSection
    val author = topic.getAuthor

    if (message.isDeleted) {
      false
    } else if (by == null || by.isAnonymous || by.isBlocked) {
      false
    } else if (by.isAdministrator) {
      true
    } else if (by.isModerator) {
      true
    } else if (by.canCorrect) {
      true
    } else if (by.getId == author.getId && !message.isCommited) {
      if (message.isSticky) {
        true
      } else if (message.isDraft) {
        true
      } else if (section.isPremoderated) {
        true
      } else if (author.getScore >= EditSelfAlwaysScore) {
        !message.isExpired
      } else {
        val editDeadline = new DateTime(message.getPostdate).plus(EditPeriod)

        editDeadline.isAfterNow
      }
    } else {
      false
    }
  }

  def canCreateTag(section: Section, user: User): Boolean = {
    if (section.isPremoderated) {
      true
    } else {
      user != null && user.getScore >= CreateTagScore
    }
  }

  def canCommit(user: User, topic: Topic): Boolean =
    user!=null && (user.isModerator || (user.isCorrector && topic.getUid != user.getId))
}