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
import ru.org.linux.section.Section;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.user.User;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserService;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupListDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public GroupListDao(UserService userService, TopicTagService topicTagService, DataSource ds) {
    this.userService = userService;
    this.topicTagService = topicTagService;
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  private final UserService userService;

  private final TopicTagService topicTagService;

  private static final String queryTrackerMain =
      "WITH topics AS (" +
        "SELECT topics.*, groups.id as gid, sections.moderate as smod, groups.title AS gtitle, urlname, section FROM topics " +
        "JOIN groups ON topics.groupid=groups.id JOIN sections ON sections.id = groups.section " +
        "WHERE not draft" +
        "%s" + // deleted
        "%s" + /* user!=null ? queryPartIgnored*/
        "%s" + // queryAuthorFilter
        "%s" + // queryPartTagIgnored
        "%s" + // noUncommited
        "%s" + // partFilter
        "%s" + // innerSortLimit
      ") SELECT * FROM (SELECT DISTINCT ON(id) * FROM (SELECT " +
        "t.userid as author, " +
        "t.id, lastmod, " +
        "t.stat1 AS stat1, " +
        "gid, " +
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
          "t.id, lastmod,  " +
          "t.stat1 AS stat1, " +
          "gid, " +
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
     "%s"; // outerSortLimit

  private static final String queryPartCommentIgnored =
          " AND not exists (select ignored from ignore_list where userid=:userid intersect select get_branch_authors(comments.id)) ";
  private static final String queryPartIgnored =
          " AND userid NOT IN (select ignored from ignore_list where userid=:userid) ";
  private static final String queryPartTagIgnored = " AND topics.id NOT IN (select tags.msgid from tags, user_tags "
    + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=:userid " +
          "except select tags.msgid from tags, user_tags where " +
          "tags.tagid=user_tags.tag_id and user_tags.is_favorite = true and user_id=:userid) ";
  private static final String queryPartNoTalks = " AND not topics.groupid = 8404 ";
  private static final String queryPartTech = " AND not topics.groupid in (8404, 4068, 9326, 19405) AND section=2 ";
  private static final String queryPartMain = " AND not topics.groupid in (8404, 4068, 9326, 19405) ";

  private static final String noUncommited = " AND (topics.moderate or NOT sections.moderate) ";

  public List<TopicsListItem> getGroupTrackerTopics(int groupid, Optional<User> currentUser, int topics, int offset,
                                                    int messagesInPage, Optional<Integer> tagId) {

    String dateFilter = ">CURRENT_TIMESTAMP-'6 month'::interval ";

    String partFilter = " AND topics.groupid = " + groupid + " ";

    String tagFilter = tagId.map(t -> " AND topics.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter + " AND lastmod"+dateFilter, "", currentUser,
            topics, offset, messagesInPage, "comment_postdate",
            "AND comments.postdate"+dateFilter,
            "t.postdate"+dateFilter, false, false);
  }

  public List<TopicsListItem> getGroupListTopics(int groupid, Optional<User> currentUser, int topics, int offset,
                                                 int messagesInPage, boolean showIgnored, boolean showDeleted,
                                                 Optional<Integer> year, Optional<Integer> month, Optional<Integer> tagId) {
    String dateInterval;

    if (year.isPresent()) {
      dateInterval="postdate>='" + year.get() + '-' + month.get() + "-01'::timestamp AND " +
              "(postdate<'" + year.get() + '-' + month.get() + "-01'::timestamp+'1 month'::interval)";
    } else  {
      dateInterval = "postdate>CURRENT_TIMESTAMP-'6 month'::interval ";
    }

    String partFilter = " AND topics.groupid = " + groupid + " AND NOT topics.sticky AND " + dateInterval;

    String tagFilter = tagId.map(t -> " AND topics.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter, "", currentUser,
            topics, offset, messagesInPage,
            "topic_postdate", "", "", showIgnored, showDeleted);
  }

  public List<TopicsListItem> getSectionListTopics(Section section, Optional<User> currentUser, int topics, int offset,
                                                   int messagesInPage, Integer tagId) {
    String partFilter = " AND section = " + section.getId();

    String tagFilter = " AND topics.id IN (SELECT msgid FROM tags WHERE tagid="+tagId+") ";

    return load(partFilter + tagFilter, "", currentUser,
            topics, offset, messagesInPage,
            "topic_postdate", "", "", false, false);
  }

  public List<TopicsListItem> getGroupStickyTopics(Group group, int messagesInPage, Optional<Integer> tagId) {
    String partFilter = " AND topics.groupid = " + group.getId() + " AND topics.sticky ";

    String tagFilter = tagId.map(t -> " AND topics.id IN (SELECT msgid FROM tags WHERE tagid="+t+") ").orElse("");

    return load(partFilter + tagFilter, "", Optional.empty(),
            100, 0, messagesInPage,
            "topic_postdate", "", "", true, false);
  }

  public List<TopicsListItem> getTrackerTopics(TrackerFilterEnum filter, Optional<User> currentUser,
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

    String dateFilter = ">CURRENT_TIMESTAMP-'7 days'::interval ";

    return load(partFilter +" AND lastmod"+dateFilter, userFilter, currentUser, topics, offset, messagesInPage, "comment_postdate",
            "AND comments.postdate"+dateFilter,
            "t.postdate"+dateFilter, false, false);
  }

  private List<TopicsListItem> load(String partFilter, String authorFilter, Optional<User> currentUserOpt,
                                    int topics, int offset, final int messagesInPage, String orderColumn,
                                    String commentInterval, String topicInterval, boolean showIgnored,
                                    boolean showDeleted) {
    String innerSortLimit;
    String outerSortLimit;

    // если сортируем по топику, то можно заранее отобрать нужные топики,
    // до получения даты последнего комментария
    if (orderColumn.equals("topic_postdate")) {
      innerSortLimit = "ORDER BY postdate DESC LIMIT " + topics + " OFFSET " + offset;
      outerSortLimit = "ORDER BY topic_postdate DESC";
    } else {
      innerSortLimit = "";
      outerSortLimit = "ORDER BY " + orderColumn + " DESC LIMIT " + topics + " OFFSET " + offset;
    }

    User currentUser = currentUserOpt.orElse(null);

    MapSqlParameterSource parameter = new MapSqlParameterSource();

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
    String partDeleted = showDeleted?"":" AND NOT deleted ";

    String query;

    query = String.format(
            // topics CTE
            queryTrackerMain, partDeleted, partIgnored, authorFilter, tagIgnored, partUncommited, partFilter, innerSortLimit,
            // comments part
            commentIgnored, authorFilter, commentInterval,
            // topics part
            topicInterval.isEmpty()?"":("WHERE " + topicInterval),
            // order
            outerSortLimit);

    SqlRowSet resultSet = jdbcTemplate.queryForRowSet(query, parameter);

    List<TopicsListItem> res = new ArrayList<>(topics);
    
    while (resultSet.next()) {
      User author = userService.getUserCached(resultSet.getInt("author"));
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
          lastCommentBy = userService.getUserCached(id);
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
