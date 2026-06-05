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
package ru.org.linux.group

import org.springframework.stereotype.Repository
import ru.org.linux.auth.AnySession
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.Section
import ru.org.linux.section.SectionController.NonTech
import ru.org.linux.topic.TopicPermissionService
import ru.org.linux.tracker.TrackerFilterEnum
import ru.org.linux.util.StringUtil
import ru.org.linux.warning.WarningService.TopicMaxWarnings
import scalikejdbc.*

object GroupListDao:
  private def queryPartCommentIgnored(userId: Int): SQLSyntax =
    sqls"AND NOT EXISTS (SELECT ignored FROM ignore_list WHERE userid=${userId} INTERSECT SELECT get_branch_authors(comments.id))"

  private def queryPartIgnored(userId: Int): SQLSyntax =
    sqls"AND userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=${userId})"

  private def queryPartTagIgnored(userId: Int): SQLSyntax =
    sqls"AND topics.id NOT IN (SELECT tags.msgid FROM tags, user_tags WHERE tags.tagid=user_tags.tag_id AND user_tags.is_favorite=false AND user_id=${userId} EXCEPT SELECT tags.msgid FROM tags, user_tags WHERE tags.tagid=user_tags.tag_id AND user_tags.is_favorite=true AND user_id=${userId})"

  private val NoUncommited: SQLSyntax = sqls"AND (topics.moderate OR NOT sections.moderate)"

