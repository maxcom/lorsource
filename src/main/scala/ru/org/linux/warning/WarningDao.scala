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

package ru.org.linux.warning

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import scalikejdbc.*

@Repository
class WarningDao(springDB: SpringDB):

  def postWarning(topicId: Int, commentId: Option[Int], authorId: Int, message: String, warningType: WarningType)(using Transaction): Int =
    val commentSql = commentId.map(c => sqls"$c").getOrElse(sqls"NULL")
    sql"""INSERT INTO message_warnings (topic, comment, author, message, warning_type)
          VALUES ($topicId, $commentSql, $authorId, $message, ${warningType.id})
          RETURNING id""".map(rs => rs.int("id")).single.apply().get

  private def warningFromRs(rs: WrappedResultSet): Warning =
    Warning(
      id = rs.int("id"),
      authorId = rs.int("author"),
      topicId = rs.int("topic"),
      commentId = rs.intOpt("comment").filter(_ != 0),
      postdate = rs.timestamp("postdate").toInstant,
      message = rs.string("message"),
      warningType = WarningType.idToType(rs.string("warning_type")),
      closedBy = rs.intOpt("closed_by").filter(_ != 0),
      closedWhen = rs.timestampOpt("closed_when").map(_.toInstant)
    )

  def loadForTopic(topicId: Int, forModerator: Boolean): Seq[Warning] =
    val filter =
      if forModerator then
        SQLSyntax.empty
      else
        sqls"and warning_type IN ('tag', 'spelling')"

    springDB.run:
      sql"""select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when
            from message_warnings
            where topic=$topicId and comment is null $filter
            order by postdate""".map(warningFromRs).list.apply()
  end loadForTopic

  def loadForComments(comments: Set[Int]): Map[Int, Seq[Warning]] =
    if comments.isEmpty then
      Map.empty
    else
      springDB.run:
        sql"""select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when
              from message_warnings
              where comment IN ($comments)
              order by postdate""".map(warningFromRs).list.apply().toVector.groupBy(_.commentId.get)
  end loadForComments

  def lastWarningsCount(userId: Int): Int =
    springDB.run:
      sql"""select count(*) from message_warnings
            where postdate > CURRENT_TIMESTAMP-'1 hour'::interval and author = $userId"""
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def get(id: Int): Warning =
    springDB.run:
      sql"""select id, topic, comment, postdate, author, message, warning_type, closed_by, closed_when
            from message_warnings
            where id=$id
            order by postdate""".map(warningFromRs).single.apply().get

  def clear(id: Int, byUserId: Int)(using Transaction): Unit =
    sql"""update message_warnings set closed_by = $byUserId, closed_when = CURRENT_TIMESTAMP
          where id=$id and closed_by is null""".update.apply()
