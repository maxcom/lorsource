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
package ru.org.linux.reaction

import com.typesafe.scalalogging.StrictLogging
import io.circe.*
import io.circe.parser.*
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.comment.Comment
import ru.org.linux.user.User

import javax.sql.DataSource

// reaction -> Seq[UserId]
case class Reactions(reactions: Map[String, Seq[Int]])

object Reactions {
  val empty: Reactions = Reactions(Map.empty)
}

object ReactionDao extends StrictLogging {
  def parse(json: String): Reactions = {
    val parsed: Either[Error, Map[String, Seq[Int]]] = decode[Map[String, Seq[Int]]](json)

    Reactions(parsed.toTry.recover { ex =>
      logger.warn("Can't parse reactions", ex)

      Map.empty[String, Seq[Int]]
    }.get)
  }
}

@Repository
class ReactionDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def setCommentReaction(comment: Comment, user: User, reaction: String, set: Boolean): Unit = {
    val basePath = s"{$reaction}"

    if (set) {
      jdbcTemplate.update(
        "UPDATE comments SET reactions = jsonb_insert(reactions, ?, '[]') WHERE id=?" +
          " AND reactions->? is null", basePath, comment.id, reaction)

      val path = s"{$reaction, -1}"

      jdbcTemplate.update(
        "UPDATE comments SET reactions = jsonb_insert(reactions, ?, ?, true) WHERE id=?" +
          " AND NOT (reactions->? @> ?)",
        path, user.getId.toString, comment.id, reaction, user.getId.toString)
    } else {
      jdbcTemplate.update(
        "UPDATE comments SET reactions = jsonb_set(reactions, ?, " +
          "COALESCE((SELECT jsonb_agg(val) FROM jsonb_array_elements(reactions->?) x(val) where val!=?::jsonb), '[]')) WHERE id=?",
        basePath, reaction, user.getId.toString, comment.id)

    }
  }
}
