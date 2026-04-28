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

package ru.org.linux.warning

import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.auth.AuthorizedSession
import ru.org.linux.comment.Comment
import ru.org.linux.topic.{Topic, TopicDao}
import ru.org.linux.user.{User, UserEventService, UserService}

import java.util.Date

object WarningService {
  val MaxWarningsPerHour = 5

  // число открытых warning'ов типа 'rule' для топика, при превышении которого включаются ограничения
  val TopicMaxWarnings = 2
}

@Service
class WarningService(warningDao: WarningDao, eventService: UserEventService, userService: UserService,
                     topicDao: TopicDao, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  def postWarning(topic: Topic, comment: Option[Comment], author: User, message: String,
                  warningType: WarningType): Unit = transactional() { _ =>
    val notifyList = warningType match {
      case RuleWarning | GroupWarning =>
        userService.getModerators.filter(_._2).map(_._1)
      case TagsWarning | SpellingWarning =>
        (userService.getModerators.filter(_._2).map(_._1) ++ userService.getCorrectors.filter(_._2).map(_._1)).distinct
    }

    val id = warningDao.postWarning(topicId = topic.id, commentId = comment.map(_.id), authorId = author.id,
      message = message, warningType = warningType)

    eventService.addWarningEvent(author, notifyList, topic, comment, s"[${warningType.name}] $message", warningId = id)

    if (comment.isEmpty) {
      topicDao.updateLastmod(topic.id)
      topicDao.recalcWarningsCount(topic.id)
    }
  }

  def lastWarningsCount(user: AuthorizedSession): Int = warningDao.lastWarningsCount(user.user.id)

  def prepareWarning(warnings: Seq[Warning]): Seq[PreparedWarning] =
    warnings.map { warning =>
      val text = s"[${warning.warningType.name}] ${warning.message}"

      PreparedWarning(
        postdate = new Date(warning.postdate.toEpochMilli),
        author = userService.getUserCached(warning.authorId),
        message = text,
        id = warning.id,
        closedBy = warning.closedBy.map(userService.getUserCached).orNull)
    }

  def load(topic: Topic, forModerator: Boolean): Seq[Warning] =
    warningDao.loadForTopic(topic.id, forModerator).toVector

  def load(comments: Seq[Comment]): Map[Int, Seq[Warning]] = warningDao.loadForComments(comments.map(_.id).toSet)

  def get(id: Int): Warning = warningDao.get(id)

  def clear(warning: Warning, by: AuthorizedSession): Unit = {
    warningDao.clear(warning.id, by.user.id)

    if (warning.commentId.isEmpty) {
      topicDao.updateLastmod(warning.topicId)
      topicDao.recalcWarningsCount(warning.topicId)
    }
  }
}