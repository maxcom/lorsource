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

package ru.org.linux.topic

import com.google.common.base.Strings
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.group.Group
import ru.org.linux.section.SectionScrollModeEnum
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.user.User
import ru.org.linux.warning.RuleWarning

import javax.sql.DataSource
import java.sql.Timestamp
import java.time.{LocalDate, ZoneId, ZonedDateTime}
import scala.jdk.CollectionConverters.*

@Repository
object TopicDao:
  private val QueryMessage =
    "SELECT " + "postdate, topics.id as msgid, userid, topics.title, " +
      "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
      "urlname, section, topics.sticky, topics.postip, " +
      "COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
      "commitdate, topics.stat1, postscore, topics.moderate, notop, " +
      "topics.resolved, minor, draft, allow_anonymous, topics.reactions, " +
      "COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, " + "topics.open_warnings " +
      "FROM topics " + "INNER JOIN groups ON (groups.id=topics.groupid) " +
      "INNER JOIN sections ON (sections.id=groups.section) " + "WHERE topics.id=?"

  private val QueryTopicsIdByTime = "SELECT id FROM topics WHERE postdate>=? AND postdate<?"

  def equalStrings(s1: String, s2: String): Boolean =
    if Strings.isNullOrEmpty(s1) then
      Strings.isNullOrEmpty(s2)
    else
      s1.equals(s2)

