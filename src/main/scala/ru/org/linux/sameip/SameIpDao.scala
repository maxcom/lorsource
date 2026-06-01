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

package ru.org.linux.sameip

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.util.StringUtil
import scalikejdbc.*

@Repository
class SameIpDao(springDB: SpringDB):
  def getComments(
      ip: Option[String],
      userAgent: Option[Int],
      score: Option[Int],
      limit: Int): collection.Seq[PostListItem] =
    val ipQuery = ip.map(v => sqls"AND m.postip <<= ${v}::inet").getOrElse(SQLSyntax.empty)
    val userAgentQuery = userAgent.map(v => sqls"AND m.ua_id=${v}").getOrElse(SQLSyntax.empty)
    val scoreQuery = score
      .map(v => sqls"AND m.userid IN (SELECT id FROM users WHERE score < ${v} OR id = 2)")
      .getOrElse(SQLSyntax.empty)

    springDB.run:
      sql"""
        SELECT groups.title as group_title, topics.title, topics.id as topic_id,
               m.id as cid, m.postdate, m.deleted, del_info.reason,
               m.userid
        FROM groups JOIN topics ON groups.id=topics.groupid
                    JOIN comments m ON m.topic=topics.id
                    LEFT JOIN del_info ON del_info.msgid=m.id
        WHERE m.postdate>CURRENT_TIMESTAMP-'5 days'::interval
              $ipQuery $userAgentQuery $scoreQuery
        UNION ALL
        SELECT groups.title as group_title, m.title, m.id as topic_id,
               0 as cid, m.postdate, m.deleted, del_info.reason,
               m.userid
        FROM groups JOIN topics m ON groups.id=m.groupid
                    LEFT JOIN del_info ON del_info.msgid=m.id
        WHERE m.postdate>CURRENT_TIMESTAMP-'5 days'::interval
              $ipQuery $userAgentQuery $scoreQuery
        ORDER BY postdate DESC LIMIT $limit
      """
        .map { rs =>
          PostListItem(
            groupTitle = rs.string("group_title"),
            topicId = rs.int("topic_id"),
            title = StringUtil.makeTitle(rs.string("title")),
            reason = rs.stringOpt("reason"),
            commentId = Option(rs.int("cid")).filter(_ != 0),
            deleted = rs.boolean("deleted"),
            postdate = rs.underlying.getTimestamp("postdate"),
            authorId = rs.int("userid")
          )
        }
        .list
        .apply()
