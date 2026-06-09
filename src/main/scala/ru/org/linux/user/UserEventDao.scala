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

package ru.org.linux.user

import org.springframework.stereotype.Repository
import ru.org.linux.comment.Comment
import ru.org.linux.reaction.ReactionDao
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.topic.Topic
import ru.org.linux.user.UserEvent.NoReaction
import ru.org.linux.user.UserEventFilterEnum.DELETED
import ru.org.linux.util.StringUtil
import scalikejdbc.*

import java.sql.Timestamp
import java.util as ju
import scala.jdk.CollectionConverters.*

@Repository
class UserEventDao(springDB: SpringDB):

  /** Добавление уведомления
    *
    * @param eventType
    *   тип уведомления
    * @param userId
    *   идентификационный номер пользователя
    * @param isPrivate
    *   приватное ли уведомление
    * @param topicId
    *   идентификационный номер топика (null если нет)
    * @param commentId
    *   идентификационный номер комментария (null если нет)
    * @param message
    *   дополнительное сообщение уведомления (null если нет)
    */
  def addEvent(
      eventType: String,
      userId: Int,
      isPrivate: Boolean,
      topicId: Option[Int],
      commentId: Option[Int],
      message: Option[String],
      originUser: Option[Int] = None,
      warningId: Option[Int] = None): Unit =
    springDB.run {
      sql"""INSERT INTO user_events (userid, type, private, message_id, comment_id, message, origin_user, warning_id)
          VALUES ($userId, $eventType, $isPrivate, $topicId, $commentId, $message, $originUser, $warningId)"""
        .update
        .apply()
    }

  def insertTopicNotification(topicId: Int, userIds: Iterable[Int]): Unit =
    springDB.run {
      sql"INSERT INTO topic_users_notified (topic, userid) VALUES ({topic}, {userid})"
        .batchByName(userIds.toSeq.map(uid => Seq[(String, Any)]("topic" -> topicId, "userid" -> uid))*)
        .apply()
    }

  def insertReactionNotification(user: User, topic: Topic, comment: Option[Comment]): Unit =
    springDB.run {
      val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)
      sql"""INSERT INTO user_events (userid, type, private, message_id, comment_id, origin_user)
          VALUES ($authorId, 'REACTION', false, ${topic.id}, ${comment.map(_.id)}, ${user.id})
          ON CONFLICT DO NOTHING""".update.apply()
    }

  def deleteUnreadReactionNotification(user: User, topic: Topic, comment: Option[Comment])(using Transaction): Unit =
    val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)

    sql"""DELETE FROM user_events
          WHERE userid=$authorId AND message_id=${topic.id}
          AND comment_id IS NOT DISTINCT FROM ${comment.map(_.id)}
          AND origin_user=${user.id} AND unread AND type='REACTION'""".update.apply()

    recalcEventCount(Seq(authorId))

  def getNotifiedUsers(topicId: Int): Seq[Integer] =
    springDB.run {
      sql"SELECT userid FROM topic_users_notified WHERE topic=$topicId"
        .map(rs => rs.int("userid"))
        .list
        .apply()
        .map(Integer.valueOf)
    }

  /** Сброс уведомлений.
    *
    * @param userId
    *   идентификационный номер пользователь которому сбрасываем
    * @param topId
    *   сбрасываем уведомления с идентификатором не больше этого
    */
  def resetUnreadEvents(userId: Int, topId: Int, eventType: Option[UserEventFilterEnum])(using Transaction): Unit =
    eventType match
      case Some(et) =>
        sql"""UPDATE user_events SET unread=false WHERE userid=$userId AND unread AND id<=$topId
            AND type=${et.getType}::event_type""".update.apply()
      case None =>
        sql"UPDATE user_events SET unread=false WHERE userid=$userId AND unread AND id<=$topId".update.apply()

    recalcEventCount(Seq(userId))

  def resetUnreadEvents(userId: Int, topId: Int, topicId: Int, eventType: UserEventFilterEnum)(using
      Transaction): Unit =
    sql"""UPDATE user_events SET unread=false WHERE userid=$userId AND unread AND id<=$topId
        AND type=${eventType.getType}::event_type AND message_id=$topicId""".update.apply()

    recalcEventCount(Seq(userId))

  def resetUnreadReactionGroup(userId: Int, firstEventId: Int, lastEventId: Int, topicId: Int, commentId: Int)(using
      Transaction): Unit =
    sql"""UPDATE user_events SET unread=false WHERE userid=$userId AND unread AND id BETWEEN $firstEventId AND $lastEventId
        AND type='REACTION' AND message_id=$topicId AND coalesce(comment_id, 0)=$commentId""".update.apply()

    recalcEventCount(Seq(userId))

  /** Сброс уведомлений.
    *
    * @param userId
    *   идентификационный номер пользователь которому сбрасываем
    * @param topId
    *   сбрасываем уведомления с идентификатором не больше этого
    */
  def resetSingle(userId: Int, eventId: Int)(using Transaction): Unit =
    sql"UPDATE user_events SET unread=false WHERE userid=$userId AND unread AND id=$eventId".update.apply()
    recalcEventCount(Seq(userId))

  def recalcEventCount(userids: collection.Seq[Int])(using Transaction): Unit =
    if userids.nonEmpty then
      sql"""UPDATE users SET unread_events =
          (SELECT count(*) FROM user_events WHERE unread AND userid=users.id)
          WHERE users.id IN ($userids)""".update.apply()

  /** Получение списка первых 20 идентификационных номеров пользователей, количество уведомлений которых превышает
    * максимально допустимое значение.
    *
    * @param maxEventsPerUser
    *   максимальное количество уведомлений для одного пользователя
    * @return
    *   список идентификационных номеров пользователей
    */
  def getUserIdListByOldEvents(maxEventsPerUser: Int): ju.List[Integer] =
    springDB.run {
      sql"""SELECT userid FROM user_events GROUP BY userid HAVING count(user_events.id) > $maxEventsPerUser
          ORDER BY count(user_events.id) DESC LIMIT 20"""
        .map(rs => rs.int("userid"))
        .list
        .apply()
        .map(Integer.valueOf)
        .asJava
    }

  /** Очистка старых уведомлений пользователя.
    *
    * @param userId
    *   идентификационный номер пользователя
    * @param maxEventsPerUser
    *   максимальное количество уведомлений для одного пользователя
    */
  def cleanupOldEvents(userId: Int, maxEventsPerUser: Int)(using Transaction): Unit =
    sql"""DELETE FROM user_events WHERE user_events.id IN
        (SELECT id FROM user_events WHERE userid=$userId ORDER BY event_date DESC OFFSET $maxEventsPerUser)"""
      .update
      .apply()

    recalcEventCount(Seq(userId))

  def dropBannedUserEvents()(using Transaction): Int =
    val count =
      sql"""DELETE FROM user_events WHERE event_date < CURRENT_TIMESTAMP - '2 year'::interval
        AND user_events.userid IN
        (SELECT id FROM users WHERE users.blocked AND lastlogin < CURRENT_TIMESTAMP-'2 year'::interval)"""
        .update
        .apply()

    sql"""UPDATE users SET unread_events = (SELECT count(*) FROM user_events WHERE unread AND userid=users.id)
        WHERE unread_events != 0 AND users.blocked AND lastlogin < CURRENT_TIMESTAMP-'2 year'::interval"""
      .update
      .apply()

    count

  def getEvent(id: Int): Option[UserEvent] =
    springDB.run {
      sql"""SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
          comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
          user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
          message_warnings.closed_by is not null as closed_warning, user_events.userid
          FROM user_events
          INNER JOIN topics ON (topics.id = message_id)
          LEFT JOIN comments ON (comments.id=comment_id)
          LEFT JOIN message_warnings ON (message_warnings.id=warning_id)
          WHERE user_events.id = $id""".map(toUserEvent).single.apply()
    }

  /** Получить список уведомлений для пользователя.
    *
    * @param userId
    *   идентификационный номер пользователя
    * @param showPrivate
    *   включать ли приватные
    * @param topics
    *   кол-во уведомлений
    * @param offset
    *   сдвиг относительно начала
    * @param eventFilterType
    *   тип уведомлений
    * @return
    *   список уведомлений
    */
  def getRepliesForUser(
      userId: Int,
      showPrivate: Boolean,
      topics: Int,
      offset: Int,
      eventFilterType: UserEventFilterEnum): Seq[UserEvent] =
    springDB.run {
      if showPrivate then
        val typeFilter =
          if eventFilterType != UserEventFilterEnum.ALL then
            sqls"AND type = ${eventFilterType.getType}::event_type"
          else
            SQLSyntax.empty

        sql"""SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
            comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
            user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
            message_warnings.closed_by is not null as closed_warning, user_events.userid
            FROM user_events
            INNER JOIN topics ON (topics.id = message_id)
            LEFT JOIN comments ON (comments.id=comment_id)
            LEFT JOIN message_warnings ON (message_warnings.id=warning_id)
            WHERE user_events.userid = $userId $typeFilter
            ORDER BY id DESC LIMIT $topics OFFSET $offset""".map(toUserEvent).list.apply()
      else
        sql"""SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
            comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
            user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
            false as closed_warning, user_events.userid
            FROM user_events
            INNER JOIN topics ON (topics.id = message_id)
            LEFT JOIN comments ON (comments.id=comment_id)
            WHERE user_events.userid = $userId AND not private
            ORDER BY id DESC LIMIT $topics OFFSET $offset""".map(toUserEvent).list.apply()
    }

  private def toUserEvent(rs: WrappedResultSet): UserEvent =
    val subj = StringUtil.makeTitle(rs.string("subj"))
    val eventDate = rs.timestamp("event_date")
    val cidOpt = rs.intOpt("cid")
    val cid = cidOpt.getOrElse(0)
    val cAuthor = cidOpt.map(_ => rs.int("cAuthor")).getOrElse(0)
    val groupId = rs.int("groupid")
    val msgid = rs.int("msgid")
    val eventType = UserEventFilterEnum
      .valueOfByType(rs.string("type"))
      .getOrElse(throw new IllegalArgumentException(s"Unknown event type: ${rs.string("type")}"))
    val eventMessage = rs.stringOpt("ev_msg").orNull
    val unread = rs.boolean("unread")
    val originUser = rs.intOpt("origin_user").getOrElse(0)
    val reaction =
      val topicReactions = rs.stringOpt("t_reactions").orNull
      val commentReactions = rs.stringOpt("c_reactions").orNull
      ReactionDao.parse(Option(commentReactions).getOrElse(topicReactions)).reactions.getOrElse(originUser, NoReaction)

    UserEvent(
      cid,
      cAuthor,
      groupId,
      subj,
      msgid,
      eventType,
      eventMessage,
      eventDate,
      unread,
      rs.int("tAuthor"),
      rs.int("id"),
      originUser,
      reaction,
      rs.booleanOpt("closed_warning").getOrElse(false),
      rs.int("userid")
    )

  def deleteTopicEvents(topics: Seq[Int])(using Transaction): collection.Seq[Int] =
    if topics.isEmpty then
      Seq.empty
    else
      val affectedUsers =
        sql"""SELECT DISTINCT userid FROM user_events
          WHERE message_id IN ($topics) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION', 'WARNING')"""
          .map(rs => rs.int("userid"))
          .list
          .apply()

      sql"""DELETE FROM user_events
          WHERE message_id IN ($topics) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION', 'WARNING')"""
        .update
        .apply()

      affectedUsers

  def deleteCommentEvents(comments: Seq[Int])(using Transaction): collection.Seq[Int] =
    if comments.isEmpty then
      Seq.empty
    else
      val affectedUsers =
        sql"""SELECT DISTINCT userid FROM user_events
          WHERE comment_id IN ($comments) AND type IN ('REPLY', 'WATCH', 'REF', 'REACTION', 'WARNING')"""
          .map(rs => rs.int("userid"))
          .list
          .apply()

      sql"""DELETE FROM user_events
          WHERE comment_id IN ($comments) AND type IN ('REPLY', 'WATCH', 'REF', 'REACTION', 'WARNING')""".update.apply()

      affectedUsers

  def insertCommentWatchNotification(comment: Comment, parentComment: Option[Comment], commentId: Int)(using
      Transaction): collection.Seq[Int] =
    val userIds =
      parentComment match
        case Some(parent) =>
          sql"""SELECT memories.userid FROM memories
            WHERE memories.topic = ${comment.topicId}
            AND ${comment.userid} != memories.userid
            AND memories.userid != ${parent.userid}
            AND memories.userid != ${UserConstants.ANONYMOUS_ID}
            AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored IN (select get_branch_authors($commentId)))
            AND watch""".map(rs => rs.int("userid")).list.apply()
        case None =>
          sql"""SELECT memories.userid FROM memories
            WHERE memories.topic = ${comment.topicId}
            AND ${comment.userid} != memories.userid
            AND memories.userid != ${UserConstants.ANONYMOUS_ID}
            AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored=${comment
              .userid})
            AND watch""".map(rs => rs.int("userid")).list.apply()

    if userIds.nonEmpty then
      sql"""INSERT INTO user_events (userid, type, private, message_id, comment_id)
            VALUES ({userid}, {type}, {private}, {message_id}, {comment_id})"""
        .batchByName(
          userIds.map(uid =>
            Seq[(String, Any)](
              "userid" -> uid,
              "type" -> "WATCH",
              "private" -> false,
              "message_id" -> comment.topicId,
              "comment_id" -> commentId))*)
        .apply()

    userIds

  def getEventTypes(userId: Int): Seq[UserEventFilterEnum] =
    springDB.run {
      sql"SELECT distinct(type) FROM user_events WHERE userid=$userId"
        .map(rs => UserEventFilterEnum.valueOfByType(rs.string("type")))
        .list
        .apply()
        .flatten
    }

  def insertTopicMassDeleteNotifications(topicsIds: Seq[Int], reason: String, deletedBy: Int): Unit =
    springDB.run {
      sql"""INSERT INTO user_events (userid, type, private, message_id, message)
          (SELECT topics.userid, ${DELETED
          .getType}::event_type, true, topics.id, $reason FROM topics WHERE topics.id IN ($topicsIds)
            AND topics.userid != $deletedBy AND topics.userid != ${UserConstants.ANONYMOUS_ID})""".update.apply()
    }

  def insertCommentMassDeleteNotifications(commentIds: Seq[Int], reason: String, deletedBy: Int): Unit =
    springDB.run {
      sql"""INSERT INTO user_events (userid, type, private, message_id, comment_id, message)
          (SELECT comments.userid, ${DELETED
          .getType}::event_type, true, comments.topic, comments.id, $reason FROM comments WHERE comments.id IN ($commentIds)
            AND comments.userid != $deletedBy AND comments.userid != ${UserConstants.ANONYMOUS_ID})""".update.apply()
    }