@Repository
class TopicDao(dataSource: DataSource):
  private val jdbcTemplate: JdbcTemplate = new JdbcTemplate(dataSource)
  private val namedJdbcTemplate: NamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)

  def getTimeFirstTopic: Timestamp =
    jdbcTemplate.queryForObject(
      "SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp",
      classOf[Timestamp])

  def updateLastmod(topicId: Int): Unit =
    jdbcTemplate.update("UPDATE topics SET lastmod=lastmod+'1 second'::interval WHERE id=?", topicId)

  def getById(id: Int): Topic = findById(id).getOrElse(throw MessageNotFoundException(id))

  def findById(id: Int): Option[Topic] =
    try
      Some(
        jdbcTemplate.queryForObject(
          TopicDao.QueryMessage,
          (resultSet: java.sql.ResultSet, _: Int) => Topic.fromResultSet(resultSet),
          id))
    catch
      case _: EmptyResultDataAccessException =>
        None

  def getMessageForMonth(year: Int, month: Int): Seq[Int] =
    val start = LocalDate.of(year, month, 1).atStartOfDay.atZone(ZoneId.systemDefault)
    val end = start.plusMonths(1)

    jdbcTemplate
      .query(
        TopicDao.QueryTopicsIdByTime,
        (resultSet: java.sql.ResultSet, _: Int) => resultSet.getInt("id"),
        start.toOffsetDateTime,
        end.toOffsetDateTime)
      .asScala
      .view
      .map(_.intValue())
      .toVector

  def delete(msgid: Int): Boolean =
    jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=? AND NOT deleted", msgid) > 0

  def undelete(message: Topic): Unit = jdbcTemplate.update("UPDATE topics SET deleted='f' WHERE id=?", message.id)

  private def allocateMsgid: Int = jdbcTemplate.queryForObject("select nextval('s_msgid') as msgid", classOf[Int])

  def saveNewMessage(msg: Topic, user: User, userAgent: String, group: Group): Int =
    val msgid = allocateMsgid

    jdbcTemplate.update(
      "INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous) VALUES (?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f', create_user_agent(?),?::inet, ?, CURRENT_TIMESTAMP, ?)",
      group.id.asInstanceOf[AnyRef],
      user.id.asInstanceOf[AnyRef],
      msg.title,
      msg.url,
      Integer.valueOf(msgid),
      msg.linktext,
      userAgent,
      msg.postIP,
      java.lang.Boolean.valueOf(msg.draft),
      java.lang.Boolean.valueOf(msg.allowAnonymous)
    )

    msgid

  def updateTitle(msgid: Int, title: String): Unit =
    namedJdbcTemplate.update(
      "UPDATE topics SET title=:title WHERE id=:id",
      Map("title" -> title, "id" -> msgid.asInstanceOf[AnyRef]).asJava)

  def updateLinktext(msgid: Int, linktext: String): Unit =
    namedJdbcTemplate.update(
      "UPDATE topics SET linktext=:linktext WHERE id=:id",
      Map("linktext" -> linktext, "id" -> msgid.asInstanceOf[AnyRef]).asJava)

  def updateUrl(msgid: Int, url: String): Unit =
    namedJdbcTemplate.update(
      "UPDATE topics SET url=:url WHERE id=:id",
      Map("url" -> url, "id" -> msgid.asInstanceOf[AnyRef]).asJava)

  def setMinor(msgid: Int, minor: Boolean): Unit =
    namedJdbcTemplate.update(
      "UPDATE topics SET minor=:minor WHERE id=:id",
      Map("minor" -> java.lang.Boolean.valueOf(minor), "id" -> msgid.asInstanceOf[AnyRef]).asJava)

  def commit(msg: Topic, commiter: User): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET moderate='t', commitby=?, commitdate=CURRENT_TIMESTAMP, lastmod=CURRENT_TIMESTAMP WHERE id=?",
      commiter.id.asInstanceOf[AnyRef],
      msg.id.asInstanceOf[AnyRef]
    )

  def publish(msg: Topic): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET draft='f',postdate=CURRENT_TIMESTAMP,lastmod=CURRENT_TIMESTAMP WHERE id=? AND draft",
      msg.id.asInstanceOf[AnyRef])

  def uncommit(msg: Topic): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET moderate='f',commitby=NULL,commitdate=NULL WHERE id=?",
      msg.id.asInstanceOf[AnyRef])

  def getPreviousMessage(message: Topic, currentUser: User, scrollMode: SectionScrollModeEnum): Option[Topic] =
    if message.sticky then
      return None

    val res =
      scrollMode match
        case SectionScrollModeEnum.SECTION =>
          jdbcTemplate.queryForList(
            "SELECT topics.id as msgid " + "FROM topics " + "WHERE topics.commitdate=" +
              "(SELECT commitdate FROM topics, groups, sections WHERE NOT draft AND sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky ORDER BY commitdate DESC LIMIT 1)",
            classOf[Integer],
            message.commitDate,
            message.sectionId
          )

        case SectionScrollModeEnum.GROUP =>
          if currentUser == null || currentUser.anonymous then
            jdbcTemplate.queryForList(
              "SELECT topics.id " + "FROM topics " +
                "WHERE NOT draft AND topics.postdate<? AND topics.groupid=? AND NOT deleted AND NOT sticky ORDER BY postdate DESC LIMIT 1",
              classOf[Integer],
              message.postdate,
              message.groupId
            )
          else
            jdbcTemplate.queryForList(
              "SELECT topics.id as msgid " + "FROM topics " +
                "WHERE NOT draft AND topics.postdate<? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                "AND userid NOT IN (select ignored from ignore_list where userid=?) ORDER BY postdate DESC LIMIT 1",
              classOf[Integer],
              message.postdate,
              message.groupId,
              currentUser.id
            )

        case SectionScrollModeEnum.NO_SCROLL | _ =>
          return None

    if res.isEmpty || res.get(0) == null then
      None
    else
      try
        Some(getById(res.get(0).intValue()))
      catch
        case _: MessageNotFoundException =>
          None
  end getPreviousMessage

  def getNextMessage(message: Topic, currentUser: User, scrollMode: SectionScrollModeEnum): Option[Topic] =
    if message.sticky then
      return None

    val res =
      scrollMode match
        case SectionScrollModeEnum.SECTION =>
          jdbcTemplate.queryForList(
            "SELECT topics.id as msgid " + "FROM topics " + "WHERE topics.commitdate=" +
              "(SELECT commitdate FROM topics, groups, sections WHERE NOT draft AND sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky ORDER BY commitdate ASC LIMIT 1)",
            classOf[Integer],
            message.commitDate,
            message.sectionId
          )

        case SectionScrollModeEnum.GROUP =>
          if currentUser == null || currentUser.anonymous then
            jdbcTemplate.queryForList(
              "SELECT topics.id as msgid " + "FROM topics " +
                "WHERE NOT draft AND topics.postdate>? AND topics.groupid=? AND NOT deleted AND NOT sticky ORDER BY postdate ASC LIMIT 1",
              classOf[Integer],
              message.postdate,
              message.groupId
            )
          else
            jdbcTemplate.queryForList(
              "SELECT topics.id as msgid " + "FROM topics " +
                "WHERE NOT draft AND topics.postdate>? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                "AND userid NOT IN (select ignored from ignore_list where userid=?) ORDER BY postdate ASC LIMIT 1",
              classOf[Integer],
              message.postdate,
              message.groupId,
              currentUser.id
            )

        case SectionScrollModeEnum.NO_SCROLL | _ =>
          return None

    if res.isEmpty || res.get(0) == null then
      None
    else
      try
        Some(getById(res.get(0).intValue()))
      catch
        case _: MessageNotFoundException =>
          None
  end getNextMessage

  def resolveMessage(msgid: Int, b: Boolean): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET resolved=?,lastmod=lastmod+'1 second'::interval WHERE id=?",
      java.lang.Boolean.valueOf(b),
      msgid.asInstanceOf[AnyRef])

  def setTopicOptions(msg: Topic, postscore: Int, sticky: Boolean, notop: Boolean): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET postscore=?, sticky=?, notop=?, lastmod=CURRENT_TIMESTAMP WHERE id=?",
      postscore.asInstanceOf[AnyRef],
      java.lang.Boolean.valueOf(sticky),
      java.lang.Boolean.valueOf(notop),
      msg.id.asInstanceOf[AnyRef]
    )

  def changeGroup(msg: Topic, changeGroupId: Int): Unit =
    jdbcTemplate.update(
      "UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?",
      changeGroupId.asInstanceOf[AnyRef],
      msg.id.asInstanceOf[AnyRef])

  def moveTopic(msg: Topic, newGrp: Group): Unit =
    val oldId = jdbcTemplate.queryForObject(
      "SELECT groupid FROM topics WHERE id=? FOR UPDATE",
      classOf[Integer],
      msg.id.asInstanceOf[AnyRef])

    if oldId == newGrp.id then
      return

    changeGroup(msg, newGrp.id)

    if !newGrp.linksAllowed then
      jdbcTemplate.update("UPDATE topics SET linktext=null, url=null WHERE id=?", msg.id.asInstanceOf[AnyRef])
  end moveTopic

  def getUserTopicForUpdate(user: User): Seq[Int] =
    jdbcTemplate
      .queryForList(
        "SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE",
        classOf[Integer],
        user.id.asInstanceOf[AnyRef])
      .asScala
      .view
      .map(_.intValue())
      .toVector

  def getAllByIPForUpdate(ip: String, startTime: Timestamp): Seq[Int] =
    jdbcTemplate
      .queryForList(
        "SELECT id FROM topics WHERE postip=?::inet AND not deleted AND postdate>? FOR UPDATE",
        classOf[Integer],
        ip,
        startTime)
      .asScala
      .view
      .map(_.intValue())
      .toVector

  def getUncommitedCounts: Seq[(Int, Int)] =
    jdbcTemplate
      .query(
        "select section, count(*) from topics,groups,sections where section=sections.id AND " +
          "sections.moderate and not draft and topics.groupid=groups.id and not deleted and " +
          "not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval) " +
          "group by section order by section",
        (rs: java.sql.ResultSet, _: Int) => rs.getInt("section") -> rs.getInt("count")
      )
      .asScala
      .view
      .map(p => p._1 -> p._2)
      .toVector

  def getUncommitedCount(sectionId: Int): Int =
    val count = jdbcTemplate.queryForObject(
      "SELECT count(*) FROM topics,groups,sections WHERE section=sections.id " +
        "AND sections.moderate AND NOT draft AND topics.groupid=groups.id AND NOT deleted " +
        "AND NOT topics.moderate AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval) " + "AND section=?",
      classOf[Integer],
      sectionId.asInstanceOf[AnyRef]
    )

    if count != null then
      count.intValue()
    else
      0

  def countRecentTopics(userId: Int, sectionId: Int): Int = {
    val count = jdbcTemplate.queryForObject(
      """SELECT COUNT(*) FROM topics t
        |LEFT JOIN del_info di ON di.msgid = t.id
        |WHERE t.userid = ?
        |AND t.postdate >= (CURRENT_TIMESTAMP - '24 hours'::interval)
        |AND NOT t.draft
        |AND NOT (t.deleted AND (di.msgid IS NULL OR di.delby = t.userid))
        |AND EXISTS (SELECT 1 FROM groups g WHERE g.id = t.groupid AND g.section = ?)""".stripMargin,
      classOf[Integer],
      userId.asInstanceOf[AnyRef],
      sectionId.asInstanceOf[AnyRef]
    )

    if count != null then
      count.intValue()
    else
      0
  }

  def hasDrafts(author: User): Boolean =
    val res = jdbcTemplate.queryForList(
      "select id FROM topics WHERE draft AND userid=? LIMIT 1",
      classOf[Integer],
      author.id.asInstanceOf[AnyRef])

    !res.isEmpty

  def recalcWarningsCount(topicId: Int): Unit =
    jdbcTemplate.update(
      """update topics set open_warnings = (select count(distinct mw.author) from message_warnings mw where mw.topic = topics.id
        | and mw.comment is null and mw.closed_by is null and mw.warning_type=? and
        | mw.postdate > CURRENT_TIMESTAMP - '12 hours'::interval and
        | mw.author in (select id from users where score>100)) where topics.id=?""".stripMargin,
      RuleWarning.id.asInstanceOf[AnyRef],
      topicId.asInstanceOf[AnyRef]
    )

  def recalcAllWarningsCount(): Unit =
    jdbcTemplate.update(
      """update topics set open_warnings = (select count(distinct mw.author) from message_warnings mw where mw.topic = topics.id
        | and mw.comment is null and mw.closed_by is null and mw.warning_type=? and
        | mw.postdate > CURRENT_TIMESTAMP - '12 hours'::interval and
        | mw.author in (select id from users where score>100)) where open_warnings > 0""".stripMargin,
      RuleWarning.id.asInstanceOf[AnyRef]
    )
