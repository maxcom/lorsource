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

import com.google.common.collect.ImmutableMap
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import ru.org.linux.comment.Comment
import ru.org.linux.reaction.ReactionDao
import ru.org.linux.topic.Topic
import ru.org.linux.user.UserEvent.NoReaction
import ru.org.linux.user.UserEventDao.QueryById
import ru.org.linux.user.UserEventFilterEnum.DELETED
import ru.org.linux.util.StringUtil

import java.sql.ResultSet
import java.util
import javax.sql.DataSource
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Repository
object UserEventDao {
  private val QueryById =
    """
      |SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
      |  comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
      |  user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
      |  message_warnings.closed_by is not null as closed_warning, user_events.userid
      |FROM user_events
      |  INNER JOIN topics ON (topics.id = message_id)
      |  LEFT JOIN comments ON (comments.id=comment_id)
      |  LEFT JOIN message_warnings ON (message_warnings.id=warning_id)
      |WHERE user_events.id = ?
      |""".stripMargin

  private val QueryAll =
    """
      |SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
      |  comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
      |  user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
      |  message_warnings.closed_by is not null as closed_warning, user_events.userid
      |FROM user_events
      |  INNER JOIN topics ON (topics.id = message_id)
      |  LEFT JOIN comments ON (comments.id=comment_id)
      |  LEFT JOIN message_warnings ON (message_warnings.id=warning_id)
      |WHERE user_events.userid = ? %s
      |ORDER BY id DESC LIMIT ? OFFSET ?
      |""".stripMargin

  private val QueryWithoutPrivate =
    """
      |SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
      |  comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
      |  user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions,
      |  false as closed_warning, user_events.userid
      |FROM user_events
      |  INNER JOIN topics ON (topics.id = message_id)
      |  LEFT JOIN comments ON (comments.id=comment_id)
      |WHERE user_events.userid = ? AND not private
      |ORDER BY id DESC LIMIT ? OFFSET ?
      |""".stripMargin
}