@Repository
class GroupListDao(springDB: SpringDB):

  def getGroupTrackerTopics(groupid: Int, offset: Int, tagId: Option[Int])(using
      session: AnySession): collection.Seq[TopicsListItem] =
    val partFilter = SQLSyntax.join(
      Seq(
        sqls"AND topics.groupid = ${groupid}",
        tagId.map(t => sqls"AND topics.id IN (SELECT msgid FROM tags WHERE tagid=${t})").getOrElse(SQLSyntax.empty),
        sqls"AND lastmod > CURRENT_TIMESTAMP - '6 month'::interval"
      ),
      sqls" "
    )

    load(
      partFilter = partFilter,
      topics = session.profile.topics,
      offset = offset,
      sortByTopic = false,
      commentInterval = sqls"AND comments.postdate > CURRENT_TIMESTAMP - '6 month'::interval",
      topicInterval = sqls"t.postdate > CURRENT_TIMESTAMP - '6 month'::interval",
      showIgnored = false,
      showDeleted = false,
      showUncommited = false
    )

  def getGroupListTopics(
      groupid: Int,
      offset: Int,
      showIgnored: Boolean,
      showDeleted: Boolean,
      yearMonth: Option[(Int, Int)],
      tagId: Option[Int])(using session: AnySession): collection.Seq[TopicsListItem] =
    val dateInterval: SQLSyntax = yearMonth
      .map { (year, month) =>
        sqls"postdate >= make_date(${year}, ${month}, 1)::timestamp AND postdate < make_date(${year}, ${month}, 1)::timestamp + '1 month'::interval"
      }
      .getOrElse(sqls"postdate > CURRENT_TIMESTAMP - '6 month'::interval")

    val partFilter = SQLSyntax.join(
      Seq(
        sqls"AND topics.groupid = ${groupid}",
        sqls"AND NOT topics.sticky",
        sqls"AND ${dateInterval}",
        tagId.map(t => sqls"AND topics.id IN (SELECT msgid FROM tags WHERE tagid=${t})").getOrElse(SQLSyntax.empty)
      ),
      sqls" "
    )

    load(
      partFilter = partFilter,
      topics = session.profile.topics,
      offset = offset,
      sortByTopic = true,
      commentInterval = SQLSyntax.empty,
      topicInterval = SQLSyntax.empty,
      showIgnored = showIgnored,
      showDeleted = showDeleted,
      showUncommited = false
    )

  def getSectionListTopics(section: Section, offset: Int, tagId: Int)(using
      session: AnySession): collection.Seq[TopicsListItem] =
    val partFilter = SQLSyntax.join(
      Seq(sqls"AND section = ${section.id}", sqls"AND topics.id IN (SELECT msgid FROM tags WHERE tagid=${tagId})"),
      sqls" ")

    load(
      partFilter = partFilter,
      topics = session.profile.topics,
      offset = offset,
      sortByTopic = true,
      commentInterval = SQLSyntax.empty,
      topicInterval = SQLSyntax.empty,
      showIgnored = false,
      showDeleted = false,
      showUncommited = false
    )

  def getGroupStickyTopics(group: Group, tagId: Option[Int])(using
      session: AnySession): collection.Seq[TopicsListItem] =
    val partFilter = SQLSyntax.join(
      Seq(
        sqls"AND topics.groupid = ${group.id}",
        sqls"AND topics.sticky",
        tagId.map(t => sqls"AND topics.id IN (SELECT msgid FROM tags WHERE tagid=${t})").getOrElse(SQLSyntax.empty)
      ),
      sqls" "
    )

    load(
      partFilter = partFilter,
      topics = 100,
      offset = 0,
      sortByTopic = true,
      commentInterval = SQLSyntax.empty,
      topicInterval = SQLSyntax.empty,
      showIgnored = true,
      showDeleted = false,
      showUncommited = false
    )

  def getTrackerTopics(filter: TrackerFilterEnum, offset: Int)(using
      session: AnySession): collection.Seq[TopicsListItem] =
    val sectionFilter: SQLSyntax =
      filter match
        case TrackerFilterEnum.NOTALKS =>
          sqls"AND NOT topics.groupid = 8404 AND NOT topics.notop"
        case TrackerFilterEnum.MAIN =>
          sqls"AND NOT topics.groupid IN (${NonTech}) AND NOT topics.notop"
        case TrackerFilterEnum.TECH =>
          sqls"AND NOT topics.groupid IN (${NonTech}) AND NOT topics.notop AND section=2"
        case _ =>
          SQLSyntax.empty

    val partFilter = SQLSyntax.join(
      Seq(sectionFilter, sqls"AND lastmod > CURRENT_TIMESTAMP - '7 days'::interval"),
      sqls" ")

    load(
      partFilter = partFilter,
      topics = session.profile.topics,
      offset = offset,
      sortByTopic = false,
      commentInterval = sqls"AND comments.postdate > CURRENT_TIMESTAMP - '7 days'::interval",
      topicInterval = sqls"t.postdate > CURRENT_TIMESTAMP - '7 days'::interval",
      showIgnored = false,
      showDeleted = false,
      showUncommited = filter == TrackerFilterEnum.ALL || session.moderator || session.corrector
    )

  private def load(
      partFilter: SQLSyntax,
      topics: Int,
      offset: Int,
      sortByTopic: Boolean,
      commentInterval: SQLSyntax,
      topicInterval: SQLSyntax,
      showIgnored: Boolean,
      showDeleted: Boolean,
      showUncommited: Boolean)(using session: AnySession): collection.Seq[TopicsListItem] =
    val innerSortLimit: SQLSyntax =
      if sortByTopic then
        sqls"ORDER BY postdate DESC LIMIT $topics OFFSET $offset"
      else
        SQLSyntax.empty

    val outerSortLimit: SQLSyntax =
      if sortByTopic then
        sqls"ORDER BY topic_postdate DESC"
      else
        sqls"ORDER BY comment_postdate DESC LIMIT $topics OFFSET $offset"

    val userId =
      if session.authorized && !showIgnored then
        Some(session.userOpt.get.id)
      else
        None

    val partIgnored: SQLSyntax = userId.map(GroupListDao.queryPartIgnored).getOrElse(SQLSyntax.empty)
    val commentIgnored: SQLSyntax = userId.map(GroupListDao.queryPartCommentIgnored).getOrElse(SQLSyntax.empty)
    val tagIgnored: SQLSyntax = userId.map(GroupListDao.queryPartTagIgnored).getOrElse(SQLSyntax.empty)

    val noHidden: SQLSyntax =
      if session.authorized then
        SQLSyntax.empty
      else
        sqls"AND topics.open_warnings <= ${TopicMaxWarnings}"
    val partUncommited: SQLSyntax =
      if showUncommited then
        SQLSyntax.empty
      else
        GroupListDao.NoUncommited
    val partDeleted: SQLSyntax =
      if showDeleted then
        SQLSyntax.empty
      else
        sqls"AND NOT deleted"
    val whereTopicInterval: SQLSyntax =
      if topicInterval.isEmpty then
        SQLSyntax.empty
      else
        sqls"WHERE $topicInterval"

    springDB.run:
      sql"""WITH topics AS (
        SELECT topics.*, sections.moderate as smod, groups.title AS gtitle, urlname, section FROM topics
        JOIN groups ON topics.groupid=groups.id JOIN sections ON sections.id = groups.section
        WHERE NOT draft $partDeleted $partIgnored $tagIgnored $partUncommited $partFilter $noHidden $innerSortLimit
      ) SELECT * FROM (SELECT DISTINCT ON(id) * FROM (SELECT
        t.userid as author, t.id, t.stat1 AS stat1, gtitle, t.title AS title,
        comments.id as cid, comments.userid AS last_comment_by, t.resolved as resolved,
        section, urlname, comments.postdate as comment_postdate, smod, t.moderate,
        t.sticky, t.postdate as topic_postdate, t.deleted, t.postscore as topic_postscore
        FROM topics AS t JOIN comments ON (t.id=comments.topic)
        WHERE t.postscore IS DISTINCT FROM ${TopicPermissionService.POSTSCORE_HIDE_COMMENTS}
        AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id
        $commentIgnored ORDER BY postdate DESC LIMIT 1)
        $commentInterval
        UNION ALL
        SELECT
        t.userid as author, t.id, t.stat1 AS stat1, gtitle, t.title AS title,
        0, 0, t.resolved as resolved, section, urlname,
        postdate as comment_postdate, smod, t.moderate, t.sticky,
        t.postdate as topic_postdate, t.deleted, t.postscore as topic_postscore
        FROM topics AS t $whereTopicInterval) as tracker ORDER BY id, comment_postdate DESC) tracker $outerSortLimit"""
        .map(mapTopicsListItem)
        .list
        .apply()
        .toSeq

  private def mapTopicsListItem(rs: WrappedResultSet): TopicsListItem =
    TopicsListItem(
      topicAuthor = rs.int("author"),
      topicId = rs.int("id"),
      commentCount = rs.int("stat1"),
      groupTitle = rs.string("gtitle"),
      title = StringUtil.makeTitle(rs.string("title")),
      lastCommentId = rs.intOpt("cid").filter(_ != 0),
      lastCommentBy = rs.intOpt("last_comment_by").filter(_ != 0),
      resolved = rs.booleanOpt("resolved").getOrElse(false),
      section = rs.int("section"),
      groupUrlName = rs.string("urlname"),
      postdate = rs.timestamp("comment_postdate"),
      uncommited = rs.booleanOpt("smod").getOrElse(false) && !rs.booleanOpt("moderate").getOrElse(false),
      deleted = rs.booleanOpt("deleted").getOrElse(false),
      sticky = rs.booleanOpt("sticky").getOrElse(false),
      topicPostscore = rs.intOpt("topic_postscore").getOrElse(TopicPermissionService.POSTSCORE_UNRESTRICTED)
    )
