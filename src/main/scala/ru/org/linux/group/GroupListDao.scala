/*
 * Copyright 1998-2025 Linux.org.ru
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

import org.springframework.jdbc.core.namedparam.{MapSqlParameterSource, NamedParameterJdbcTemplate}
import org.springframework.stereotype.Repository
import ru.org.linux.auth.AnySession
import ru.org.linux.section.Section
import ru.org.linux.section.SectionController.NonTech
import ru.org.linux.topic.TopicPermissionService
import ru.org.linux.tracker.TrackerFilterEnum
import ru.org.linux.util.StringUtil
import ru.org.linux.warning.WarningService.TopicMaxWarnings

import java.sql.ResultSet
import javax.sql.DataSource
import scala.jdk.CollectionConverters.ListHasAsScala

object GroupListDao {
  private val QueryTrackerMain =
    "WITH topics AS (" + "SELECT topics.*, sections.moderate as smod, groups.title AS gtitle, urlname, section FROM topics " +
      "JOIN groups ON topics.groupid=groups.id JOIN sections ON sections.id = groups.section " + "WHERE not draft" + "%s" + // deleted
      "%s" + /* user!=null ? queryPartIgnored*/
      "%s" + // queryAuthorFilter
      "%s" + // queryPartTagIgnored
      "%s" + // noUncommited
      "%s" + // partFilter
      "%s" + // noHidden
      "%s" + // innerSortLimit
    ") SELECT * FROM (SELECT DISTINCT ON(id) * FROM (SELECT " +
      "t.userid as author, " +
      "t.id, " +
      "t.stat1 AS stat1, " +
      "gtitle, " +
      "t.title AS title, " +
      "comments.id as cid, " +
      "comments.userid AS last_comment_by, " +
      "t.resolved as resolved," +
      "section," +
      "urlname," +
      "comments.postdate as comment_postdate, " +
      "smod, " +
      "t.moderate, " +
      "t.sticky, " +
      "t.postdate as topic_postdate, " +
      "t.deleted, " +
      "t.postscore as topic_postscore " +
      "FROM topics AS t JOIN comments ON (t.id=comments.topic) " +
      "WHERE t.postscore IS DISTINCT FROM " + TopicPermissionService.POSTSCORE_HIDE_COMMENTS + " " +
      "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id " +
      "%s" + /* user!=null ? queryCommentIgnored */
      "%s" + // queryAuthorFilter
      "ORDER BY postdate DESC LIMIT 1) " +
    "%s" + // commentInterval
    "UNION ALL " +
      "SELECT " +
      "t.userid as author, " +
      "t.id, " +
      "t.stat1 AS stat1, " +
      "gtitle, " +
      "t.title AS title, " +
      "0, " + /*cid*/
      "0, " + /*last_comment_by*/
      "t.resolved as resolved," +
      "section," +
      "urlname," +
      "postdate as comment_postdate, " +
      "smod, " +
      "t.moderate, " +
      "t.sticky, " +
      "t.postdate as topic_postdate, " +
      "t.deleted, " +
      "t.postscore as topic_postscore " +
      "FROM topics AS t " +
      "%s " + // WHERE topicInterval
    ") as tracker ORDER BY id, comment_postdate desc) tracker " +
      "%s" // outerSortLimit

  private val QueryPartCommentIgnored = " AND not exists (select ignored from ignore_list where userid=:userid intersect select get_branch_authors(comments.id)) "
  private val QueryPartIgnored = " AND userid NOT IN (select ignored from ignore_list where userid=:userid) "

  private val QueryPartTagIgnored = " AND topics.id NOT IN (select tags.msgid from tags, user_tags " +
    "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=:userid " +
    "except select tags.msgid from tags, user_tags where " +
    "tags.tagid=user_tags.tag_id and user_tags.is_favorite = true and user_id=:userid) "

  private val NoUncommited = " AND (topics.moderate or NOT sections.moderate) "
}

