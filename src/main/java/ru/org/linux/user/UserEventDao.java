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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserEventDao {
  private static final Log logger = LogFactory.getLog(UserEventDao.class);

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

  /**
   * Добавление уведомления
   *
   * @param eventType тип уведомления
   * @param userId    идентификационный номер пользователя
   * @param isPrivate приватное ли уведомление
   * @param topicId   идентификационный номер топика (null если нет)
   * @param commentId идентификационный номер комментария (null если нет)
   * @param message   дополнительное сообщение уведомления (null если нет)
   */
  public void addEvent(
    String eventType,
    int userId,
    boolean isPrivate,
    Integer topicId,
    Integer commentId,
    String message
  ) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("userid", userId);
    params.put("type", eventType);
    params.put("private", isPrivate);
    if (topicId != null) {
      params.put("message_id", topicId);
    }
    if (commentId != null) {
      params.put("comment_id", commentId);
    }
    if (message != null) {
      params.put("message", message);
    }

    insert.execute(params);
  }

  /**
   * Сброс уведомлений.
   *
   * @param userId идентификационный номер пользователь которому сбрасываем
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void resetUnreadReplies(int userId) {
    jdbcTemplate.update(UPDATE_RESET_UNREAD_REPLIES, userId);
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread", userId);
  }

  /**
   * Получение списка первых 10 идентификационных номеров пользователей,
   * количество уведомлений которых превышает максимально допустимое значение.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   * @return список идентификационных номеров пользователей
   */
  public List<Integer> getUserIdListByOldEvents(int maxEventsPerUser) {
    final List<Integer> oldEventsList = jdbcTemplate.queryForList(
      "select userid from user_events group by userid having count(user_events.id) > ? order by count(user_events.id) DESC limit 10",
      Integer.class,
      maxEventsPerUser
    );
    return oldEventsList;
  }

  /**
   * Очистка старых уведомлений пользователя.
   *
   * @param userId           идентификационный номер пользователя
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   */
  public void cleanupOldEvents(int userId, int maxEventsPerUser) {
    jdbcTemplate.update(
      "DELETE FROM user_events WHERE user_events.id IN (SELECT id FROM user_events WHERE userid=? ORDER BY event_date DESC OFFSET ?)",
      userId,
      maxEventsPerUser
    );
  }

  /**
   * Получить список уведомлений для пользователя.
   *
   * @param userId          идентификационный номер пользователя
   * @param showPrivate     включать ли приватные
   * @param topics          кол-во уведомлений
   * @param offset          сдвиг относительно начала
   * @param eventFilterType тип уведомлений
   * @return список уведомлений
   */
  public List<UserEvent> getRepliesForUser(int userId, boolean showPrivate, int topics, int offset,
                                           String eventFilterType) {
    String queryString;
    if (showPrivate) {
      String queryPart = "";
      if (eventFilterType != null) {
        queryPart = " AND type = '" + eventFilterType + "' ";
      }
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
    }, userId, topics, offset);
  }
}
