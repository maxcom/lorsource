/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.tracker;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class TrackerDao {
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  @Autowired
  private UserDao userDao;

  @Autowired
  private TopicTagService topicTagService;

  private static final String queryTrackerZeroMain =
      "SELECT " +
          "t.userid as author, " +
          "t.id, lastmod,  " +
          "t.stat1 AS stat1, " +
          "g.id AS gid, " +
          "g.title AS gtitle, " +
          "t.title AS title, " +
          "0 as cid, " +
          "0 as last_comment_by, " +
          "t.resolved as resolved," +
          "section," +
          "urlname," +
          "postdate, " +
          "sections.moderate as smod, " +
          "t.moderate " +
          "FROM topics AS t, groups AS g, sections " +
          "WHERE sections.id=g.section AND not t.deleted AND not t.draft AND t.postdate > :interval " +
          "%s" + /* user!=null ? queryPartIgnored*/
          " AND t.stat1=0 AND g.id=t.groupid " +
          "ORDER BY lastmod DESC LIMIT :topics OFFSET :offset";

  private static final String queryTrackerMain =
      "SELECT " +
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
        "t.moderate " +
      "FROM topics AS t, groups AS g, comments, sections " +
      "WHERE g.section=sections.id AND not t.deleted AND not t.draft AND t.id=comments.topic AND t.groupid=g.id " +
        "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id ORDER BY postdate DESC LIMIT 1) " +
        "AND t.lastmod > :interval " +
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
          "t.moderate " +
      "FROM topics AS t, groups AS g, sections " +
      "WHERE sections.id=g.section AND not t.deleted AND not t.draft AND t.postdate > :interval " +
          "%s" + /* noUncommited */
          "%s" + /* user!=null ? queryPartIgnored*/
          "%s" + /* noTalks ? queryPartNoTalks tech ? queryPartTech mine ? queryPartMine*/
          " AND t.stat1=0 AND g.id=t.groupid " +
      "%s" + /* wikiPart */
     "ORDER BY lastmod DESC LIMIT :topics OFFSET :offset";

  private static final String queryPartWiki = "UNION ALL " +
      "SELECT " + // wiki
          "0 as author, " +
          "0 as id, change_date as lastmod, " +
          "characters_changed as stat1, " +
          "0 as gid, " +
          "'Wiki' as gtitle, " +
          "topic_name as title, " +
          "0 as cid, " +
          "wiki_user_id as last_comment_by, " +
          "'f' as resolved, " +
          "0 as section, " +
          "'' as urlname, " +
          "change_date as postdate, " +
          "'f' as smod, " +
          "'f' as moderate " +
      "FROM wiki_recent_change " +
      "WHERE change_date > :interval ";

  private static final String queryPartWikiMine =      "UNION ALL " +
      "SELECT " + // wiki
          "0 as author, " +
          "0 as id, change_date as lastmod, " +
          "characters_changed as stat1, " +
          "0 as gid, " +
          "'Wiki' as gtitle, " +
          "topic_name as title, " +
          "0 as cid, " +
          "wiki_user_id as last_comment_by, " +
          "'f' as resolved, " +
          "0 as section, " +
          "'' as urlname, " +
          "change_date as postdate, " +
          "'f' as smod, " +
          "'f' as moderate " +
      "FROM jam_recent_change " +
      "WHERE topic_id is not null AND change_date > :interval " +
      " AND wiki_user_id=:userid ";
  
  

  private static final String queryPartIgnored = " AND t.userid NOT IN (select ignored from ignore_list where userid=:userid) ";
  private static final String queryPartTagIgnored = " AND t.id NOT IN (select distinct tags.msgid from tags, user_tags "
    + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=:userid) ";
  private static final String queryPartNoTalks = " AND not t.groupid=8404 ";
  private static final String queryPartTech = " AND not t.groupid=8404 AND not t.groupid=4068 AND section=2 ";
  private static final String queryPartMine = " AND t.userid=:userid ";

  private static final String noUncommited = " AND (t.moderate or NOT sections.moderate) ";

  public List<TrackerItem> getTrackAll(TrackerFilterEnum filter, User currentUser, Date startDate,
                                       int topics, int offset, final int messagesInPage) {

    MapSqlParameterSource parameter = new MapSqlParameterSource();
    parameter.addValue("interval", startDate);
    parameter.addValue("topics", topics);
    parameter.addValue("offset", offset);

    String partIgnored;

    if(currentUser != null) {
      partIgnored = queryPartIgnored + queryPartTagIgnored;
      parameter.addValue("userid", currentUser.getId());
    } else {
      partIgnored = "";
    }

    String partFilter;
    String partWiki = queryPartWiki;
    switch (filter) {
      case ALL:
        partFilter = "";
        break;
      case NOTALKS:
        partFilter = queryPartNoTalks;
        break;
      case TECH:
        partFilter = queryPartTech;
        break;
      case MINE:
        if(currentUser != null) {
          partFilter = queryPartMine;
          partWiki = queryPartWikiMine;
        } else {
          partFilter = "";
        }
        break;
      default:
        partFilter = "";
    }

    boolean showUncommited = currentUser!=null && (currentUser.isModerator() || currentUser.isCorrector());

    String partUncommited = showUncommited ? "" : noUncommited;

    String query;

    if(filter != TrackerFilterEnum.ZERO) {
      query = String.format(queryTrackerMain, partUncommited, partIgnored, partFilter, partUncommited, partIgnored, partFilter, partWiki);
    } else {
      query = String.format(queryTrackerZeroMain, partIgnored);
    }

    SqlRowSet resultSet = jdbcTemplate.queryForRowSet(query, parameter);

    List<TrackerItem> res = new ArrayList<>(topics);
    
    while (resultSet.next()) {
      User author;
      int author_id = resultSet.getInt("author");
      if (author_id != 0) {
        author = userDao.getUserCached(author_id);
      } else {
        author = null;
      }
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
      boolean uncommited = resultSet.getBoolean("smod") && !resultSet.getBoolean("moderate");
      int pages = Topic.getPageCount(stat1, messagesInPage);

      ImmutableList<String> tags;

      if (msgid != 0) {
        tags = topicTagService.getTagsForTitle(msgid);
      } else {
        tags = ImmutableList.of();
      }

      res.add(new TrackerItem(author, msgid, lastmod, stat1,
              groupId, groupTitle, title, cid, lastCommentBy, resolved,
              section, groupUrlName, postdate, uncommited, pages, tags));
    }
    
    return res;
  }
}
