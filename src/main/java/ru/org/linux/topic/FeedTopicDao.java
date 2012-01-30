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

package ru.org.linux.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@Repository
public class FeedTopicDao {
  public static enum CommitMode {
    COMMITED_ONLY(" AND sections.moderate AND commitdate is not null "),
    UNCOMMITED_ONLY(" AND (NOT topics.moderate) AND sections.moderate "),
    POSTMODERATED_ONLY(" AND NOT sections.moderate"),
    COMMITED_AND_POSTMODERATED(" AND (topics.moderate OR NOT sections.moderate) "),
    ALL(" ");

    final String queryPiece;

    CommitMode(String queryPiece) {
      this.queryPiece = queryPiece;
    }

    public String getQueryPiece() {
      return queryPiece;
    }
  }

  private static final Log logger = LogFactory.getLog(FeedTopicDao.class);

  private static final RowMapper<FeedTopicDto.DeletedTopic> rowMapperForDeletedTopics = getRowMapperForDeletedTopics();

  private JdbcTemplate jdbcTemplate;


  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Получение список топиков.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return список топиков
   */
  public List<Topic> getTopics(FeedTopicDto feedTopicDto) {
    logger.debug("FeedTopicDao.getTopics(); feedTopicDto = " + feedTopicDto.toString());
    String where = makeConditions(feedTopicDto);
    String sort = makeSortOrder(feedTopicDto);
    String limit = makeLimitAndOffset(feedTopicDto);

    StringBuilder query = new StringBuilder();

    query
      .append("SELECT ")
      .append("postdate, topics.id as msgid, topics.userid, topics.title, ")
      .append("topics.groupid as guid, topics.url, topics.linktext, ua_id, ")
      .append("urlname, havelink, section, topics.sticky, topics.postip, ")
      .append("postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, ")
      .append("commitdate, topics.stat1, postscore, topics.moderate, notop, ")
      .append("topics.resolved, restrict_comments, minor ")
      .append("FROM topics ")
      .append("INNER JOIN groups ON (groups.id=topics.groupid) ")
      .append("INNER JOIN sections ON (sections.id=groups.section) ");
    if (feedTopicDto.isUserFavs()) {
      query.append("INNER JOIN memories ON (memories.topic = topics.id) ");
    }
    query
      .append("WHERE ")
      .append(where)
      .append(sort)
      .append(limit);

    logger.trace("SQL query: " + query.toString());

    return jdbcTemplate.query(
      query.toString(),
      new RowMapper<Topic>() {
        @Override
        public Topic mapRow(ResultSet resultSet, int i) throws SQLException {
          return new Topic(resultSet);
        }
      }
    );
  }

  /**
   * @return
   */
  public List<FeedTopicDto.DeletedTopic> getDeletedTopics() {
    StringBuilder query = new StringBuilder();
    query
      .append("SELECT ")
      .append("topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, ")
      .append("groups.id as guid, sections.name as ptitle, reason ")
      .append("FROM topics,groups,users,sections,del_info ")
      .append("WHERE sections.id=groups.section AND topics.userid=users.id ")
      .append("AND topics.groupid=groups.id AND sections.moderate AND deleted ")
      .append("AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ")
      .append("AND delDate is not null ORDER BY del_info.delDate DESC LIMIT 20");

    return jdbcTemplate.query(
      query.toString(),
      rowMapperForDeletedTopics
    );
  }

  public List<FeedTopicDto.DeletedTopic> getDeletedTopics(Integer sectionId) {
    StringBuilder query = new StringBuilder();

    query
      .append("SELECT ")
      .append("topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, ")
      .append("groups.id as guid, sections.name as ptitle, reason ")
      .append("FROM topics,groups,users,sections,del_info ")
      .append("WHERE sections.id=groups.section AND topics.userid=users.id ")
      .append("AND topics.groupid=groups.id AND sections.moderate AND deleted ")
      .append("AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ")
      .append("AND delDate is not null AND section=? ORDER BY del_info.delDate DESC LIMIT 20");
    return jdbcTemplate.query(
      query.toString(),
      rowMapperForDeletedTopics,
      sectionId
    );
  }

