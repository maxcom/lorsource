/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.dao.UserDao;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.Message;
import ru.org.linux.site.UserNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 *
 */
@Repository
public class TrackerDao {
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  @Autowired
  UserDao userDao;

  public enum TrackerFilter {
    ALL("all", "все сообщения"),
    NOTALKS("notalks", "без talks"),
    TECH("tech", "тех. разделы форума"),
    MINE("mine", "мои темы");
    private final String value;
    private final String label;
    TrackerFilter(String value, String label) {
      this.value = value;
      this.label = label;
    }
    public String getValue() {
      return value;
    }
    public String getLabel() {
      return label;
    }
  }

  private static final String queryTrackerMain =
      "SELECT " +
        "t.userid as author, " +
        "t.id, lastmod, " +
        "t.stat1 AS stat1, " +
        "t.stat3 AS stat3, " +
        "t.stat4 AS stat4, " +
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
      "WHERE g.section=sections.id AND not t.deleted AND t.id=comments.topic AND t.groupid=g.id " +
        "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id ORDER BY postdate DESC LIMIT 1) " +
        "AND t.lastmod > :interval " +
        "%s" + /* user!=null ? queryPartIgnored*/
        "%s" + /* noTalks ? queryPartNoTalks tech ? queryPartTech mine ? queryPartMine*/
     "UNION ALL " +
      "SELECT " +
          "t.userid as author, " +
          "t.id, lastmod,  " +
          "t.stat1 AS stat1, " +
          "t.stat3 AS stat3, " +
          "t.stat4 AS stat4, " +
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
      "WHERE sections.id=g.section AND not t.deleted AND t.postdate > :interval " +
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
          "0 as stat3, " +
          "0 as stat4, " +
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
          "0 as stat3, " +
          "0 as stat4, " +
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
  private static final String queryPartNoTalks = " AND not t.groupid=8404 ";
  private static final String queryPartTech = " AND not t.groupid=8404 AND not t.groupid=4068 AND section=2 ";
  private static final String queryPartMine = " AND t.userid=:userid ";

  public List<TrackerItem> getTrackAll(TrackerFilter filter, UserDto currentUser, Timestamp interval,
                                       int topics, int offset, final int messagesInPage) {

    MapSqlParameterSource parameter = new MapSqlParameterSource();
    parameter.addValue("interval", interval);
    parameter.addValue("topics", topics);
    parameter.addValue("offset", offset);

    String partIgnored;
    String partFilter;
    String partWiki=queryPartWiki;

    if(currentUser != null) {
      partIgnored = queryPartIgnored;
      parameter.addValue("userid", currentUser.getId());
    } else {
      partIgnored = "";
    }

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

    String query = String.format(queryTrackerMain, partIgnored, partFilter, partIgnored, partFilter, partWiki);

    return jdbcTemplate.query(query, parameter, new RowMapper<TrackerItem>() {
      @Override
      public TrackerItem mapRow(ResultSet resultSet, int i) throws SQLException {
        UserDto author, lastCommentBy;
        int msgid, cid, pages;
        Timestamp lastmod, postdate;
        int stat1, stat3, stat4;
        int groupId, section;
        String groupTitle, groupUrlName;
        String title;
        boolean resolved, uncommited;
        try {
          int author_id = resultSet.getInt("author");
          if(author_id != 0) {
            author = userDao.getUserCached(author_id);
          } else {
            author = null;
          }
        } catch (UserNotFoundException e) {
          throw new RuntimeException(e);
        }
        msgid = resultSet.getInt("id");
        lastmod = resultSet.getTimestamp("lastmod");
        stat1 = resultSet.getInt("stat1");
        stat3 = resultSet.getInt("stat3");
        stat4 = resultSet.getInt("stat4");
        groupId = resultSet.getInt("gid");
        groupTitle = resultSet.getString("gtitle");
        title = resultSet.getString("title");
        cid = resultSet.getInt("cid");
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
        resolved = resultSet.getBoolean("resolved");
        section = resultSet.getInt("section");
        groupUrlName = resultSet.getString("urlname");
        postdate = resultSet.getTimestamp("postdate");
        uncommited = resultSet.getBoolean("smod") && !resultSet.getBoolean("moderate");
        pages = Message.getPageCount(stat1, messagesInPage);
        return new TrackerItem(author, msgid, lastmod, stat1, stat3, stat4,
            groupId, groupTitle, title, cid, lastCommentBy, resolved,
            section, groupUrlName, postdate, uncommited, pages);
      }
    });
  }
}
