/*
 * Copyright 1998-2023 Linux.org.ru
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
import ru.org.linux.util.StringUtil

import java.util
import java.util.Optional
import javax.sql.DataSource
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Repository
object UserEventDao {
  private val QueryAll =
    """
      |SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
      |  comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
      |  user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions
      |FROM user_events
      |  INNER JOIN topics ON (topics.id = message_id)
      |  LEFT JOIN comments ON (comments.id=comment_id)
      |WHERE user_events.userid = ? %s
      |ORDER BY id DESC LIMIT ? OFFSET ?
      |""".stripMargin

  private val QueryWithoutPrivate =
    """
      |SELECT user_events.id, event_date, topics.title as subj, topics.id as msgid, comments.id AS cid,
      |  comments.userid AS cAuthor, topics.userid AS tAuthor, unread, groupid, comments.deleted, type,
      |  user_events.message as ev_msg, origin_user, comments.reactions as c_reactions, topics.reactions as t_reactions
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
    insert.usingColumns("userid", "type", "private", "message_id", "comment_id", "message")
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
   * @param message   дополнительное сообщение уведомления (null если нет)
   */
  def addEvent(eventType: String, userId: Int, isPrivate: Boolean, topicId: Option[Int],
               commentId: Option[Int], message: Option[String]): Unit = {
    val params = mutable.Map("userid" -> userId, "type" -> eventType, "private" -> isPrivate)

    topicId.foreach(v => params.put("message_id", v))
    commentId.foreach(v => params.put("comment_id", v))
    message.foreach(v => params.put("message", v))

    insert.execute(params.asJava)
  }

  def insertTopicNotification(topicId: Int, userIds: Iterable[Integer]): Unit = {
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
  def resetUnreadReplies(userId: Int, topId: Int): Unit = transactional() { _ =>
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id<=?", userId, topId)
    recalcEventCount(Seq(userId))
  }

  def recalcEventCount(userids: collection.Seq[Integer]): Unit = {
    if (userids.nonEmpty) {
      namedJdbcTemplate.update("UPDATE users SET unread_events = " +
        "(SELECT count(*) FROM user_events WHERE unread AND userid=users.id) WHERE users.id IN (:list)",
        ImmutableMap.of("list", userids.asJavaCollection))
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
        originUser, reaction)
    }
  }

  def deleteTopicEvents(topics: collection.Seq[Integer]): collection.Seq[Integer] = {
    transactional() { _ =>
      if (topics.isEmpty) {
        Seq.empty
      } else {
        val affectedUsers = namedJdbcTemplate.queryForList(
          "SELECT DISTINCT (userid) FROM user_events WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION')",
          ImmutableMap.of("list", topics.asJava), classOf[Integer])

        namedJdbcTemplate.update(
          "DELETE FROM user_events WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH', 'REACTION')",
          ImmutableMap.of("list", topics.asJava))

        affectedUsers.asScala
      }
    }
  }

  def deleteCommentEvents(comments: collection.Seq[Integer]): collection.Seq[Integer] = {
    transactional() { _ =>
      if (comments.isEmpty) {
        Seq.empty
      } else {
        val affectedUsers = namedJdbcTemplate.queryForList(
          "SELECT DISTINCT (userid) FROM user_events WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF', 'REACTION')",
          ImmutableMap.of("list", comments.asJava), classOf[Integer])

        namedJdbcTemplate.update("DELETE FROM user_events WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF', 'REACTION')",
          ImmutableMap.of("list", comments.asJava))

        affectedUsers.asScala
      }
    }
  }

  def insertCommentWatchNotification(comment: Comment, parentComment: Optional[Comment], commentId: Int): collection.Seq[Integer] = {
    transactional(propagation = Propagation.MANDATORY) { _ =>
      val params = new util.HashMap[String, Integer]
      params.put("topic", comment.topicId)
      params.put("id", commentId)
      params.put("userid", comment.userid)

      val userIds = (if (parentComment.isPresent) {
        params.put("parent_author", parentComment.get.userid)
        namedJdbcTemplate.queryForList("SELECT memories.userid " + "FROM memories WHERE memories.topic = :topic AND :userid != memories.userid " + "AND memories.userid != :parent_author " + "AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored IN (select get_branch_authors(:id))) AND watch", params, classOf[Integer])
      } else {
        namedJdbcTemplate.queryForList("SELECT memories.userid " + "FROM memories WHERE memories.topic = :topic AND :userid != memories.userid " + "AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored=:userid) AND watch", params, classOf[Integer])
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

      userIds
    }
  }
}