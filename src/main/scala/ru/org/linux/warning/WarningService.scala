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

package ru.org.linux.warning

import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.auth.CurrentUser
import ru.org.linux.comment.Comment
import ru.org.linux.topic.Topic
import ru.org.linux.user.{User, UserEventService, UserService}

@Service
class WarningService(warningDao: WarningDao, eventService: UserEventService, userService: UserService,
                     val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  def postWarning(topic: Topic, comment: Option[Comment], author: User, message: String): Unit = transactional() { _ =>
    val moderators = userService.getModerators.filter(_._2).map(_._1)

    eventService.addWarningEvent(author, moderators, topic, comment, message)

    warningDao.postWarning(topicId = topic.id, commentId = comment.map(_.id), authorId = author.getId, message = message)
  }

  def canPostWarning(user: CurrentUser): Boolean = {
    user.moderator
  }
}