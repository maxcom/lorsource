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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class UserEventsDao {
  private static final Log logger = LogFactory.getLog(UserEventsDao.class);

  private static final String UPDATE_RESET_UNREAD_REPLIES = "UPDATE users SET unread_events=0 where id=?";

  private static final String QUERY_ALL_REPLIES_FOR_USER =
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

  private static final String QUERY_REPLIES_FOR_USER_WIHOUT_PRIVATE =
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

  private SimpleJdbcInsert insert;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    insert = new SimpleJdbcInsert(ds);

    insert.setTableName("user_events");
    insert.usingColumns("userid", "type", "private", "message_id", "comment_id", "message");

    jdbcTemplate = new JdbcTemplate(ds);
  }

  public void addUserRefEvent(User[] refs, int topic, int comment) {
    if (refs.length == 0) {
      return;
    }

    Map<String, Object>[] batch = new Map[refs.length];

    for (int i = 0; i < refs.length; i++) {
      User ref = refs[i];

      batch[i] = ImmutableMap.<String, Object>of(
        "userid", ref.getId(),
        "type", UserEventFilterEnum.REFERENCE.getType(),
        "private", false,
        "message_id", topic,
        "comment_id", comment
      );
    }

    insert.executeBatch(batch);
  }

  public void addUserRefEvent(User[] refs, int topic) {
    if (refs.length == 0) {
      return;
    }

    Map<String, Object>[] batch = new Map[refs.length];

    for (int i = 0; i < refs.length; i++) {
      User ref = refs[i];

      batch[i] = ImmutableMap.<String, Object>of(
        "userid", ref.getId(),
        "type", UserEventFilterEnum.REFERENCE.getType(),
        "private", false,
        "message_id", topic
      );
    }

    insert.executeBatch(batch);
  }

  public void addReplyEvent(User parentAuthor, int topicId, int commentId) {
    insert.execute(ImmutableMap.<String, Object>of(
      "userid", parentAuthor.getId(),
      "type", UserEventFilterEnum.ANSWERS.getType(),
      "private", false,
      "message_id", topicId,
      "comment_id", commentId
    ));
  }

  /**
   * Сброс уведомлений
   *
   * @param user пользователь которому сбрасываем
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void resetUnreadReplies(User user) {
    jdbcTemplate.update(UPDATE_RESET_UNREAD_REPLIES, user.getId());
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=?", user.getId());
  }

  /**
   * Очистка старых уведомлений пользователей.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   */
  public void cleanupOldEvents(final int maxEventsPerUser) {
    final List<Integer> deleteList = new ArrayList<Integer>();

    jdbcTemplate.query(
      "select userid, count(user_events.id) from user_events group by userid order by count desc limit 10",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          if (rs.getInt("count") > maxEventsPerUser) {
            deleteList.add(rs.getInt("userid"));
          }
        }
      }
    );

    for (int id : deleteList) {
      logger.info("Cleaning up messages for userid=" + id);

      jdbcTemplate.update(
        "DELETE FROM user_events WHERE user_events.id IN (SELECT id FROM user_events WHERE userid=? ORDER BY event_date DESC OFFSET ?)",
        id,
        maxEventsPerUser
      );
    }
  }

  /**
   * Получить список уведомлений для пользователя
   *
   * @param user        пользователь
   * @param showPrivate включать ли приватные
   * @param topics      кол-во уведомлений
   * @param offset      сдвиг относительно начала
   * @return список уведомлений
   */
  public List<UserEvent> getRepliesForUser(User user, boolean showPrivate, int topics, int offset,
                                           UserEventFilterEnum eventFilter) {
    String queryString;
    if (showPrivate) {
      String queryPart = "";
      if (eventFilter != UserEventFilterEnum.ALL)
        queryPart = " AND type = '" + eventFilter.getType() + "' ";

      queryString = String.format(QUERY_ALL_REPLIES_FOR_USER, queryPart);
    } else {
      queryString = QUERY_REPLIES_FOR_USER_WIHOUT_PRIVATE;
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
        UserEventFilterEnum type = UserEventFilterEnum.valueOfByType(resultSet.getString("type"));
        String eventMessage = resultSet.getString("ev_msg");

        boolean unread = resultSet.getBoolean("unread");

        return new UserEvent(cid, cAuthor, cDate, groupTitle, groupUrlName,
          sectionId, subj, lastmod, msgid, type, eventMessage, eventDate, unread);
      }
    }, user.getId(), topics, offset);
  }
}