@Repository
class UserEventDao(ds: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val insert = {
    val insert = new SimpleJdbcInsert(ds)
    insert.setTableName("user_events")
    insert.usingColumns("userid", "type", "private", "message_id", "comment_id", "message", "origin_user", "warning_id")
  }

  private val insertTopicUsersNotified = {
    val insert = new SimpleJdbcInsert(ds)
    insert.setTableName("topic_users_notified")
    insert.usingColumns("topic", "userid")
  }

  private val jdbcTemplate = new JdbcTemplate(ds)
  private val namedJdbcTemplate = new NamedParameterJdbcTemplate(ds)

  /**
   * Добавление уведомления
   *
   * @param eventType тип уведомления
   * @param userId    идентификационный номер пользователя
   * @param isPrivate приватное ли уведомление
   * @param topicId   идентификационный номер топика (null если нет)
   * @param commentId идентификационный номер комментария (null если нет)
   * @param message     дополнительное сообщение уведомления (null если нет)
   */
  def addEvent(eventType: String, userId: Int, isPrivate: Boolean, topicId: Option[Int],
               commentId: Option[Int], message: Option[String], originUser: Option[Int] = None,
               warningId: Option[Int] = None): Unit = {
    val params = mutable.Map("userid" -> userId, "type" -> eventType, "private" -> isPrivate)

    topicId.foreach(v => params.put("message_id", v))
    commentId.foreach(v => params.put("comment_id", v))
    message.foreach(v => params.put("message", v))
    originUser.foreach(v => params.put("origin_user", v))
    warningId.foreach(v => params.put("warning_id", v))

    insert.execute(params.asJava)
  }

  def insertTopicNotification(topicId: Int, userIds: Iterable[Int]): Unit = {
    val batch = userIds.view.map(userId => Map("topic" -> topicId, "userid" -> userId).asJava).toSeq

    insertTopicUsersNotified.executeBatch(batch*)
  }

  def insertReactionNotification(user: User, topic: Topic, comment: Option[Comment]): Unit = {
    val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)

    jdbcTemplate.update("INSERT INTO " +
        "user_events (userid, type, private, message_id, comment_id, origin_user)" +
        "VALUES (?, 'REACTION', false, ?, ?, ?) ON CONFLICT DO NOTHING",
      authorId, topic.id, comment.map(c => Integer.valueOf(c.id)).orNull, user.getId)
  }

  def deleteUnreadReactionNotification(user: User, topic: Topic, comment: Option[Comment]): Unit = {
    val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)

    jdbcTemplate.update(
        "DELETE FROM user_events " +
          "WHERE userid=? AND message_id=? AND comment_id IS NOT DISTINCT FROM ? " +
          "AND origin_user=? AND unread AND type='REACTION'",
      authorId, topic.id, comment.map(c => Integer.valueOf(c.id)).orNull, user.getId)

    recalcEventCount(Seq(authorId))
  }

  def getNotifiedUsers(topicId: Int): Seq[Integer] =
    jdbcTemplate.queryForSeq[Integer]("SELECT userid FROM topic_users_notified WHERE topic=?", topicId)

  /**
   * Сброс уведомлений.
   *
   * @param userId идентификационный номер пользователь которому сбрасываем
   * @param topId  сбрасываем уведомления с идентификатором не больше этого
   */
  def resetUnreadEvents(userId: Int, topId: Int, eventType: Option[UserEventFilterEnum]): Unit = transactional() { _ =>
    eventType match {
      case Some(eventType) =>
        jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id<=? " +
          "AND type = ?::event_type",
          userId, topId, eventType.getType)
      case None =>
        jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id<=?", userId, topId)
    }

    recalcEventCount(Seq(userId))
  }

  def resetUnreadEvents(userId: Int, topId: Int, topicId: Int, eventType: UserEventFilterEnum): Unit = transactional() { _ =>
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id<=? " +
      "AND type = ?::event_type AND message_id = ?",
      userId, topId, eventType.getType, topicId)

    recalcEventCount(Seq(userId))
  }

  /**
   * Сброс уведомлений.
   *
   * @param userId идентификационный номер пользователь которому сбрасываем
   * @param topId  сбрасываем уведомления с идентификатором не больше этого
   */
  def resetSingle(userId: Int, eventId: Int): Unit = transactional() { _ =>
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id=?", userId, eventId)
    recalcEventCount(Seq(userId))
  }

  def recalcEventCount(userids: collection.Seq[Int]): Unit = {
    if (userids.nonEmpty) {
      namedJdbcTemplate.update("UPDATE users SET unread_events = " +
        "(SELECT count(*) FROM user_events WHERE unread AND userid=users.id) WHERE users.id IN (:list)",
        ImmutableMap.of("list", userids.map(Integer.valueOf).asJavaCollection))
    }
  }

  /**
   * Получение списка первых 20 идентификационных номеров пользователей,
   * количество уведомлений которых превышает максимально допустимое значение.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   * @return список идентификационных номеров пользователей
   */
  def getUserIdListByOldEvents(maxEventsPerUser: Int): util.List[Integer] =
    jdbcTemplate.queryForSeq[Integer](
      "select userid from user_events group by userid having count(user_events.id) > ? " +
        "order by count(user_events.id) DESC limit 20", maxEventsPerUser).asJava

  /**
   * Очистка старых уведомлений пользователя.
   *
   * @param userId           идентификационный номер пользователя
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   */
  def cleanupOldEvents(userId: Int, maxEventsPerUser: Int): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "DELETE FROM user_events WHERE user_events.id IN " +
        "(SELECT id FROM user_events WHERE userid=? ORDER BY event_date DESC OFFSET ?)",
      userId, maxEventsPerUser)

    recalcEventCount(Seq(userId))
  }

  def dropBannedUserEvents(): Int = transactional() { _ =>
    val count = jdbcTemplate.update(
      "DELETE FROM user_events WHERE event_date < CURRENT_TIMESTAMP - '2 year'::interval AND user_events.userid IN " +
        "(SELECT id FROM users WHERE users.blocked and lastlogin < CURRENT_TIMESTAMP-'2 year'::interval)")

    jdbcTemplate.update(
      "UPDATE users SET unread_events = (SELECT count(*) FROM user_events WHERE unread AND userid=users.id) " +
        "WHERE unread_events != 0 AND users.blocked and lastlogin < CURRENT_TIMESTAMP-'2 year'::interval")

    count
  }

  def getEvent(id: Int): UserEvent = {
    jdbcTemplate.queryForObjectAndMap(QueryById, id) { (resultSet, _) =>
      toUserEvent(resultSet)
    }.get
  }

  /**
   * Получить список уведомлений для пользователя.
   *
   * @param userId          идентификационный номер пользователя
   * @param showPrivate     включать ли приватные
   * @param topics          кол-во уведомлений
   * @param offset          сдвиг относительно начала
   * @param eventFilterType тип уведомлений
   * @return список уведомлений
   */
  def getRepliesForUser(userId: Int, showPrivate: Boolean, topics: Int, offset: Int,
                        eventFilterType: Option[String]): Seq[UserEvent] = {
    val queryString = if (showPrivate) {
      val queryPart = if (eventFilterType.isDefined) {
        s" AND type = '${eventFilterType.get}' "
      } else {
        ""
      }

      String.format(UserEventDao.QueryAll, queryPart)
    } else {
      UserEventDao.QueryWithoutPrivate
    }

    jdbcTemplate.queryAndMap(queryString, userId, topics, offset) { (resultSet, _) =>
      toUserEvent(resultSet)
    }
  }

  private def toUserEvent(resultSet: ResultSet): UserEvent = {
    val subj = StringUtil.makeTitle(resultSet.getString("subj"))
    val eventDate = resultSet.getTimestamp("event_date")
    val cid = resultSet.getInt("cid")

    val cAuthor = if (!resultSet.wasNull) {
      resultSet.getInt("cAuthor")
    } else {
      0
    }

    val groupId = resultSet.getInt("groupid")
    val msgid = resultSet.getInt("msgid")
    val `type` = UserEventFilterEnum.valueOfByType(resultSet.getString("type"))
    val eventMessage = resultSet.getString("ev_msg")
    val unread = resultSet.getBoolean("unread")

    val originUser = resultSet.getInt("origin_user")

    val reaction = {
      val topicReactions = resultSet.getString("t_reactions")
      val commentReactions = resultSet.getString("c_reactions")

      ReactionDao.parse(Option(commentReactions).getOrElse(topicReactions)).reactions.getOrElse(originUser, NoReaction)
    }

    UserEvent(cid, cAuthor, groupId, subj, msgid, `type`, eventMessage, eventDate, unread,
      resultSet.getInt("tAuthor"), resultSet.getInt("id"),
      originUser, reaction, resultSet.getBoolean("closed_warning"), resultSet.getInt("userid"))
  }

  def deleteTopicEvents(topics: Seq[Int]): collection.Seq[Int] = {
    transactional() { _ =>
      if (topics.isEmpty) {
        Seq.empty
      } else {
        val affectedUsers = namedJdbcTemplate.queryForList(
          "SELECT DISTINCT (userid) FROM user_events WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION', 'WARNING')",
          ImmutableMap.of("list", topics.asJava), classOf[Integer])

        namedJdbcTemplate.update(
          "DELETE FROM user_events WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION', 'WARNING')",
          ImmutableMap.of("list", topics.asJava))

        affectedUsers.asScala.map(i => i)
      }
    }
  }

  def deleteCommentEvents(comments: Seq[Int]): collection.Seq[Int] = {
    transactional() { _ =>
      if (comments.isEmpty) {
        Seq.empty
      } else {
        val affectedUsers = namedJdbcTemplate.queryForList(
          "SELECT DISTINCT (userid) FROM user_events WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF', 'REACTION', 'WARNING')",
          Map("list" -> comments.map(Integer.valueOf).asJava).asJava, classOf[Integer])

        namedJdbcTemplate.update("DELETE FROM user_events WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF', 'REACTION', 'WARNING')",
          Map("list" -> comments.asJava).asJava)

        affectedUsers.asScala.map(i => i)
      }
    }
  }

  def insertCommentWatchNotification(comment: Comment, parentComment: Option[Comment], commentId: Int): collection.Seq[Int] =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      val params = new util.HashMap[String, Integer]
      params.put("topic", comment.topicId)
      params.put("id", commentId)
      params.put("userid", comment.userid)

      val userIds = (if (parentComment.isDefined) {
        params.put("parent_author", parentComment.get.userid)
        namedJdbcTemplate.queryForList("SELECT memories.userid FROM memories WHERE memories.topic = :topic AND :userid != memories.userid AND memories.userid != :parent_author " + "AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored IN (select get_branch_authors(:id))) AND watch", params, classOf[Integer])
      } else {
        namedJdbcTemplate.queryForList("SELECT memories.userid FROM memories WHERE memories.topic = :topic AND :userid != memories.userid AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored=:userid) AND watch", params, classOf[Integer])
      }).asScala

      if (userIds.nonEmpty) {
        val batch = userIds.view.map { userId =>
          Map(
            "userid" -> userId,
            "type" -> "WATCH",
            "private" -> false,
            "message_id" -> comment.topicId,
            "comment_id" -> commentId).asJava
        }.toSeq

        insert.executeBatch(batch*)
      }

      userIds.map(i => i)
    }

  def getEventTypes(userId: Int): Seq[UserEventFilterEnum] = {
    jdbcTemplate.queryAndMap("select distinct(type) from user_events where userid=?", userId) { (rs, _) =>
      UserEventFilterEnum.valueOfByType(rs.getString("type"))
    }
  }

  def insertTopicMassDeleteNotifications(topicsIds: Seq[Int], reason: String, deletedBy: Int): Unit = {
    namedJdbcTemplate.update(s"""
        insert into user_events (userid, type, private, message_id, message)
          (select topics.userid, '${DELETED.getType}', true, topics.id, :message from topics where topics.id in (:topics)
            and topics.userid != :deletedBy and topics.userid != ${User.ANONYMOUS_ID})
      """, Map("message" -> reason, "topics" -> topicsIds.map(Integer.valueOf).asJava, "deletedBy" -> deletedBy).asJava)
  }

  def insertCommentMassDeleteNotifications(commentIds: Seq[Int], reason: String, deletedBy: Int): Unit = {
    namedJdbcTemplate.update(s"""
        insert into user_events (userid, type, private, message_id, comment_id, message)
          (select comments.userid, '${DELETED.getType}', true, comments.topic, comments.id,
            :message from comments where comments.id in (:comments)
            and comments.userid != :deletedBy and comments.userid != ${User.ANONYMOUS_ID})
      """, Map("message" -> reason, "comments" -> commentIds.map(Integer.valueOf).asJava, "deletedBy" -> deletedBy).asJava)
  }
}