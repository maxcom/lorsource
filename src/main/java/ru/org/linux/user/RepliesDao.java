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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Доступ к данным о уведомлениях
 */
@Repository
public class RepliesDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  private static final String queryAllRepliesForUser =
      "SELECT event_date, " +
          " topics.title as subj, groups.title as gtitle, " +
          " lastmod, topics.id as msgid, " +
          " comments.id AS cid, " +
          " comments.postdate AS cDate, " +
          " comments.userid AS cAuthor, " +
          " unread, " +
          " urlname, groups.section, comments.deleted," +
          " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
          " INNER JOIN groups ON (groups.id = topics.groupid) " +
          " LEFT JOIN comments ON (comments.id=comment_id) " +
      " WHERE user_events.userid = ? " +
          " %s " +
          " AND (comments.id is null or NOT comments.topic_deleted)" +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";

  private static final String queryRepliesForUserWihoutPrivate =
      "SELECT event_date, " +
          " topics.title as subj, groups.title as gtitle, " +
          " lastmod, topics.id as msgid, " +
          " comments.id AS cid, " +
          " comments.postdate AS cDate, " +
          " comments.userid AS cAuthor, " +
          " unread, " +
          " urlname, groups.section, comments.deleted," +
          " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
          " INNER JOIN groups ON (groups.id = topics.groupid) " +
          " LEFT JOIN comments ON (comments.id=comment_id) " +
      " WHERE user_events.userid = ? " +
          " AND NOT private " +
          " AND (comments.id is null or NOT comments.topic_deleted)" +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";


  /**
   * Получить список уведомлений для пользователя
   *
   *
   * @param user пользователь
   * @param showPrivate включать ли приватные
   * @param topics кол-во уведомлений
   * @param offset сдвиг относительно начала
   * @return список уведомлений
   */
  public List<UserEvent> getRepliesForUser(User user, boolean showPrivate, int topics, int offset,
                                           UserEventFilterEnum eventFilter) {
    String queryString;
    if(showPrivate) {
      String queryPart = "";
      if (eventFilter != UserEventFilterEnum.ALL)
        queryPart = " AND type = '"+eventFilter.getType()+"' ";

      queryString = String.format(queryAllRepliesForUser, queryPart);
    } else {
      queryString = queryRepliesForUserWihoutPrivate;
    }
    return jdbcTemplate.query(queryString, new RowMapper<UserEvent>() {
      @Override
      public UserEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        String subj = StringUtil.makeTitle(resultSet.getString("subj"));
        Timestamp lastmod = resultSet.getTimestamp("lastmod");
        if (lastmod == null) {
          lastmod = new Timestamp(0);
        }
        Timestamp eventDate = resultSet.getTimestamp("event_date");
        int cid = resultSet.getInt("cid");
        int cAuthor;
        Timestamp cDate;
        if (!resultSet.wasNull()) {
          cAuthor = resultSet.getInt("cAuthor");
          cDate = resultSet.getTimestamp("cDate");
        } else {
          cDate = null;
          cAuthor = 0;
        }
        String groupTitle = resultSet.getString("gtitle");
        String groupUrlName = resultSet.getString("urlname");
        int sectionId = resultSet.getInt("section");
        int msgid = resultSet.getInt("msgid");
        UserEventFilterEnum type = UserEventFilterEnum.valueOfByType (resultSet.getString("type"));
        String eventMessage = resultSet.getString("ev_msg");

        boolean unread = resultSet.getBoolean("unread");

        return new UserEvent(cid, cAuthor, cDate, groupTitle, groupUrlName,
                sectionId, subj, lastmod, msgid, type, eventMessage, eventDate, unread);
      }
    }, user.getId(), topics, offset);
  }
}
