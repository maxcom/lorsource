/*
 * Copyright 1998-2019 Linux.org.ru
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

package ru.org.linux.group;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.org.linux.group.Group;
import ru.org.linux.group.TopicsListItem;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class GroupListDao {
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  @Autowired
  private UserDao userDao;

  @Autowired
  private TopicTagService topicTagService;

  private static final String queryTrackerMain =
      "SELECT * FROM (SELECT DISTINCT ON(id) * FROM (SELECT " +
        "t.userid as author, " +
        "t.id, lastmod, " +
        "t.stat1 AS stat1, " +
        "g.id AS gid, " +
        "g.title AS gtitle, " +
        "t.title AS title, " +
        "comments.id as cid, " +
        "comments.userid AS last_comment_by, " +
        "t.resolved as resolved," +
        "section," +
        "urlname," +
        "comments.postdate, " +
        "sections.moderate as smod, " +
        "t.moderate, " +
        "t.sticky " +
      "FROM topics AS t, groups AS g, comments, sections " +
      "WHERE g.section=sections.id AND not t.deleted AND not t.draft AND t.id=comments.topic AND t.groupid=g.id " +
        "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id " +
              "%s" + /* user!=null ? queryCommentIgnored*/
              "ORDER BY postdate DESC LIMIT 1) " +
        "AND t.lastmod > :interval AND comments.postdate > :interval " +
        "%s" + /* noUncommited */
        "%s" + /* user!=null ? queryPartIgnored*/
        "%s" + /* noTalks ? queryPartNoTalks tech ? queryPartTech mine ? queryPartMine*/
     "UNION ALL " +
      "SELECT " +
          "t.userid as author, " +
          "t.id, lastmod,  " +
          "t.stat1 AS stat1, " +
          "g.id AS gid, " +
          "g.title AS gtitle, " +
          "t.title AS title, " +
          "0, " + /*cid*/
          "0, " + /*last_comment_by*/
          "t.resolved as resolved," +
          "section," +
          "urlname," +
          "postdate, " +
          "sections.moderate as smod, " +
          "t.moderate, " +
          "t.sticky " +
      "FROM topics AS t, groups AS g, sections " +
      "WHERE sections.id=g.section AND not t.deleted AND not t.draft AND t.postdate > :interval " +
          "%s" + /* noUncommited */
          "%s" + /* user!=null ? queryPartIgnored*/
          "%s" + /* noTalks ? queryPartNoTalks tech ? queryPartTech mine ? queryPartMine*/
          " AND g.id=t.groupid) as tracker ORDER BY id, postdate desc) tracker " +
              "WHERE true %s" + // queryPartTagIgnored
     "ORDER BY postdate DESC LIMIT :topics OFFSET :offset";

  private static final String queryPartCommentIgnored = " AND not exists (select ignored from ignore_list where userid=:userid intersect select get_branch_authors(comments.id)) ";
  private static final String queryPartIgnored = " AND t.userid NOT IN (select ignored from ignore_list where userid=:userid) ";
  private static final String queryPartTagIgnored = " AND tracker.id NOT IN (select tags.msgid from tags, user_tags "
    + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=:userid " +
          "except select tags.msgid from tags, user_tags where " +
          "tags.tagid=user_tags.tag_id and user_tags.is_favorite = true and user_id=:userid) ";
  private static final String queryPartNoTalks = " AND not t.groupid in (8404, 19390) ";
  private static final String queryPartTech = " AND not t.groupid in (8404, 4068, 19392, 19390, 9326, 19405) AND section=2 ";
  private static final String queryPartMain = " AND not t.groupid in (8404, 4068, 19392, 19390, 19405) ";

  private static final String noUncommited = " AND (t.moderate or NOT sections.moderate) ";

  public List<TopicsListItem> getGroupTrackerTopics(int groupid, User currentUser, Date startDate,
                                                    int topics, int offset, final int messagesInPage) {

    return load(" AND t.groupid = " + groupid + " ", currentUser, startDate, topics, offset, messagesInPage);
  }

  public List<TopicsListItem> getTrackerTopics(TrackerFilterEnum filter, User currentUser, Date startDate,
                                               int topics, int offset, final int messagesInPage) {
    String partFilter;
    switch (filter) {
      case ALL:
        partFilter = "";
        break;
      case NOTALKS:
        partFilter = queryPartNoTalks;
        break;
      case MAIN:
        partFilter = queryPartMain;
        break;
      case TECH:
        partFilter = queryPartTech;
        break;
      default:
        partFilter = "";
    }

    return load(partFilter, currentUser, startDate, topics, offset, messagesInPage);
  }

  private List<TopicsListItem> load(String partFilter, User currentUser, Date startDate,
                                          int topics, int offset, final int messagesInPage) {

    MapSqlParameterSource parameter = new MapSqlParameterSource();
    parameter.addValue("interval", startDate);
    parameter.addValue("topics", topics);
    parameter.addValue("offset", offset);

    String partIgnored;
    String commentIgnored;
    String tagIgnored;

    if (currentUser != null) {
      commentIgnored = queryPartCommentIgnored;
      partIgnored = queryPartIgnored;
      tagIgnored = queryPartTagIgnored;
      parameter.addValue("userid", currentUser.getId());
    } else {
      partIgnored = "";
      commentIgnored = "";
      tagIgnored = "";
    }

    boolean showUncommited = currentUser!=null && (currentUser.isModerator() || currentUser.isCorrector());

    String partUncommited = showUncommited ? "" : noUncommited;

    String query;

    query = String.format(queryTrackerMain, commentIgnored, partUncommited, partIgnored, partFilter,
            partUncommited, partIgnored, partFilter, tagIgnored);

    SqlRowSet resultSet = jdbcTemplate.queryForRowSet(query, parameter);

    List<TopicsListItem> res = new ArrayList<>(topics);
    
    while (resultSet.next()) {
      User author = userDao.getUserCached(resultSet.getInt("author"));
      int msgid = resultSet.getInt("id");
      Timestamp lastmod = resultSet.getTimestamp("lastmod");
      int stat1 = resultSet.getInt("stat1");
      int groupId = resultSet.getInt("gid");
      String groupTitle = resultSet.getString("gtitle");
      String title = StringUtil.makeTitle(resultSet.getString("title"));
      int cid = resultSet.getInt("cid");
      User lastCommentBy;
      try {
        int id = resultSet.getInt("last_comment_by");

        if (id != 0) {
          lastCommentBy = userDao.getUserCached(id);
        } else {
          lastCommentBy = null;
        }
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }
      boolean resolved = resultSet.getBoolean("resolved");
      int section = resultSet.getInt("section");
      String groupUrlName = resultSet.getString("urlname");
      Timestamp postdate = resultSet.getTimestamp("postdate");
      boolean sticky = resultSet.getBoolean("sticky");
      boolean uncommited = resultSet.getBoolean("smod") && !resultSet.getBoolean("moderate");
      int pages = Topic.getPageCount(stat1, messagesInPage);

      ImmutableList<String> tags;

      tags = topicTagService.getTagsForTitle(msgid);

      res.add(new TopicsListItem(author, msgid, lastmod, stat1,
              groupId, groupTitle, title, cid, lastCommentBy, resolved,
              section, groupUrlName, postdate, uncommited, pages, tags, false, sticky));
    }
    
    return res;
  }

  public List<TopicsListItem> getGroupTopics(
          Group group,
          int messagesInPage,
          Integer year,
          Integer month,
          int topics,
          int offset,
          boolean showDeleted,
          boolean showIgnored,
          @Nullable User currentUser
  ) {
    String delq = showDeleted ? "" : " AND NOT deleted ";

    String ignq = "";
    String commentIgnq = "";

    if (!showIgnored && currentUser != null) {
      int currentUserId = currentUser.getId();
      ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=" + currentUserId + ')';
      ignq += " AND topics.id NOT IN (select distinct tags.msgid from tags, user_tags "
              + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=" + currentUserId + ") ";
      commentIgnq = " AND comments.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=" + currentUserId + ')';
    }

    String q = "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.sticky, topics.resolved, " +
            "(SELECT max(postdate) FROM comments WHERE topic=topics.id AND NOT comments.deleted "+commentIgnq+") as commentdate " +
            "FROM topics WHERE NOT draft AND NOT sticky AND topics.groupid=" + group.getId() + delq;

    if (year!=null) {
      q+=" AND postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)";
    }


    if (year==null) {
      if (offset==0) {
        q += " AND postdate>CURRENT_TIMESTAMP-'3 month'::interval ";
      }
    }

    return jdbcTemplate.getJdbcOperations().query(
            q + ignq + " ORDER BY postdate DESC LIMIT " + topics + " OFFSET " + offset,
            groupListMapper(group, messagesInPage)
    );
  }

  public List<TopicsListItem> getGroupStickyTopics(
          Group group,
          int messagesInPage
  ) {
    String q =
            "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.sticky, topics.resolved, " +
                    "(SELECT max(postdate) FROM comments WHERE topic=topics.id AND NOT comments.deleted) as commentdate " +
                    "FROM topics WHERE sticky AND NOT deleted AND topics.groupid=? ORDER BY postdate DESC";

    return jdbcTemplate.getJdbcOperations().query(q, groupListMapper(group, messagesInPage), group.getId());
  }

  private RowMapper<TopicsListItem> groupListMapper(Group group, int messagesInPage) {
    return (rs, rowNum) -> {
      User author;

      author = userDao.getUserCached(rs.getInt("userid"));

      ImmutableList<String> tags = topicTagService.getTagsForTitle(rs.getInt("msgid"));

      Timestamp lastmod = rs.getTimestamp("lastmod");
      Timestamp postdate = rs.getTimestamp("commentdate");

      if (postdate==null) {
        postdate = lastmod;
      }

      return new TopicsListItem(
              author,
              rs.getInt("msgid"),
              lastmod,
              rs.getInt("stat1"),
              group.getId(),
              group.getTitle(),
              StringUtil.makeTitle(rs.getString("subj")),
              0,
              null,
              rs.getBoolean("resolved"),
              group.getSectionId(),
              group.getUrlName(),
              postdate,
              false,
              Topic.getPageCount(rs.getInt("stat1"), messagesInPage),
              tags,
              rs.getBoolean("deleted"),
              rs.getBoolean("sticky"));
    };
  }
}
