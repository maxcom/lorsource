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
import io.circe.syntax.*
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.comment.Comment
import ru.org.linux.topic.Topic
import ru.org.linux.user.User

import javax.sql.DataSource

// reaction -> Seq[UserId]
case class Reactions(reactions: Map[Int, String])

object Reactions {
  val empty: Reactions = Reactions(Map.empty)
}

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
class ReactionDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def setCommentReaction(comment: Comment, user: User, reaction: String, set: Boolean): Int = {
    if (set) {
      val add = Map(user.getId -> reaction).asJson.noSpaces

      jdbcTemplate.update("UPDATE comments SET reactions = reactions || ? WHERE id=?", add, comment.id)
    } else {
      jdbcTemplate.update("UPDATE comments SET reactions = reactions - ? WHERE id=?", user.getId.toString, comment.id)
    }

    val r = jdbcTemplate.queryForObject[String]("SELECT reactions FROM comments WHERE id=?", comment.id)

    ReactionDao.parse(r.get).reactions.values.count(_ == reaction)
  }

  def setTopicReaction(topic: Topic, user: User, reaction: String, set: Boolean): Int = {
    if (set) {
      val add = Map(user.getId -> reaction).asJson.noSpaces

      jdbcTemplate.update("UPDATE topics SET reactions = reactions || ? WHERE id=?", add, topic.id)
    } else {
      jdbcTemplate.update("UPDATE topics SET reactions = reactions - ? WHERE id=?", user.getId.toString, topic.id)
    }

    val r = jdbcTemplate.queryForObject[String]("SELECT reactions FROM topics WHERE id=?", topic.id)

    ReactionDao.parse(r.get).reactions.values.count(_ == reaction)
  }
}
