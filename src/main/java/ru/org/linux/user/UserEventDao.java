/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserEventDao {

  private static final String QUERY_ALL_REPLIES_FOR_USER =
    "SELECT user_events.id, event_date, " +
      " topics.title as subj, " +
      " topics.id as msgid, " +
      " comments.id AS cid, " +
      " comments.userid AS cAuthor, " +
      " topics.userid AS tAuthor, " +
      " unread, " +
      " groupid, comments.deleted," +
      " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
      " LEFT JOIN comments ON (comments.id=comment_id) " +
      " WHERE user_events.userid = ? " +
      " %s " +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";

  private static final String QUERY_REPLIES_FOR_USER_WIHOUT_PRIVATE =
    "SELECT user_events.id, event_date, " +
      " topics.title as subj, " +
      " topics.id as msgid, " +
      " comments.id AS cid, " +
      " comments.userid AS cAuthor, " +
      " topics.userid AS tAuthor, " +
      " unread, " +
      " groupid, comments.deleted," +
      " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
      " LEFT JOIN comments ON (comments.id=comment_id) " +
      " WHERE user_events.userid = ? " +
      " AND NOT private " +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";

  private SimpleJdbcInsert insert;
  private SimpleJdbcInsert insertTopicUsersNotified;

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    insert = new SimpleJdbcInsert(ds);

    insert.setTableName("user_events");
    insert.usingColumns("userid", "type", "private", "message_id", "comment_id", "message");

    insertTopicUsersNotified = new SimpleJdbcInsert(ds);
    insertTopicUsersNotified.setTableName("topic_users_notified");
    insertTopicUsersNotified.usingColumns("topic", "userid");

    jdbcTemplate = new JdbcTemplate(ds);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(ds);
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
    Map<String, Object> params = new HashMap<>();
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

  public void insertTopicNotification(final int topicId, Iterable<Integer> userIds) {
    @SuppressWarnings("unchecked") Map<String, Object>[] batch = Iterables.toArray(
            Iterables.transform(
                    userIds,
                    new Function<Integer, Map<String, Object>>() {
                      @Nullable
                      @Override
                      public Map<String, Object> apply(Integer userId) {
                        return ImmutableMap.<String, Object>of("topic", topicId, "userid", userId);
                      }
                    }
            ), Map.class);

    insertTopicUsersNotified.executeBatch(batch);
  }

  public List<Integer> getNotifiedUsers(int topicId) {
    return jdbcTemplate.queryForList("SELECT userid FROM topic_users_notified WHERE topic=?", Integer.class, topicId);
  }

  /**
   * Сброс уведомлений.
   *
   * @param userId идентификационный номер пользователь которому сбрасываем
   * @param topId сбрасываем уведомления с идентификатором не больше этого
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void resetUnreadReplies(int userId, int topId) {
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=? AND unread AND id<=?", userId, topId);
    recalcEventCount(ImmutableList.of(userId));
  }

  public void recalcEventCount(Collection<Integer> userids) {
    if (userids.isEmpty()) {
      return;
    }

    namedJdbcTemplate.update(
            "UPDATE users SET unread_events = (SELECT count(*) FROM user_events WHERE unread AND userid=users.id) WHERE users.id IN (:list)",
            ImmutableMap.of("list", userids)
    );
  }

  /**
   * Получение списка первых 20 идентификационных номеров пользователей,
   * количество уведомлений которых превышает максимально допустимое значение.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   * @return список идентификационных номеров пользователей
   */
  public List<Integer> getUserIdListByOldEvents(int maxEventsPerUser) {
    return jdbcTemplate.queryForList(
      "select userid from user_events group by userid having count(user_events.id) > ? order by count(user_events.id) DESC limit 20",
      Integer.class,
      maxEventsPerUser
    );
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
    return jdbcTemplate.query(queryString, (resultSet, i) -> {
      String subj = StringUtil.makeTitle(resultSet.getString("subj"));
      Timestamp eventDate = resultSet.getTimestamp("event_date");
      int cid = resultSet.getInt("cid");
      int cAuthor;
      if (!resultSet.wasNull()) {
        cAuthor = resultSet.getInt("cAuthor");
      } else {
        cAuthor = 0;
      }
      int groupId = resultSet.getInt("groupid");
      int msgid = resultSet.getInt("msgid");
      UserEventFilterEnum type = UserEventFilterEnum.valueOfByType(resultSet.getString("type"));
      String eventMessage = resultSet.getString("ev_msg");

      boolean unread = resultSet.getBoolean("unread");

      return new UserEvent(cid, cAuthor,
              groupId, subj, msgid, type, eventMessage, eventDate, unread, resultSet.getInt("tAuthor"), resultSet.getInt("id"));
    }, userId, topics, offset);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteTopicEvents(Collection<Integer> topics) {
    if (topics.isEmpty()) {
      return ImmutableList.of();
    }

    List<Integer> affectedUsers = namedJdbcTemplate.queryForList("SELECT DISTINCT (userid) FROM user_events " +
            "WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH')",
            ImmutableMap.of("list", topics),
            Integer.class);

    namedJdbcTemplate.update(
            "DELETE FROM user_events WHERE message_id IN (:list) AND type IN ('TAG', 'REF', 'REPLY', 'WATCH')",
            ImmutableMap.of("list", topics)
    );

    return affectedUsers;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteCommentEvents(Collection<Integer> comments) {
    if (comments.isEmpty()) {
      return ImmutableList.of();
    }

    List<Integer> affectedUsers = namedJdbcTemplate.queryForList("SELECT DISTINCT (userid) FROM user_events " +
            "WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF')",
            ImmutableMap.of("list", comments),
            Integer.class);

    namedJdbcTemplate.update(
            "DELETE FROM user_events WHERE comment_id IN (:list) AND type in ('REPLY', 'WATCH', 'REF')",
            ImmutableMap.of("list", comments)
    );

    return affectedUsers;
  }
}
