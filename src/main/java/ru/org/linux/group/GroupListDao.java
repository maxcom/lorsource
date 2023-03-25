/*
 * Copyright 1998-2023 Linux.org.ru
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupListDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public GroupListDao(UserDao userDao, TopicTagService topicTagService, DataSource ds) {
    this.userDao = userDao;
    this.topicTagService = topicTagService;
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  private final UserDao userDao;

  private final TopicTagService topicTagService;

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
        "comments.postdate as comment_postdate, " +
        "sections.moderate as smod, " +
        "t.moderate, " +
        "t.sticky, " +
        "t.postdate as topic_postdate, " +
        "t.deleted, " +
        "t.postscore as topic_postscore " +
      "FROM topics AS t, groups AS g, comments, sections " +
      "WHERE g.section=sections.id AND not t.draft AND t.id=comments.topic AND t.groupid=g.id AND t.postscore IS DISTINCT FROM " + TopicPermissionService.POSTSCORE_HIDE_COMMENTS + " " +
        "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id " +
              "%s" + /* user!=null ? queryCommentIgnored */
              "%s" + // queryAuthorFilter
              "ORDER BY postdate DESC LIMIT 1) " +
        "%s" + // commentInterval
        "%s" + // deleted
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
          "postdate as comment_postdate, " +
          "sections.moderate as smod, " +
          "t.moderate, " +
          "t.sticky, " +
          "t.postdate as topic_postdate, " +
          "t.deleted, " +
          "t.postscore as topic_postscore " +
      "FROM topics AS t, groups AS g, sections " +
      "WHERE sections.id=g.section AND not t.draft %s " + // topicInterval
          "%s" + /* noUncommited */
          "%s" + // deleted
          "%s" + /* user!=null ? queryPartIgnored*/
          "%s" + /* noTalks ? queryPartNoTalks tech ? queryPartTech mine ? queryPartMine*/
          "%s" + // queryAuthorFilter
          " AND g.id=t.groupid) as tracker ORDER BY id, comment_postdate desc) tracker " +
              "WHERE true %s" + // queryPartTagIgnored
     "ORDER BY %s DESC LIMIT :topics OFFSET :offset"; // orderColumn

  private static final String queryPartCommentIgnored = " AND not exists (select ignored from ignore_list where userid=:userid intersect select get_branch_authors(comments.id)) ";
  private static final String queryPartIgnored = " AND t.userid NOT IN (select ignored from ignore_list where userid=:userid) ";
  private static final String queryPartTagIgnored = " AND tracker.id NOT IN (select tags.msgid from tags, user_tags "
    + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=:userid " +
          "except select tags.msgid from tags, user_tags where " +
          "tags.tagid=user_tags.tag_id and user_tags.is_favorite = true and user_id=:userid) ";
  private static final String queryPartNoTalks = " AND not t.groupid = 8404 ";
  private static final String queryPartTech = " AND not t.groupid in (8404, 4068, 9326, 19405) AND section=2 ";
  private static final String queryPartMain = " AND not t.groupid in (8404, 4068, 9326, 19405) ";

  private static final String noUncommited = " AND (t.moderate or NOT sections.moderate) ";

  public List<TopicsListItem> getGroupTrackerTopics(int groupid, User currentUser, int topics, int offset,
                                                    int messagesInPage, Optional<Integer> tagId) {

    String dateFilter = ">CURRENT_TIMESTAMP-'6 month'::interval ";

    String partFilter = " AND t.groupid = " + groupid + " ";

    String tagFilter = tagId.map(t -> " AND t.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter, "", currentUser,
            topics, offset, messagesInPage, "comment_postdate",
            "AND comments.postdate"+dateFilter+" AND t.lastmod"+dateFilter,
            "AND t.postdate"+dateFilter, false, false);
  }

  public List<TopicsListItem> getGroupListTopics(int groupid, User currentUser, int topics, int offset,
                                                 int messagesInPage, boolean showIgnored, boolean showDeleted,
                                                 Optional<Integer> year, Optional<Integer> month, Optional<Integer> tagId) {
    String commentInterval;

    if (year.isPresent()) {
      commentInterval=" AND t.postdate>='" + year.get() + '-' + month.get() + "-01'::timestamp AND " +
              "(t.postdate<'" + year.get() + '-' + month.get() + "-01'::timestamp+'1 month'::interval)";
    } else  {
      commentInterval = " AND t.postdate>CURRENT_TIMESTAMP-'6 month'::interval ";
    }

    String partFilter = " AND t.groupid = " + groupid + " AND NOT t.sticky ";

    String tagFilter = tagId.map(t -> " AND t.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter, "", currentUser,
            topics, offset, messagesInPage,
            "topic_postdate", commentInterval, commentInterval, showIgnored, showDeleted);
  }

  public List<TopicsListItem> getGroupStickyTopics(Group group, int messagesInPage, Optional<Integer> tagId) {
    String partFilter = " AND t.groupid = " + group.getId() + " AND t.sticky ";

    String tagFilter = tagId.map(t -> " AND t.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter, "", null,
            100, 0, messagesInPage,
            "topic_postdate", "", "", true, false);
  }

  public List<TopicsListItem> getTrackerTopics(TrackerFilterEnum filter, User currentUser,
                                               int topics, int offset, int messagesInPage) {
    String partFilter = switch (filter) {
      case NOTALKS -> queryPartNoTalks;
      case MAIN -> queryPartMain;
      case TECH -> queryPartTech;
      default -> "";
    };

    String userFilter = switch (filter) {
      case SCORE50 -> " AND userid IN (SELECT id FROM users WHERE score<50) ";
      case SCORE100 -> " AND userid IN (SELECT id FROM users WHERE score<100) ";
      default -> "";
    };

    String dateFilter = ">CURRENT_TIMESTAMP-'4 days'::interval ";

    return load(partFilter, userFilter, currentUser, topics, offset,
            messagesInPage, "comment_postdate", "AND comments.postdate"+dateFilter +" AND t.lastmod"+dateFilter,
            "AND t.postdate"+dateFilter, false, false);
  }

  private List<TopicsListItem> load(String partFilter, String authorFilter, User currentUser,
                                    int topics, int offset, final int messagesInPage, String orderColumn,
                                    String commentInterval, String topicInterval, boolean showIgnored,
                                    boolean showDeleted) {

    MapSqlParameterSource parameter = new MapSqlParameterSource();
    parameter.addValue("topics", topics);
    parameter.addValue("offset", offset);

    String partIgnored;
    String commentIgnored;
    String tagIgnored;

    if (currentUser != null && !showIgnored) {
      commentIgnored = queryPartCommentIgnored;
      partIgnored = queryPartIgnored;
      tagIgnored = queryPartTagIgnored;
      parameter.addValue("userid", currentUser.getId());
    } else {
      partIgnored = "";
      commentIgnored = "";
      tagIgnored = "";
    }

    boolean showUncommited = currentUser!=null && (currentUser.isModerator() || currentUser.canCorrect());

    String partUncommited = showUncommited ? "" : noUncommited;
    String partDeleted = showDeleted?"":" AND NOT t.deleted ";

    String query;

    query = String.format(queryTrackerMain, commentIgnored, authorFilter, commentInterval, partDeleted, partUncommited, partIgnored, partFilter,
            topicInterval, partDeleted, partUncommited, partIgnored, partFilter, authorFilter, tagIgnored, orderColumn);

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
      Timestamp postdate = resultSet.getTimestamp("comment_postdate");
      boolean sticky = resultSet.getBoolean("sticky");
      boolean uncommited = resultSet.getBoolean("smod") && !resultSet.getBoolean("moderate");
      int pages = Topic.pageCount(stat1, messagesInPage);

      ImmutableList<String> tags;

      tags = topicTagService.getTagsForTitle(msgid);

      int topicPostscore = (resultSet.getObject("topic_postscore") == null)
              ? TopicPermissionService.POSTSCORE_UNRESTRICTED
              : resultSet.getInt("topic_postscore");

      res.add(new TopicsListItem(author, msgid, lastmod, stat1,
              groupId, groupTitle, title, cid, lastCommentBy, resolved,
              section, groupUrlName, postdate, uncommited, pages, tags, resultSet.getBoolean("deleted"),
              sticky, topicPostscore));
    }
    
    return res;
  }
}
