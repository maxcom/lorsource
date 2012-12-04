/*
 * Copyright 1998-2012 Linux.org.ru
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
import ru.org.linux.user.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TopicListDaoImpl implements TopicListDao {

  private static final Log logger = LogFactory.getLog(TopicListDao.class);

  private static final RowMapper<TopicListDto.DeletedTopic> rowMapperForDeletedTopics = getRowMapperForDeletedTopics();

  private JdbcTemplate jdbcTemplate;


  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Override
  public List<Topic> getTopics(TopicListDto topicListDto) {
    logger.debug("TopicListDao.getTopics(); topicListDto = " + topicListDto.toString());
    String where = makeConditions(topicListDto);
    String sort = makeSortOrder(topicListDto);
    String limit = makeLimitAndOffset(topicListDto);

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
    if (topicListDto.isUserFavs()) {
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

  @Override
  public List<TopicListDto.DeletedTopic> getDeletedTopics(Integer sectionId) {
    StringBuilder query = new StringBuilder();
    List <Object> queryParameters = new ArrayList<Object>();

    query
      .append("SELECT ")
      .append("topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, ")
      .append("groups.id as guid, sections.name as ptitle, reason ")
      .append("FROM topics,groups,users,sections,del_info ")
      .append("WHERE sections.id=groups.section AND topics.userid=users.id ")
      .append("AND topics.groupid=groups.id AND sections.moderate AND deleted ")
      .append("AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ")
      .append("AND delDate is not null ");
    if (sectionId != null && sectionId != 0) {
      query.append(" AND section=? ");
      queryParameters.add(sectionId);
    }
    query.append(" ORDER BY del_info.delDate DESC LIMIT 20");

    return jdbcTemplate.query(
      query.toString(),
      queryParameters.toArray(),
      rowMapperForDeletedTopics
    );
  }

  @Override
  public List<DeletedTopicForUser> getDeletedTopicsForUser(User user, int offset, int limit) {
    StringBuilder query = new StringBuilder();
    List <Object> queryParameters = new ArrayList<Object>();
    query
        .append("SELECT")
        .append(" del_info.msgid as msgid, topics.title as subj, nick, reason, bonus, delby, ")
        .append(" case deldate is null when 't' then '1970-01-01 00:00:00'::timestamp else deldate end as del_date ")
        .append(" FROM del_info ")
        .append(" JOIN topics ON topics.id = del_info.msgid ")
        .append(" JOIN users ON users.id = topics.userid ")
        .append(" WHERE users.id = ? ")
        .append(" ORDER BY del_date DESC ");

    queryParameters.add(user.getId());

    if(limit == 0) {
      query.append(" OFFSET ? ");
      queryParameters.add(offset);
    } else {
      query.append(" LIMIT ? OFFSET ?");
      queryParameters.add(limit);
      queryParameters.add(offset);
    }

    return jdbcTemplate.query(query.toString(), queryParameters.toArray(), new RowMapper<DeletedTopicForUser>() {
      @Override
      public DeletedTopicForUser mapRow(ResultSet resultSet, int i) throws SQLException {
        return new DeletedTopicForUser(resultSet);
      }
    });
  }

  /**
   *
   * @return
   */
  private static RowMapper<TopicListDto.DeletedTopic> getRowMapperForDeletedTopics() {
    return new RowMapper<TopicListDto.DeletedTopic>() {
      @Override
      public TopicListDto.DeletedTopic mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TopicListDto.DeletedTopic(rs);
      }
    };
  }

  /**
   * Создание условий выборки SQL-запроса.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return строка, содержащая условия выборки SQL-запроса
   */
  private static String makeConditions(TopicListDto topicListDto) {
    StringBuilder where = new StringBuilder(
      "NOT deleted"
    );
    where.append(topicListDto.getCommitMode().getQueryPiece());

    if (!topicListDto.getSections().isEmpty()) {
      StringBuilder whereSections = new StringBuilder();

      for (Integer section : topicListDto.getSections()) {
        if (section == null || section == 0) {
          continue;
        }
        if (whereSections.length() != 0) {
          whereSections.append(',');
        }
        whereSections.append(section);
      }
      if (whereSections.length() != 0) {
        where
          .append(" AND section in (")
          .append(whereSections)
          .append(')');
      }
    }

    if (topicListDto.getGroup() != 0) {
      where
        .append(" AND groupid=")
        .append(topicListDto.getGroup());
    }

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    switch (topicListDto.getDateLimitType()) {
      case BETWEEN:
        where
          .append(" AND postdate>='")
          .append(dateFormat.format(topicListDto.getFromDate()))
          .append("'::timestamp AND postdate<'")
          .append(dateFormat.format(topicListDto.getToDate()))
          .append("'::timestamp ");
        break;
      case MONTH_AGO:
        where
          .append("AND postdate>'")
          .append(dateFormat.format(topicListDto.getFromDate()))
          .append("'::timestamp ");
        break;
      default:
    }

    if (topicListDto.getUserId() != 0) {
      if (topicListDto.isUserFavs()) {
        where
          .append(" AND memories.userid=")
          .append(topicListDto.getUserId());
      } else {
        where
          .append(" AND userid=")
          .append(topicListDto.getUserId());
      }

      if (topicListDto.isUserFavs()) {
        if (topicListDto.isUserWatches()) {
          where.append(" AND watch ");
        } else {
          where.append(" AND NOT watch ");
        }
      }
    }

    if (topicListDto.isNotalks()) {
      where.append(" AND not topics.groupid=8404");
    }

    if (topicListDto.isTech()) {
      where.append(" AND not topics.groupid=8404 AND not topics.groupid=4068 AND groups.section=2");
    }

    if (topicListDto.getTag() != 0) {
      where
        .append(" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=")
        .append(topicListDto.getTag())
        .append(')');
    }
    return where.toString();
  }

  /**
   * Создание условий сортировки SQL-запроса.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return строка, содержащая условия сортировки
   */
  private static String makeSortOrder(TopicListDto topicListDto) {
    if (topicListDto.isUserFavs()) {
      return "ORDER BY memories.id DESC";
    }

    switch (topicListDto.getCommitMode()) {
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
   * @param topicListDto объект, содержащий условия выборки
   * @return строка, содержащая смещение и количество записей
   */
  private static String makeLimitAndOffset(TopicListDto topicListDto) {
    String limitStr = "";
    if (topicListDto.getLimit() != null) {
      limitStr += " LIMIT " + topicListDto.getLimit().toString();
    }

    if (topicListDto.getOffset() != null) {
      limitStr += " OFFSET " + topicListDto.getOffset().toString();
    }
    return limitStr;
  }
}
