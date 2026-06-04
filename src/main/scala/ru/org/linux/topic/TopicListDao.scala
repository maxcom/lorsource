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

import org.springframework.stereotype.Repository
import ru.org.linux.auth.AnySession
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.SectionController
import ru.org.linux.topic.TopicListRequest.CommitMode
import ru.org.linux.topic.TopicListRequest.CommitMode.*
import ru.org.linux.topic.TopicListRequest.DateLimit
import ru.org.linux.user.User
import scalikejdbc.*

import scala.collection.mutable

object TopicListDao:
  private def makeConditions(request: TopicListRequest, currentUserOpt: Option[User]): SQLSyntax =
    val fragments = mutable.ListBuffer[SQLSyntax]()
    val sections = request.sections.filter(_ != 0).toSeq

    fragments += sqls"NOT deleted"

    currentUserOpt.foreach { user =>
      fragments +=
        sqls"AND ((sections.moderate AND commitdate is not null) OR userid NOT IN (select ignored from ignore_list where userid=${user
            .id}))"
    }

    if request.commitMode != CommitMode.All then
      request.commitMode match
        case CommittedOnly =>
          fragments += sqls"AND sections.moderate AND commitdate is not null"
        case UncommittedOnly =>
          fragments += sqls"AND (NOT topics.moderate) AND sections.moderate"
        case PostmoderatedOnly =>
          fragments += sqls"AND NOT sections.moderate"
        case CommittedAndPostmoderated =>
          fragments += sqls"AND (topics.moderate OR NOT sections.moderate)"
        case All =>

    if sections.nonEmpty then
      fragments += sqls"AND section IN ($sections)"

    if request.group != 0 then
      fragments += sqls"AND groupid=${request.group}"

    val dateField: SQLSyntax =
      if request.commitMode == CommittedOnly then
        sqls"commitdate"
      else
        sqls"postdate"

    request.dateLimit match
      case DateLimit.Between(from, to) =>
        fragments += sqls"AND $dateField >= $from AND $dateField < $to"
      case DateLimit.FromDate(from) =>
        fragments += sqls"AND $dateField >= $from"
      case DateLimit.NoLimit =>

    if request.userId != 0 then
      if request.userFavs then
        fragments += sqls"AND memories.userid=${request.userId}"
      else
        fragments += sqls"AND userid=${request.userId}"

      if request.userFavs then
        if request.userWatches then
          fragments += sqls"AND watch"
        else
          fragments += sqls"AND NOT watch"

    if request.notalks then
      fragments += sqls"AND not topics.groupid=8404"

    if request.tech then
      fragments += sqls"AND NOT topics.groupid IN (${SectionController.NonTech})"

    if request.tag != 0 then
      fragments += sqls"AND topics.id IN (SELECT msgid FROM tags WHERE tagid=${request.tag})"

    if !request.showDraft then
      fragments += sqls"AND NOT topics.draft"
    else
      fragments += sqls"AND topics.draft"

    SQLSyntax.join(fragments.toSeq, sqls" ")

  private def makeSortOrder(topicListRequest: TopicListRequest): SQLSyntax =
    if topicListRequest.userFavs then
      sqls"ORDER BY memories.id DESC"
    else
      topicListRequest.commitMode match
        case CommittedOnly =>
          sqls"ORDER BY commitdate DESC"
        case UncommittedOnly | PostmoderatedOnly =>
          sqls"ORDER BY postdate DESC"
        case CommittedAndPostmoderated | All =>
          sqls"ORDER BY COALESCE(commitdate, postdate) DESC"

  private def makeLimitAndOffset(topicListRequest: TopicListRequest): SQLSyntax =
    val parts = mutable.ListBuffer[SQLSyntax]()

    topicListRequest
      .limit
      .foreach { limit =>
        parts += sqls"LIMIT $limit"
      }

    topicListRequest
      .offset
      .foreach { offset =>
        parts += sqls"OFFSET $offset"
      }

    if parts.isEmpty then
      SQLSyntax.empty
    else
      SQLSyntax.join(parts.toSeq, sqls" ")

