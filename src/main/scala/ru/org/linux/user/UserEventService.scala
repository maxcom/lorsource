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
package ru.org.linux.user

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import ru.org.linux.comment.Comment
import ru.org.linux.user.UserEventFilterEnum._

import java.util
import java.util.Optional
import scala.jdk.CollectionConverters._

@Service
class UserEventService(userEventDao: UserEventDao, val transactionManager: PlatformTransactionManager)
    extends StrictLogging with TransactionManagement {
  /**
   * Добавление уведомления об упоминании пользователей в комментарии.
   *
   * @param users     список пользователей. которых надо оповестить
   * @param topicId   идентификационный номер топика
   * @param commentId идентификационный номер комментария
   */
  def addUserRefEvent(users: java.lang.Iterable[User], topicId: Int, commentId: Int): Unit = {
    for (user <- users.asScala) {
      userEventDao.addEvent(REFERENCE.getType, user.getId, isPrivate = false, Some(topicId), Some(commentId), None)
    }
  }

  /**
   * Добавление уведомления об упоминании пользователей в топике.
   *
   * @param users   список пользователей. которых надо оповестить
   * @param topicId идентификационный номер топика
   */
  def addUserRefEvent(users: java.util.Set[Integer], topicId: Int): Unit = transactional() { _ =>
    userEventDao.insertTopicNotification(topicId, users.asScala)

    users.asScala.foreach { user =>
      userEventDao.addEvent(REFERENCE.getType, user, isPrivate = false, Some(topicId), None, None)
    }
  }

  def getNotifiedUsers(topicId: Int): util.Set[Integer] = userEventDao.getNotifiedUsers(topicId).toSet.asJava

  /**
   * Добавление уведомления об ответе на сообщение пользователя.
   */
  def addReplyEvent(parentAuthor: User, topicId: Int, commentId: Int): Unit =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      userEventDao.addEvent(ANSWERS.getType, parentAuthor.getId, isPrivate = false, Some(topicId), Some(commentId), None)
    }

  /**
   * Добавление уведомления о назначении тега сообщению.
   *
   * @param userIdList список ID пользователей, которых надо оповестить
   * @param topicId    идентификационный номер топика
   */
  def addUserTagEvent(userIdList: java.util.List[Integer], topicId: Int): Unit = transactional() { _ =>
    userEventDao.insertTopicNotification(topicId, userIdList.asScala)

    userIdList.asScala.foreach { userId =>
      userEventDao.addEvent(TAG.getType, userId, isPrivate = false, Some(topicId), None, None)
    }
  }

  /**
   * Очистка старых уведомлений пользователей.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   */
  def cleanupOldEvents(maxEventsPerUser: Int): Unit = {
    val oldEventsList = userEventDao.getUserIdListByOldEvents(maxEventsPerUser)

    oldEventsList.asScala.foreach { userId =>
      logger.info(s"Cleaning up messages for userid=$userId")
      userEventDao.cleanupOldEvents(userId, maxEventsPerUser)
    }
  }

  /**
   * Получить список уведомлений для пользователя.
   *
   * @param user        пользователь
   * @param showPrivate включать ли приватные
   * @param topics      кол-во уведомлений
   * @param offset      сдвиг относительно начала
   * @param eventFilter тип уведомлений
   * @return список уведомлений
   */
  def getUserEvents(user: User, showPrivate: Boolean, topics: Int, offset: Int,
                    eventFilter: UserEventFilterEnum): collection.Seq[UserEvent] = {
    val eventFilterType = if (eventFilter!=ALL) {
      Some(eventFilter.getType)
    } else {
      None
    }

    userEventDao.getRepliesForUser(user.getId, showPrivate, topics, offset, eventFilterType)
  }

  /**
   * Сброс уведомлений.
   *
   * @param user пользователь которому сбрасываем
   */
  def resetUnreadReplies(user: User, topId: Int): Unit = userEventDao.resetUnreadReplies(user.getId, topId)

  /**
   * Удаление уведомлений, относящихся к удаленным топикам
   *
   * @param msgids идентификаторы топиков
   */
  def processTopicDeleted(msgids: util.List[Integer]): Unit = transactional() { _ =>
    userEventDao.recalcEventCount(userEventDao.deleteTopicEvents(msgids.asScala))
  }

  /**
   * Удаление уведомлений, относящихся к удаленным комментариям
   *
   * @param msgids идентификаторы комментариев
   */
  def processCommentsDeleted(msgids: util.List[Integer]): util.List[Integer] = transactional() { _ =>
    val users = userEventDao.deleteCommentEvents(msgids.asScala)
    userEventDao.recalcEventCount(users)
    users.asJava
  }

  def insertCommentWatchNotification(comment: Comment, parentComment: Optional[Comment],
                                     commentId: Int): util.List[Integer] =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      userEventDao.insertCommentWatchNotification(comment, parentComment, commentId).asJava
    }
}