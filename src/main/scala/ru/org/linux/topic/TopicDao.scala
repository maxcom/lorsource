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
import org.springframework.stereotype.Repository
import ru.org.linux.group.Group
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.section.SectionScrollModeEnum
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.user.User
import ru.org.linux.warning.RuleWarning
import scalikejdbc.*

import java.sql.Timestamp
import java.time.{LocalDate, OffsetDateTime, ZoneId}

@Repository
object TopicDao:
  def equalStrings(s1: String, s2: String): Boolean =
    if Strings.isNullOrEmpty(s1) then
      Strings.isNullOrEmpty(s2)
    else
      s1.equals(s2)

@Repository
class TopicDao(springDB: SpringDB):

  private def selectTopic(rs: WrappedResultSet): Topic = Topic.fromResultSet(rs.underlying)

  def getTimeFirstTopic: Timestamp =
    springDB.run:
      sql"SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp"
        .map(rs => rs.timestamp("min"))
        .single
        .apply()
        .orNull

  def updateLastmod(topicId: Int)(using Transaction): Unit =
    sql"UPDATE topics SET lastmod=lastmod+'1 second'::interval WHERE id=$topicId".update.apply()

  def getById(id: Int): Topic = findById(id).getOrElse(throw MessageNotFoundException(id))

  def findById(id: Int): Option[Topic] =
    springDB.run:
      sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
            topics.groupid as guid, topics.url, topics.linktext, ua_id,
            urlname, section, topics.sticky, topics.postip,
            COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
            commitdate, topics.stat1, postscore, topics.moderate, notop,
            topics.resolved, minor, draft, allow_anonymous, topics.reactions,
            COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
            FROM topics
            INNER JOIN groups ON (groups.id=topics.groupid)
            INNER JOIN sections ON (sections.id=groups.section)
            WHERE topics.id=$id""".map(selectTopic).single.apply()

  def getMessageForMonth(year: Int, month: Int): Seq[Int] =
    val start = LocalDate.of(year, month, 1).atStartOfDay.atZone(ZoneId.systemDefault).toOffsetDateTime
    val end = start.plusMonths(1)

    springDB.run:
      sql"SELECT id FROM topics WHERE postdate >= $start AND postdate < $end"
        .map(rs => rs.int("id"))
        .list
        .apply()
        .toVector

  def delete(msgid: Int)(using Transaction): Boolean =
    sql"UPDATE topics SET deleted='t',sticky='f' WHERE id=$msgid AND NOT deleted".update.apply() > 0

  def undelete(message: Topic)(using Transaction): Unit =
    sql"UPDATE topics SET deleted='f' WHERE id=${message.id}".update.apply()

  def saveNewMessage(msg: Topic, user: User, userAgent: String, group: Group)(using Transaction): Int =
    val msgid = sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val truncatedUserAgent = userAgent.substring(0, Math.min(511, userAgent.length))
    sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
          VALUES (${group.id}, ${user.id}, ${msg.title}, ${msg.url}, 'f', CURRENT_TIMESTAMP, $msgid, ${msg
          .linktext}, 'f', create_user_agent($truncatedUserAgent), ${msg.postIP}::inet, ${msg
          .draft}, CURRENT_TIMESTAMP, ${msg.allowAnonymous})""".update.apply()
    msgid

  def updateTitle(msgid: Int, title: String)(using Transaction): Unit =
    sql"UPDATE topics SET title=$title WHERE id=$msgid".update.apply()

  def updateLinktext(msgid: Int, linktext: String)(using Transaction): Unit =
    sql"UPDATE topics SET linktext=$linktext WHERE id=$msgid".update.apply()

  def updateUrl(msgid: Int, url: String)(using Transaction): Unit =
    sql"UPDATE topics SET url=$url WHERE id=$msgid".update.apply()

  def setMinor(msgid: Int, minor: Boolean)(using Transaction): Unit =
    sql"UPDATE topics SET minor=$minor WHERE id=$msgid".update.apply()

  def commit(msg: Topic, commiter: User)(using Transaction): Unit =
    sql"UPDATE topics SET moderate='t', commitby=${commiter
        .id}, commitdate=CURRENT_TIMESTAMP, lastmod=CURRENT_TIMESTAMP WHERE id=${msg.id}".update.apply()

  def publish(msg: Topic)(using Transaction): Unit =
    sql"UPDATE topics SET draft='f',postdate=CURRENT_TIMESTAMP,lastmod=CURRENT_TIMESTAMP WHERE id=${msg.id} AND draft"
      .update
      .apply()

  def uncommit(msg: Topic)(using Transaction): Unit =
    sql"UPDATE topics SET moderate='f',commitby=NULL,commitdate=NULL WHERE id=${msg.id}".update.apply()

  def getPreviousMessage(message: Topic, currentUser: User, scrollMode: SectionScrollModeEnum): Option[Topic] =
    if message.sticky then
      return None

    springDB.run {
      scrollMode match
        case SectionScrollModeEnum.SECTION =>
          sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                topics.groupid as guid, topics.url, topics.linktext, ua_id,
                urlname, section, topics.sticky, topics.postip,
                COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                commitdate, topics.stat1, postscore, topics.moderate, notop,
                topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                FROM topics
                INNER JOIN groups ON (groups.id=topics.groupid)
                INNER JOIN sections ON (sections.id=groups.section)
                WHERE topics.id=(SELECT t.id FROM topics t, groups g, sections s
                  WHERE NOT t.draft AND s.id=g.section AND t.commitdate<${message.commitDate}
                  AND t.groupid=g.id AND g.section=${message.sectionId}
                  AND (t.moderate OR NOT s.moderate) AND NOT t.deleted AND NOT t.sticky
                  ORDER BY t.commitdate DESC LIMIT 1)""".map(selectTopic).single.apply()

        case SectionScrollModeEnum.GROUP =>
          if currentUser == null || currentUser.anonymous then
            sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                  topics.groupid as guid, topics.url, topics.linktext, ua_id,
                  urlname, section, topics.sticky, topics.postip,
                  COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                  commitdate, topics.stat1, postscore, topics.moderate, notop,
                  topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                  COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                  FROM topics
                  INNER JOIN groups ON (groups.id=topics.groupid)
                  INNER JOIN sections ON (sections.id=groups.section)
                  WHERE NOT topics.draft AND topics.postdate<${message.postdate} AND topics.groupid=${message.groupId}
                  AND NOT topics.deleted AND NOT topics.sticky ORDER BY topics.postdate DESC LIMIT 1"""
              .map(selectTopic)
              .single
              .apply()
          else
            sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                  topics.groupid as guid, topics.url, topics.linktext, ua_id,
                  urlname, section, topics.sticky, topics.postip,
                  COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                  commitdate, topics.stat1, postscore, topics.moderate, notop,
                  topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                  COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                  FROM topics
                  INNER JOIN groups ON (groups.id=topics.groupid)
                  INNER JOIN sections ON (sections.id=groups.section)
                  WHERE NOT topics.draft AND topics.postdate<${message.postdate} AND topics.groupid=${message.groupId}
                  AND NOT topics.deleted AND NOT topics.sticky
                  AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=${currentUser.id})
                  ORDER BY topics.postdate DESC LIMIT 1""".map(selectTopic).single.apply()

        case SectionScrollModeEnum.NO_SCROLL | _ =>
          None
    }
  end getPreviousMessage

  def getNextMessage(message: Topic, currentUser: User, scrollMode: SectionScrollModeEnum): Option[Topic] =
    if message.sticky then
      return None

    springDB.run {
      scrollMode match
        case SectionScrollModeEnum.SECTION =>
          sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                topics.groupid as guid, topics.url, topics.linktext, ua_id,
                urlname, section, topics.sticky, topics.postip,
                COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                commitdate, topics.stat1, postscore, topics.moderate, notop,
                topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                FROM topics
                INNER JOIN groups ON (groups.id=topics.groupid)
                INNER JOIN sections ON (sections.id=groups.section)
                WHERE topics.id=(SELECT t.id FROM topics t, groups g, sections s
                  WHERE NOT t.draft AND s.id=g.section AND t.commitdate>${message.commitDate}
                  AND t.groupid=g.id AND g.section=${message.sectionId}
                  AND (t.moderate OR NOT s.moderate) AND NOT t.deleted AND NOT t.sticky
                  ORDER BY t.commitdate ASC LIMIT 1)""".map(selectTopic).single.apply()

        case SectionScrollModeEnum.GROUP =>
          if currentUser == null || currentUser.anonymous then
            sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                  topics.groupid as guid, topics.url, topics.linktext, ua_id,
                  urlname, section, topics.sticky, topics.postip,
                  COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                  commitdate, topics.stat1, postscore, topics.moderate, notop,
                  topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                  COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                  FROM topics
                  INNER JOIN groups ON (groups.id=topics.groupid)
                  INNER JOIN sections ON (sections.id=groups.section)
                  WHERE NOT topics.draft AND topics.postdate>${message.postdate} AND topics.groupid=${message.groupId}
                  AND NOT topics.deleted AND NOT topics.sticky ORDER BY topics.postdate ASC LIMIT 1"""
              .map(selectTopic)
              .single
              .apply()
          else
            sql"""SELECT postdate, topics.id as msgid, userid, topics.title,
                  topics.groupid as guid, topics.url, topics.linktext, ua_id,
                  urlname, section, topics.sticky, topics.postip,
                  COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
                  commitdate, topics.stat1, postscore, topics.moderate, notop,
                  topics.resolved, minor, draft, allow_anonymous, topics.reactions,
                  COALESCE(commitdate, topics.postdate) + sections.expire as expire_date, topics.open_warnings
                  FROM topics
                  INNER JOIN groups ON (groups.id=topics.groupid)
                  INNER JOIN sections ON (sections.id=groups.section)
                  WHERE NOT topics.draft AND topics.postdate>${message.postdate} AND topics.groupid=${message.groupId}
                  AND NOT topics.deleted AND NOT topics.sticky
                  AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=${currentUser.id})
                  ORDER BY topics.postdate ASC LIMIT 1""".map(selectTopic).single.apply()

        case SectionScrollModeEnum.NO_SCROLL | _ =>
          None
    }
  end getNextMessage

  def resolveMessage(msgid: Int, b: Boolean)(using Transaction): Unit =
    sql"UPDATE topics SET resolved=$b,lastmod=lastmod+'1 second'::interval WHERE id=$msgid".update.apply()

  def setTopicOptions(msg: Topic, postscore: Int, sticky: Boolean, notop: Boolean)(using Transaction): Unit =
    sql"UPDATE topics SET postscore=$postscore, sticky=$sticky, notop=$notop, lastmod=CURRENT_TIMESTAMP WHERE id=${msg
        .id}".update.apply()

  def changeGroup(msg: Topic, changeGroupId: Int)(using Transaction): Unit =
    sql"UPDATE topics SET groupid=$changeGroupId,lastmod=CURRENT_TIMESTAMP WHERE id=${msg.id}".update.apply()

  def moveTopic(msg: Topic, newGrp: Group)(using Transaction): Unit =
    val oldId =
      sql"SELECT groupid FROM topics WHERE id=${msg.id} FOR UPDATE".map(rs => rs.int("groupid")).single.apply().get

    if oldId != newGrp.id then
      sql"UPDATE topics SET groupid=${newGrp.id},lastmod=CURRENT_TIMESTAMP WHERE id=${msg.id}".update.apply()
      if !newGrp.linksAllowed then
        sql"UPDATE topics SET linktext=null, url=null WHERE id=${msg.id}".update.apply()
  end moveTopic

  def getUserTopicForUpdate(user: User)(using Transaction): Seq[Int] =
    sql"SELECT id FROM topics WHERE userid=${user.id} AND NOT deleted FOR UPDATE"
      .map(rs => rs.int("id"))
      .list
      .apply()
      .toVector

  def getAllByIPForUpdate(ip: String, startTime: Timestamp)(using Transaction): Seq[Int] =
    sql"SELECT id FROM topics WHERE postip=$ip::inet AND NOT deleted AND postdate>$startTime FOR UPDATE"
      .map(rs => rs.int("id"))
      .list
      .apply()
      .toVector

  def getUncommitedCounts: Seq[(Int, Int)] =
    springDB.run:
      sql"""select section, count(*) from topics,groups,sections where section=sections.id AND
            sections.moderate and not draft and topics.groupid=groups.id and not deleted and
            not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval)
            group by section order by section""".map(rs => rs.int("section") -> rs.int("count")).list.apply().toVector

  def getUncommitedCount(sectionId: Int): Int =
    springDB.run:
      sql"""SELECT count(*) FROM topics,groups,sections WHERE section=sections.id
            AND sections.moderate AND NOT draft AND topics.groupid=groups.id AND NOT deleted
            AND NOT topics.moderate AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval) AND section=$sectionId"""
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def countRecentTopics(userId: Int, sectionId: Int): Int =
    springDB.run:
      sql"""SELECT COUNT(*) FROM topics t
            LEFT JOIN del_info di ON di.msgid = t.id
            WHERE t.userid = $userId
            AND t.postdate >= (CURRENT_TIMESTAMP - '24 hours'::interval)
            AND NOT t.draft
            AND NOT (t.deleted AND (di.msgid IS NULL OR di.delby = t.userid))
            AND EXISTS (SELECT 1 FROM groups g WHERE g.id = t.groupid AND g.section = $sectionId)"""
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def countDrafts(author: User): Int =
    springDB.run:
      sql"SELECT COUNT(*) FROM topics WHERE draft AND userid=${author.id} AND NOT deleted"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def recalcWarningsCount(topicId: Int)(using Transaction): Unit =
    sql"""update topics set open_warnings = (select count(distinct mw.author) from message_warnings mw where mw.topic = topics.id
          and mw.comment is null and mw.closed_by is null and mw.warning_type=${RuleWarning.id} and
          mw.postdate > CURRENT_TIMESTAMP - '12 hours'::interval and
          mw.author in (select id from users where score>100)) where topics.id=$topicId""".update.apply()

  def recalcAllWarningsCount()(using Transaction): Unit =
    sql"""update topics set open_warnings = (select count(distinct mw.author) from message_warnings mw where mw.topic = topics.id
          and mw.comment is null and mw.closed_by is null and mw.warning_type=${RuleWarning.id} and
          mw.postdate > CURRENT_TIMESTAMP - '12 hours'::interval and
          mw.author in (select id from users where score>100)) where open_warnings > 0""".update.apply()

  def recalcAllWarningsCountInTx(): Unit = springDB.localTx(recalcAllWarningsCount())