@Repository
class TopicListDao(springDB: SpringDB):

  def getTopics(topicListRequest: TopicListRequest, currentUser: AnySession): collection.Seq[Topic] =
    val conditions = TopicListDao.makeConditions(topicListRequest, currentUser.userOpt)
    val sort = TopicListDao.makeSortOrder(topicListRequest)
    val limitOffset = TopicListDao.makeLimitAndOffset(topicListRequest)

    val fromClause =
      if topicListRequest.userFavs then
        sqls"""FROM topics
              INNER JOIN groups ON (groups.id=topics.groupid)
              INNER JOIN sections ON (sections.id=groups.section)
              INNER JOIN memories ON (memories.topic = topics.id)"""
      else
        sqls"""FROM topics
              INNER JOIN groups ON (groups.id=topics.groupid)
              INNER JOIN sections ON (sections.id=groups.section)"""

    springDB.run:
      sql"""SELECT postdate, topics.id as msgid, topics.userid, topics.title, topics.groupid as guid, topics.url,
            topics.linktext, ua_id, urlname, section, topics.sticky, topics.postip,
            COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
            commitdate, topics.stat1, postscore, topics.moderate, notop, topics.resolved, minor, draft, allow_anonymous,
            topics.reactions, COALESCE(commitdate, postdate) + sections.expire as expire_date,
            topics.open_warnings
            $fromClause
            WHERE $conditions $sort $limitOffset""".map(rs => Topic.fromResultSet(rs.underlying)).list.apply().toSeq

  private val DeletionBlockReason = "Блокировка пользователя с удалением сообщений"
  private val DeletionSpamReason = "4.6 Спам"

  /** Возвращает удаленные темы в премодерируемом разделе.
    *
    * Темы, удаленные автором пропускаются.
    *
    * @param sectionId
    *   номер раздела или 0 для всех премодерируемых
    * @param skipBadReason
    *   Пропускать темы, удаленные с пустым комментарием и спам
    * @return
    *   список удаленных тем
    */
  def getDeletedTopics(sectionId: Int, skipBadReason: Boolean): Seq[DeletedTopic] =
    val fragments = mutable.ListBuffer[SQLSyntax](sqls"""topics.title as subj, nick, groups.section, topics.id as msgid,
            reason, topics.postdate, del_info.delDate, bonus
            FROM topics,groups,users,sections,del_info
            WHERE sections.id=groups.section AND topics.userid=users.id
            AND topics.groupid=groups.id AND sections.moderate AND deleted
            AND del_info.msgid=topics.id AND topics.userid!=del_info.delby
            AND delDate > CURRENT_TIMESTAMP - '2 weeks'::interval""")

    if skipBadReason then
      fragments += sqls"AND reason!='' AND reason!=${DeletionBlockReason} AND reason!=${DeletionSpamReason}"

    if sectionId != 0 then
      fragments += sqls"AND section=$sectionId"

    fragments += sqls"ORDER BY del_info.delDate DESC LIMIT 20"

    val query = SQLSyntax.join(fragments.toSeq, sqls" ")

    springDB.run:
      sql"SELECT $query".map(rs => DeletedTopic(rs.underlying)).list.apply().toSeq

  def getDeletedUserTopics(user: User, topics: Int): Seq[DeletedTopic] =
    springDB.run:
      sql"""SELECT topics.title as subj, nick, groups.section, topics.id as msgid, reason, topics.postdate,
            del_info.delDate, bonus FROM topics,groups,users,del_info
            WHERE topics.userid=users.id AND topics.groupid=groups.id AND deleted AND del_info.msgid=topics.id
            AND delDate is not null AND topics.userid = ${user.id}
            ORDER BY del_info.delDate DESC LIMIT $topics""".map(rs => DeletedTopic(rs.underlying)).list.apply().toSeq

  def getUserSections(user: User): Seq[Int] =
    springDB.run:
      sql"""select distinct section from
            groups join topics on topics.groupid=groups.id
            where topics.userid=${user.id} and not deleted and not draft ORDER BY section"""
        .map(rs => rs.int("section"))
        .list
        .apply()