  private static RowMapper<FeedTopicDto.DeletedTopic> getRowMapperForDeletedTopics() {
    return new RowMapper<FeedTopicDto.DeletedTopic>() {
      @Override
      public FeedTopicDto.DeletedTopic mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FeedTopicDto.DeletedTopic(rs);
      }
    };
  }

  /**
   * Создание условий выборки SQL-запроса.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return строка, содержащая условия выборки SQL-запроса
   */
  private String makeConditions(FeedTopicDto feedTopicDto) {
    StringBuilder where = new StringBuilder(
      "NOT deleted"
    );
    where.append(feedTopicDto.getCommitMode().getQueryPiece());

    if (!feedTopicDto.getSections().isEmpty()) {
      where.append(" AND section in (");
      boolean first = true;
      for (int section : feedTopicDto.getSections()) {
        if (!first) {
          where.append(',');
        }
        where.append(section);
        first = false;
      }
      where.append(')');
    }

    if (feedTopicDto.getGroup() != 0) {
      where
        .append(" AND groupid=")
        .append(feedTopicDto.getGroup());
    }

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    switch (feedTopicDto.getDateLimitType()) {
      case BETWEEN:
        where
          .append(" AND postdate>='")
          .append(dateFormat.format(feedTopicDto.getFromDate()))
          .append("'::timestamp AND postdate<'")
          .append(dateFormat.format(feedTopicDto.getToDate()))
          .append("'::timestamp ");
        break;
      case MONTH_AGO:
        where
          .append("AND postdate>'")
          .append(dateFormat.format(feedTopicDto.getFromDate()))
          .append("'::timestamp ");
        break;
      default:
    }

    if (feedTopicDto.getUserId() != 0) {
      if (feedTopicDto.isUserFavs()) {
        where
          .append(" AND memories.userid=")
          .append(feedTopicDto.getUserId());
      } else {
        where
          .append(" AND userid=")
          .append(feedTopicDto.getUserId());
      }
    }

    if (feedTopicDto.isNotalks()) {
      where.append(" AND not topics.groupid=8404");
    }

    if (feedTopicDto.isTech()) {
      where.append(" AND not topics.groupid=8404 AND not topics.groupid=4068 AND groups.section=2");
    }

    if (feedTopicDto.getTag() != 0) {
      where
        .append(" AND topics.moderate AND topics.id IN (SELECT msgid FROM tags WHERE tagid=")
        .append(feedTopicDto.getTag())
        .append(')');
    }
    return where.toString();
  }

  /**
   * Создание условий сортировки SQL-запроса.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return строка, содержащая условия сортировки
   */
  private String makeSortOrder(FeedTopicDto feedTopicDto) {
    if (feedTopicDto.isUserFavs()) {
      return "ORDER BY memories.id DESC";
    }

    switch (feedTopicDto.getCommitMode()) {
      case COMMITED_ONLY:
        return " ORDER BY commitdate DESC";
      case UNCOMMITED_ONLY:
        return " ORDER BY postdate DESC";
      case POSTMODERATED_ONLY:
        return " ORDER BY postdate DESC";
      default:
        return " ORDER BY COALESCE(commitdate, postdate) DESC";
    }
  }

  /**
   * Создание ограничений размера результатов SQL-запроса.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return строка, содержащая смещение и количество записей
   */
  private String makeLimitAndOffset(FeedTopicDto feedTopicDto) {
    String limitStr = "";
    if (feedTopicDto.getLimit() != null) {
      limitStr += " LIMIT " + feedTopicDto.getLimit().toString();
    }

    if (feedTopicDto.getOffset() != null) {
      limitStr += " OFFSET " + feedTopicDto.getOffset().toString();
    }
    return limitStr;
  }
}
