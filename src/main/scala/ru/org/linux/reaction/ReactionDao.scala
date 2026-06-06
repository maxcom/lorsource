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
package ru.org.linux.reaction

import com.typesafe.scalalogging.StrictLogging
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.springframework.stereotype.Repository
import ru.org.linux.comment.Comment
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import scalikejdbc.*

import java.sql.Timestamp
import scala.beans.BeanProperty

case class Reactions(reactions: Map[Int, String])

object Reactions:
  val empty: Reactions = Reactions(Map.empty)

case class ReactionsLogItem(
    originUserId: Int,
    topicId: Int,
    commentId: Option[Int],
    @BeanProperty
    setDate: Timestamp,
    @BeanProperty
    reaction: String)

case class ReactionsView(item: ReactionsLogItem, title: String, targetUserId: Int, sectionId: Int, groupUrlName: String)

object ReactionDao extends StrictLogging:
  def parse(json: String): Reactions =
    val parsed: Either[Error, Map[Int, String]] = decode[Map[Int, String]](json)

    Reactions(
      parsed
        .toTry
        .recover { ex =>
          logger.warn("Can't parse reactions", ex)

          Map.empty[Int, String]
        }
        .get)

@Repository
class ReactionDao(springDB: SpringDB):

  def setCommentReaction(comment: Comment, user: User, reaction: String, set: Boolean): Int =
    springDB.run:
      if set then
        val add = Map(user.id -> reaction).asJson.noSpaces
        sql"UPDATE comments SET reactions = reactions || ${add}::jsonb WHERE id=${comment.id}".update.apply()
        sql"""INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction) VALUES(${user.id}, ${comment
            .topicId}, ${comment.id}, $reaction)
              ON CONFLICT (origin_user, topic_id, comment_id)
              DO UPDATE SET set_date=CURRENT_TIMESTAMP, reaction = EXCLUDED.reaction""".update.apply()
      else
        sql"UPDATE comments SET reactions = reactions - ${user.id.toString} WHERE id=${comment.id}".update.apply()
        sql"DELETE FROM reactions_log WHERE origin_user=${user.id} AND topic_id=${comment
            .topicId} AND comment_id=${comment.id}".update.apply()
      end if

      val r =
        sql"SELECT reactions FROM comments WHERE id=${comment.id}".map(rs => rs.string("reactions")).single.apply().get
      ReactionDao.parse(r).reactions.values.count(_ == reaction)

  def setTopicReaction(topic: Topic, user: User, reaction: String, set: Boolean): Int =
    springDB.run:
      if set then
        val add = Map(user.id -> reaction).asJson.noSpaces
        sql"UPDATE topics SET reactions = reactions || ${add}::jsonb WHERE id=${topic.id}".update.apply()
        sql"""INSERT INTO reactions_log (origin_user, topic_id, reaction) VALUES(${user.id}, ${topic.id}, $reaction)
              ON CONFLICT (origin_user, topic_id, comment_id)
              DO UPDATE SET set_date=CURRENT_TIMESTAMP, reaction = EXCLUDED.reaction""".update.apply()
      else
        sql"UPDATE topics SET reactions = reactions - ${user.id.toString} WHERE id=${topic.id}".update.apply()
        sql"DELETE FROM reactions_log WHERE origin_user=${user.id} AND topic_id=${topic.id} AND comment_id IS NULL"
          .update
          .apply()
      end if

      val r =
        sql"SELECT reactions FROM topics WHERE id=${topic.id}".map(rs => rs.string("reactions")).single.apply().get
      ReactionDao.parse(r).reactions.values.count(_ == reaction)

  def recentReactionCount(origin: User): Int =
    springDB.run:
      sql"SELECT count(*) FROM reactions_log WHERE origin_user=${origin
          .id} AND set_date > CURRENT_TIMESTAMP - '10 minutes'::interval"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def getLogByTopic(topic: Topic): Seq[ReactionsLogItem] =
    springDB.run:
      sql"SELECT origin_user, set_date, reaction FROM reactions_log WHERE topic_id=${topic.id} AND comment_id IS NULL"
        .map(rs =>
          ReactionsLogItem(
            originUserId = rs.int("origin_user"),
            topicId = topic.id,
            commentId = None,
            setDate = rs.timestamp("set_date"),
            reaction = rs.string("reaction")))
        .list
        .apply()

  def getLogByComment(comment: Comment): Seq[ReactionsLogItem] =
    springDB.run:
      sql"SELECT origin_user, set_date, reaction FROM reactions_log WHERE topic_id=${comment
          .topicId} AND comment_id=${comment.id}"
        .map(rs =>
          ReactionsLogItem(
            originUserId = rs.int("origin_user"),
            topicId = comment.topicId,
            commentId = Some(comment.id),
            setDate = rs.timestamp("set_date"),
            reaction = rs.string("reaction")
          ))
        .list
        .apply()

  def getReactionsView(
      originUser: User,
      offset: Int,
      size: Int,
      isReactionsOn: Boolean,
      includeDeleted: Boolean): Seq[ReactionsView] =
    springDB.run:
      val toView =
        (rs: WrappedResultSet) =>
          ReactionsView(
            item = ReactionsLogItem(
              originUserId = originUser.id,
              topicId = rs.int("topic_id"),
              commentId = rs.intOpt("comment_id"),
              setDate = rs.timestamp("set_date"),
              reaction = rs.string("reaction")
            ),
            title = rs.string("title"),
            targetUserId = rs.int("target_user"),
            sectionId = rs.int("section"),
            groupUrlName = rs.string("urlname")
          )

      if isReactionsOn then
        val notDeletedTopic =
          if includeDeleted then
            SQLSyntax.empty
          else
            sqls"AND NOT t.deleted"
        val notDeletedComment =
          if includeDeleted then
            SQLSyntax.empty
          else
            sqls"AND NOT c.deleted"

        sql"""SELECT r.topic_id, r.comment_id, r.set_date, r.reaction, r.origin_user as "target_user", g."section", g.urlname, t.title
              FROM reactions_log r
              JOIN topics t ON r.topic_id = t.id $notDeletedTopic
              JOIN groups g ON t.groupid = g.id
              WHERE r.comment_id IS NULL AND t.userid=${originUser.id}
              UNION ALL
              SELECT r.topic_id, r.comment_id, r.set_date, r.reaction, r.origin_user, g."section", g.urlname, t.title
              FROM reactions_log r
              JOIN topics t ON r.topic_id = t.id $notDeletedTopic
              JOIN comments c ON c.id = r.comment_id
              JOIN groups g ON t.groupid = g.id
              WHERE c.userid=${originUser.id} $notDeletedComment
              ORDER BY set_date DESC OFFSET $offset LIMIT $size""".map(toView).list.apply()
      else
        val notDeleted =
          if includeDeleted then
            SQLSyntax.empty
          else
            sqls"AND NOT topics.deleted AND comments.deleted IS NOT TRUE"

        sql"""SELECT topic_id, comment_id, set_date, reaction, topics.title,
              COALESCE(comments.userid, topics.userid) target_user, groups.section, groups.urlname
              FROM reactions_log JOIN topics ON topic_id = topics.id
              JOIN groups ON topics.groupid = groups.id
              LEFT JOIN comments ON comment_id = comments.id
              WHERE origin_user=${originUser.id} $notDeleted
              ORDER BY set_date DESC OFFSET $offset LIMIT $size""".map(toView).list.apply()
