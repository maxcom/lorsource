package ru.org.linux.sameip

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.comment.CommentsListItem
import ru.org.linux.util.StringUtil

import java.sql.ResultSet
import javax.sql.DataSource
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MutableMapHasAsJava}

@Repository
class SameIpDao(dataSource: DataSource) {
  private val namedJdbcTemplate: NamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)

  def getComments(ip: Option[String], userAgent: Option[Int], limit: Int): collection.Seq[PostListItem] = {
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

    val params = mutable.HashMap[String, AnyRef]()

    ip.foreach(ip => params.put("ip", ip))
    userAgent.foreach(userAgent => params.put("userAgent", Integer.valueOf(userAgent)))
    params.put("limit", Integer.valueOf(limit))

    namedJdbcTemplate.query(
      "SELECT groups.title as group_title, topics.title, topics.id as topic_id, " +
        "m.id as cid, m.postdate, m.deleted, del_info.reason, " +
        "m.userid " +
        "FROM groups JOIN topics ON groups.id=topics.groupid " +
        "JOIN comments m ON m.topic=topics.id " + "LEFT JOIN del_info ON del_info.msgid=m.id " +
        "WHERE m.postdate>CURRENT_TIMESTAMP-'3 days'::interval " + ipQuery + userAgentQuery +
      "UNION ALL " +
        "SELECT groups.title as group_title, m.title, m.id as topic_id, " +
        "0 as cid, m.postdate, m.deleted, del_info.reason, " +
        "m.userid " +
        "FROM groups JOIN topics m ON groups.id=m.groupid " +
        "LEFT JOIN del_info ON del_info.msgid=m.id " +
        "WHERE m.postdate>CURRENT_TIMESTAMP-'3 days'::interval " + ipQuery + userAgentQuery +
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