@Repository
class GroupListDao(ds: DataSource) {
  private val jdbcTemplate = new NamedParameterJdbcTemplate(ds)

  def getGroupTrackerTopics(groupid: Int, offset: Int, tagId: Option[Int])
                           (implicit session: AnySession): collection.Seq[TopicsListItem] = {
    val dateFilter = ">CURRENT_TIMESTAMP-'6 month'::interval "
    val partFilter = s" AND topics.groupid = $groupid "
    val tagFilter = tagId.map(t => s" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=$t) ").getOrElse("")

    load(
      partFilter = partFilter + tagFilter + " AND lastmod" + dateFilter,
      topics = session.profile.topics,
      offset = offset,
      orderColumn = "comment_postdate",
      commentInterval = "AND comments.postdate" + dateFilter,
      topicInterval = "t.postdate" + dateFilter,
      showIgnored = false,
      showDeleted = false)
  }

  def getGroupListTopics(groupid: Int, offset: Int, showIgnored: Boolean, showDeleted: Boolean,
                         yearMonth: Option[(Int, Int)], tagId: Option[Int])
                        (implicit session: AnySession): collection.Seq[TopicsListItem] = {
    val dateInterval: String = yearMonth.map { v =>
      val (year, month) = v

      s"postdate>='$year-$month-01'::timestamp AND (postdate<'$year-$month-01'::timestamp+'1 month'::interval)"
    }.getOrElse("postdate>CURRENT_TIMESTAMP-'6 month'::interval ")

    val partFilter = s" AND topics.groupid = $groupid AND NOT topics.sticky AND $dateInterval"
    val tagFilter = tagId.map(t => s" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=$t) ").getOrElse("")

    load(
      partFilter = partFilter + tagFilter,
      topics = session.profile.topics,
      offset = offset,
      orderColumn = "topic_postdate",
      commentInterval = "",
      topicInterval = "",
      showIgnored = showIgnored,
      showDeleted = showDeleted)
  }

  def getSectionListTopics(section: Section, offset: Int, tagId: Int)
                          (implicit session: AnySession): collection.Seq[TopicsListItem] = {
    val partFilter = s" AND section = ${section.getId}"
    val tagFilter = s" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=$tagId) "

    load(
      partFilter = partFilter + tagFilter,
      topics = session.profile.topics,
      offset = offset,
      orderColumn = "topic_postdate",
      commentInterval = "",
      topicInterval = "",
      showIgnored = false,
      showDeleted = false)
  }

  def getGroupStickyTopics(group: Group, tagId: Option[Int])
                          (implicit session: AnySession): collection.Seq[TopicsListItem] = {
    val partFilter = s" AND topics.groupid = ${group.id} AND topics.sticky "
    val tagFilter = tagId.map(t => s" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=$t) ").getOrElse("")

    load(
      partFilter = partFilter + tagFilter,
      topics = 100,
      offset = 0,
      orderColumn = "topic_postdate",
      commentInterval = "",
      topicInterval = "",
      showIgnored = true,
      showDeleted = false)
  }

  def getTrackerTopics(filter: TrackerFilterEnum, offset: Int)
                      (implicit session: AnySession): collection.Seq[TopicsListItem] = {
    val partFilter = filter match {
      case TrackerFilterEnum.NOTALKS =>
        " AND not topics.groupid = 8404 "
      case TrackerFilterEnum.MAIN =>
        " AND not topics.groupid in " + NonTech.mkString("(", ", ",")") + " "
      case TrackerFilterEnum.TECH =>
        " AND not topics.groupid in " + NonTech.mkString("(", ", ",")") + " AND section=2 "
      case _ => ""
    }

    val dateFilter = ">CURRENT_TIMESTAMP-'7 days'::interval "

    load(
      partFilter = partFilter + " AND lastmod" + dateFilter,
      topics = session.profile.topics,
      offset = offset,
      orderColumn = "comment_postdate",
      commentInterval = "AND comments.postdate" + dateFilter,
      topicInterval = "t.postdate" + dateFilter,
      showIgnored = false,
      showDeleted = false)
  }

