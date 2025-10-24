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

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class WarningDao(ds: DataSource) {
  private val insert =
    new SimpleJdbcInsert(ds).usingColumns("topic", "comment", "author", "message", "warning_type")
      .withTableName("message_warnings")
      .usingGeneratedKeyColumns("id")

  private val namedJdbcTemplate = new NamedParameterJdbcTemplate(ds)

  def postWarning(topicId: Int, commentId: Option[Int], authorId: Int, message: String, warningType: WarningType): Int = {
    val args = Map("topic" -> topicId, "author" -> authorId, "message" -> message, "warning_type" -> warningType.id) ++
      commentId.map("comment" -> _)

    insert.executeAndReturnKey(args.asJava).intValue()
  }

  private val mapper: RowMapper[Warning] = { (rs, _) =>
    Warning(
      id = rs.getInt("id"),
      authorId = rs.getInt("author"),
      topicId = rs.getInt("topic"),
      commentId = Some(rs.getInt("comment")).filter(_ != 0),
      postdate = rs.getTimestamp("postdate").toInstant,
      message = rs.getString("message"),
      warningType = WarningType.idToType(rs.getString("warning_type")),
      closedBy = Some(rs.getInt("closed_by")).filter(_ != 0),
      closedWhen = Option(rs.getTimestamp("closed_when")).map(_.toInstant))
  }

  def loadForTopic(topicId: Int, forModerator: Boolean): collection.Seq[Warning] = {
    val filter = if (forModerator) {
      ""
    } else {
      "and warning_type IN ('tag', 'spelling') "
    }

    namedJdbcTemplate.query("select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when " +
      "from message_warnings " +
      "where topic=:topic and comment is null " + filter +
      "order by postdate", Map("topic" -> topicId).asJava, mapper).asScala
  }

  def loadForComments(comments: Set[Int]): Map[Int, Seq[Warning]] = {
    val params = Map("list" -> comments.asJava)

    namedJdbcTemplate.query("select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when " +
      "from message_warnings " +
      "where comment in (:list) " +
      "order by postdate", params.asJava, mapper).asScala.toVector.groupBy(_.commentId.get)
  }

  def lastWarningsCount(userId: Int): Int =
    namedJdbcTemplate.queryForObject("select count(*) from message_warnings where " +
      "postdate > CURRENT_TIMESTAMP-'1 hour'::interval and author = :author",
      Map("author" -> userId).asJava, classOf[Int])

  def get(id: Int): Warning =
    namedJdbcTemplate.queryForObject("select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when " +
      "from message_warnings " +
      "where id=:id " +
      "order by postdate", Map("id" -> id).asJava, mapper)

  def clear(id: Int, byUserId: Int): Unit = {
    val params = Map(
      "byUserId" -> byUserId,
      "id" -> id
    )

    namedJdbcTemplate.update("update message_warnings set closed_by = :byUserId, closed_when = CURRENT_TIMESTAMP " +
      "where id=:id and closed_by is null", params.asJava)
  }
}
