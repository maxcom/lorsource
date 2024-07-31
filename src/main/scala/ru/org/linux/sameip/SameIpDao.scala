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

package ru.org.linux.sameip

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.util.StringUtil

import java.sql.ResultSet
import javax.sql.DataSource
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MutableMapHasAsJava}

@Repository
class SameIpDao(dataSource: DataSource) {
  private val namedJdbcTemplate: NamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)

  def getComments(ip: Option[String], userAgent: Option[Int], score: Option[Int], limit: Int): collection.Seq[PostListItem] = {
    val ipQuery: String = if (ip.isDefined) {
      "AND m.postip <<= :ip::inet "
    } else {
      ""
    }

    val userAgentQuery: String = if (userAgent.isDefined) {
      "AND m.ua_id=:userAgent "
    } else {
      ""
    }

    val scoreQuery = if (score.isDefined) {
      "AND m.userid IN (SELECT id FROM users WHERE score < :score OR id = 2)"
    } else {
      ""
    }

    val params = mutable.HashMap[String, AnyRef]()

    ip.foreach(ip => params.put("ip", ip))
    userAgent.foreach(userAgent => params.put("userAgent", Integer.valueOf(userAgent)))
    score.foreach(score => params.put("score", Integer.valueOf(score)))

    params.put("limit", Integer.valueOf(limit))

    namedJdbcTemplate.query(
      "SELECT groups.title as group_title, topics.title, topics.id as topic_id, " +
        "m.id as cid, m.postdate, m.deleted, del_info.reason, " +
        "m.userid " +
        "FROM groups JOIN topics ON groups.id=topics.groupid " +
        "JOIN comments m ON m.topic=topics.id " + "LEFT JOIN del_info ON del_info.msgid=m.id " +
        "WHERE m.postdate>CURRENT_TIMESTAMP-'5 days'::interval " + ipQuery + userAgentQuery + scoreQuery +
      "UNION ALL " +
        "SELECT groups.title as group_title, m.title, m.id as topic_id, " +
        "0 as cid, m.postdate, m.deleted, del_info.reason, " +
        "m.userid " +
        "FROM groups JOIN topics m ON groups.id=m.groupid " +
        "LEFT JOIN del_info ON del_info.msgid=m.id " +
        "WHERE m.postdate>CURRENT_TIMESTAMP-'5 days'::interval " + ipQuery + userAgentQuery + scoreQuery +
      "ORDER BY postdate DESC LIMIT :limit",
      params.asJava, (rs: ResultSet, _: Int) =>
        PostListItem(
          groupTitle = rs.getString("group_title"),
          topicId = rs.getInt("topic_id"),
          title = StringUtil.makeTitle(rs.getString("title")),
          reason = rs.getString("reason"),
          commentId = Some(rs.getInt("cid")).filter(_ != 0),
          deleted = rs.getBoolean("deleted"),
          postdate = rs.getTimestamp("postdate"),
          authorId = rs.getInt("userid"))).asScala
  }
}
