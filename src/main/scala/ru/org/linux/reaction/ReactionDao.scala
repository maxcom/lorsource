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
package ru.org.linux.reaction

import com.typesafe.scalalogging.StrictLogging
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import ru.org.linux.comment.Comment
import ru.org.linux.topic.Topic
import ru.org.linux.user.User

import java.sql.Timestamp
import javax.sql.DataSource
import scala.beans.BeanProperty

// reaction -> Seq[UserId]
case class Reactions(reactions: Map[Int, String])

object Reactions {
  val empty: Reactions = Reactions(Map.empty)
}

case class ReactionsLogItem(originUserId: Int, topicId: Int, commentId: Option[Int], @BeanProperty setDate: Timestamp,
                            @BeanProperty reaction: String)

case class ReactionsView(item: ReactionsLogItem, title: String, targetUserId: Int, sectionId: Int, groupUrlName: String)

object ReactionDao extends StrictLogging {
  def parse(json: String): Reactions = {
    val parsed: Either[Error, Map[Int, String]] = decode[Map[Int, String]](json)

    Reactions(parsed.toTry.recover { ex =>
      logger.warn("Can't parse reactions", ex)

      Map.empty[Int, String]
    }.get)
  }
}

@Repository
class ReactionDao(ds: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def setCommentReaction(comment: Comment, user: User, reaction: String, set: Boolean): Int =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      if (set) {
        val add = Map(user.getId -> reaction).asJson.noSpaces

        jdbcTemplate.update("UPDATE comments SET reactions = reactions || ? WHERE id=?", add, comment.id)
        jdbcTemplate.update("INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction) VALUES(?, ?, ?, ?) " +
          "ON CONFLICT (origin_user, topic_id, comment_id) " +
          "DO UPDATE SET set_date=CURRENT_TIMESTAMP, reaction = EXCLUDED.reaction",
          user.getId, comment.topicId, comment.id, reaction)
      } else {
        jdbcTemplate.update("UPDATE comments SET reactions = reactions - ? WHERE id=?", user.getId.toString, comment.id)
        jdbcTemplate.update("DELETE FROM reactions_log WHERE origin_user=? AND topic_id=? AND comment_id=?",
          user.getId, comment.topicId, comment.id)
      }

      val r = jdbcTemplate.queryForObject[String]("SELECT reactions FROM comments WHERE id=?", comment.id)

      ReactionDao.parse(r.get).reactions.values.count(_ == reaction)
    }

  def setTopicReaction(topic: Topic, user: User, reaction: String, set: Boolean): Int =
    transactional(propagation = Propagation.MANDATORY) { _ =>
      if (set) {
        val add = Map(user.getId -> reaction).asJson.noSpaces

        jdbcTemplate.update("UPDATE topics SET reactions = reactions || ? WHERE id=?", add, topic.id)
        jdbcTemplate.update("INSERT INTO reactions_log (origin_user, topic_id, reaction) VALUES(?, ?, ?) " +
          "ON CONFLICT (origin_user, topic_id, comment_id) " +
          "DO UPDATE SET set_date=CURRENT_TIMESTAMP, reaction = EXCLUDED.reaction", user.getId, topic.id, reaction)
      } else {
        jdbcTemplate.update("UPDATE topics SET reactions = reactions - ? WHERE id=?", user.getId.toString, topic.id)
        jdbcTemplate.update("DELETE FROM reactions_log WHERE origin_user=? AND topic_id=? AND comment_id IS NULL",
          user.getId, topic.id)
      }

      val r = jdbcTemplate.queryForObject[String]("SELECT reactions FROM topics WHERE id=?", topic.id)

      ReactionDao.parse(r.get).reactions.values.count(_ == reaction)
    }

  def recentReactionCount(origin: User): Int =
    jdbcTemplate.queryForObject[Int](
      "SELECT count(*) FROM reactions_log " +
        "WHERE origin_user=? AND set_date > CURRENT_TIMESTAMP - '10 minutes'::interval", origin.getId).getOrElse(0)

  def getLogByTopic(topic: Topic): Seq[ReactionsLogItem] =
    jdbcTemplate.queryAndMap[ReactionsLogItem](
      "SELECT origin_user, set_date, reaction " +
        "FROM reactions_log WHERE topic_id = ? AND comment_id IS NULL", topic.id) { case (rs, _) =>
          ReactionsLogItem(
            originUserId = rs.getInt("origin_user"),
            topicId = topic.id,
            commentId = None,
            setDate = rs.getTimestamp("set_date"),
            reaction = rs.getString("reaction"))
    }

  def getLogByComment(comment: Comment): Seq[ReactionsLogItem] =
    jdbcTemplate.queryAndMap[ReactionsLogItem](
      "SELECT origin_user, set_date, reaction " +
        "FROM reactions_log WHERE topic_id = ? AND comment_id = ?", comment.topicId, comment.id) { case (rs, _) =>
      ReactionsLogItem(
        originUserId = rs.getInt("origin_user"),
        topicId = comment.topicId,
        commentId = Some(comment.id),
        setDate = rs.getTimestamp("set_date"),
        reaction = rs.getString("reaction"))
    }

  def getReactionsView(originUser: User, offset: Int, size: Int,isReactionsOn: Boolean,includeDeleted: Boolean): Seq[ReactionsView] =
    jdbcTemplate.queryAndMap(
      if (isReactionsOn)
         "WITH constants (selectedId) as ( values (?) ) " +
           " select r.topic_id,r.comment_id,r.set_date, r.reaction,r.origin_user as \"target_user\", g.\"section\", g.urlname, t.title " +
           " from reactions_log r " +
           " join topics t ON r.topic_id = t.id  " +
           (if (!includeDeleted) { " AND NOT t.deleted" } else "" ) +
           " join groups g ON t.groupid = g.id " +
           " WHERE r.comment_id is null and t.userid=(select selectedId from constants) " +
           " UNION ALL " +
           " select r.topic_id,r.comment_id,r.set_date, r.reaction, r.origin_user, g.\"section\", g.urlname, t.title " +
           " from reactions_log r " +
           " join topics t ON r.topic_id = t.id " +
           (if (!includeDeleted) { " AND NOT t.deleted" } else "") +
           " JOIN comments c ON c.id = r.comment_id " +
           " join groups g ON t.groupid = g.id " +
           " WHERE c.userid=(select selectedId from constants) " +
           (if (!includeDeleted) { " AND NOT c.deleted" } else "") +
           " order by set_date desc OFFSET ? LIMIT ?"
      else
      "SELECT topic_id, comment_id, set_date, reaction, topics.title, " +
        "COALESCE(comments.userid, topics.userid) target_user, groups.section, groups.urlname " +
        "FROM reactions_log JOIN topics ON topic_id = topics.id " +
          "JOIN groups ON topics.groupid = groups.id " +
          "LEFT JOIN comments ON comment_id = comments.id " +
        "WHERE origin_user=?  " +
        (if (!includeDeleted) {  " AND NOT topics.deleted AND comments.deleted IS NOT TRUE "  } else "") +
        " ORDER BY set_date DESC OFFSET ? LIMIT ?",
      originUser.getId, offset, size)  { case (rs, _) =>
      ReactionsView(
        item = ReactionsLogItem(
          originUserId = originUser.getId,
          topicId = rs.getInt("topic_id"),
          commentId = Option(rs.getInt("comment_id")).filter(_ != 0),
          setDate = rs.getTimestamp("set_date"),
          reaction = rs.getString("reaction")),
        title = rs.getString("title"), // escaped in database
        targetUserId = rs.getInt("target_user"),
        sectionId = rs.getInt("section"),
        groupUrlName = rs.getString("urlname"))
    }
}