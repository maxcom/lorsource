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
package ru.org.linux.user

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import ru.org.linux.comment.Comment
import ru.org.linux.spring.dao.DeleteInfoDao.InsertDeleteInfo
import ru.org.linux.topic.Topic
import ru.org.linux.user.UserEventFilterEnum.*
import ru.org.linux.user.UserEventFilterEnum.DELETED

import java.util
import scala.jdk.CollectionConverters.*

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
  def addUserRefEvent(users: collection.Set[User], topicId: Int, commentId: Int): Unit = {
    for (user <- users) {
      userEventDao.addEvent(REFERENCE.getType, user.getId, isPrivate = false, Some(topicId), Some(commentId), None)
    }
  }

  /**
   * Добавление уведомления об упоминании пользователей в топике.
   *
   * @param users   список пользователей. которых надо оповестить
   * @param topicId идентификационный номер топика
   */
  def addUserRefEvent(users: Set[Int], topicId: Int): Unit = transactional() { _ =>
    userEventDao.insertTopicNotification(topicId, users)

    users.foreach { user =>
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
  def addUserTagEvent(userIdList: Seq[Int], topicId: Int): Unit = transactional() { _ =>
    userEventDao.insertTopicNotification(topicId, userIdList)

    userIdList.foreach { userId =>
      userEventDao.addEvent(TAG.getType, userId, isPrivate = false, Some(topicId), None, None)
    }
  }

  def addWarningEvent(author: User, users: collection.Seq[User], topic: Topic, comment: Option[Comment],
                      message: String, warningId: Int): Unit = {
    for (user <- users) {
      userEventDao.addEvent(
        eventType = WARNING.getType,
        userId = user.getId,
        isPrivate = true,
        topicId = Some(topic.id),
        commentId = comment.map(_.id),
        message = Some(message),
        originUser = Some(author.getId),
        warningId = Some(warningId))
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
      logger.info(s"Cleaning up events for userid=$userId")
      userEventDao.cleanupOldEvents(userId, maxEventsPerUser)
    }

    val deleted = userEventDao.dropBannedUserEvents()

    if (deleted != 0) {
      logger.info(s"Deleted $deleted abandoned events")
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
                    eventFilter: UserEventFilterEnum): Seq[UserEvent] = {
    val eventFilterType = if (eventFilter != ALL) {
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
  def processTopicDeleted(msgids: Seq[Int]): Unit = transactional() { _ =>
    userEventDao.recalcEventCount(userEventDao.deleteTopicEvents(msgids))
  }

  /**
   * Удаление уведомлений, относящихся к удаленным комментариям
   *
   * @param msgids идентификаторы комментариев
   */
  def processCommentsDeleted(msgids: Seq[Int]): collection.Seq[Int] = transactional() { _ =>
    val users = userEventDao.deleteCommentEvents(msgids)

    userEventDao.recalcEventCount(users)

    users
  }

  def insertCommentWatchNotification(comment: Comment, parentComment: Option[Comment],
                                     commentId: Int): collection.Seq[Int] =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      userEventDao.insertCommentWatchNotification(comment, parentComment, commentId)
    }

  def getEventTypes(user: User): Seq[UserEventFilterEnum] = {
    val unsorted = userEventDao.getEventTypes(user.getId).toSet

    if (unsorted.sizeIs > 1) {
      UserEventFilterEnum.values.view.filter(v => v == UserEventFilterEnum.ALL || unsorted(v)).toSeq
    } else {
      Seq.empty
    }
  }

  def insertTopicDeleteNotification(topic: Topic, info: InsertDeleteInfo): Unit = {
    assert(topic.id == info.msgid())

    if (info.deleteUser().getId != topic.authorUserId && topic.authorUserId != User.ANONYMOUS_ID) {
      userEventDao.addEvent(
        eventType = DELETED.getType,
        userId = topic.authorUserId,
        isPrivate = true,
        topicId = Some(topic.id),
        commentId = None,
        message = Some(info.reason()))
    }
  }

  def insertCommentDeleteNotification(comment: Comment, info: InsertDeleteInfo): Unit = {
    assert(comment.id == info.msgid())

    if (info.deleteUser().getId != comment.userid && comment.userid != User.ANONYMOUS_ID) {
      userEventDao.addEvent(
        eventType = DELETED.getType,
        userId = comment.userid,
        isPrivate = true,
        topicId = Some(comment.topicId),
        commentId = Some(comment.id),
        message = Some(info.reason()))
    }
  }

  def insertTopicMassDeleteNotifications(topicsIds: Seq[Int], reason: String, deletedBy: User): Unit = {
    if (topicsIds.nonEmpty) {
      userEventDao.insertTopicMassDeleteNotifications(topicsIds, reason, deletedBy.getId)
    }
  }

  def insertCommentMassDeleteNotifications(commentIds: Seq[Int], reason: String, deletedBy: User): Unit = {
    if (commentIds.nonEmpty) {
      userEventDao.insertCommentMassDeleteNotifications(commentIds, reason, deletedBy.getId)
    }
  }
}