  private def load(partFilter: String, topics: Int, offset: Int, orderColumn: String,
                   commentInterval: String, topicInterval: String, showIgnored: Boolean,
                   showDeleted: Boolean)(implicit session: AnySession): collection.Seq[TopicsListItem] = {
    // если сортируем по топику, то можно заранее отобрать нужные топики,
    // до получения даты последнего комментария
    val (innerSortLimit, outerSortLimit) = if (orderColumn == "topic_postdate") {
      (s"ORDER BY postdate DESC LIMIT $topics OFFSET $offset",
        "ORDER BY topic_postdate DESC")
    } else {
      ("", s"ORDER BY $orderColumn DESC LIMIT $topics OFFSET $offset")
    }

    val parameter = new MapSqlParameterSource

    var partIgnored: String = null
    var commentIgnored: String = null
    var tagIgnored: String = null

    if (session.authorized && !showIgnored) {
      commentIgnored = GroupListDao.QueryPartCommentIgnored
      partIgnored = GroupListDao.QueryPartIgnored
      tagIgnored = GroupListDao.QueryPartTagIgnored

      parameter.addValue("userid", session.userOpt.get.id)
    } else {
      partIgnored = ""
      commentIgnored = ""
      tagIgnored = ""
    }

    val noHidden = if (session.authorized) "" else s" AND topics.open_warnings <= $TopicMaxWarnings "

    val showUncommited = session.moderator || session.corrector

    val partUncommited = if (showUncommited) "" else GroupListDao.NoUncommited

    val partDeleted = if (showDeleted) "" else " AND NOT deleted "

    val query: String = String.format( // topics CTE
      GroupListDao.QueryTrackerMain, partDeleted, partIgnored, "", tagIgnored, partUncommited, partFilter, noHidden,
      innerSortLimit,
      // comments part
      commentIgnored, "", commentInterval, // topics part
      if (topicInterval.isEmpty) "" else "WHERE " + topicInterval, // order
      outerSortLimit)

    jdbcTemplate.query(query, parameter, (resultSet: ResultSet, rowNum: Int) => {
      val author = resultSet.getInt("author")
      val msgid = resultSet.getInt("id")
      val stat1 = resultSet.getInt("stat1")
      val groupTitle = resultSet.getString("gtitle")
      val title = StringUtil.makeTitle(resultSet.getString("title"))

      val cid = Some(resultSet.getInt("cid")).filter(_ != 0)

      val lastCommentBy = Some(resultSet.getInt("last_comment_by")).filter(_ != 0)

      val resolved = resultSet.getBoolean("resolved")
      val section = resultSet.getInt("section")
      val groupUrlName = resultSet.getString("urlname")
      val postdate = resultSet.getTimestamp("comment_postdate")
      val sticky = resultSet.getBoolean("sticky")
      val uncommited = resultSet.getBoolean("smod") && !resultSet.getBoolean("moderate")

      val topicPostscore = if (resultSet.getObject("topic_postscore") == null) {
        TopicPermissionService.POSTSCORE_UNRESTRICTED
      } else {
        resultSet.getInt("topic_postscore")
      }

      TopicsListItem(
        topicAuthor = author,
        topicId = msgid,
        commentCount = stat1,
        groupTitle = groupTitle,
        title = title,
        lastCommentId = cid,
        lastCommentBy = lastCommentBy,
        resolved = resolved,
        section = section,
        groupUrlName = groupUrlName,
        postdate = postdate,
        uncommited = uncommited,
        deleted = resultSet.getBoolean("deleted"),
        sticky = sticky,
        topicPostscore = topicPostscore)
    })
  }.asScala
}