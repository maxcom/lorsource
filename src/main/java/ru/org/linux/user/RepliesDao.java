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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.*;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;

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
  private UserDao userDao;
  private LorCodeService lorCodeService;
  private TopicDao messageDao;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setLorCodeService(LorCodeService lorCodeService) {
    this.lorCodeService = lorCodeService;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }



  private static final String queryAllRepliesForUser =
      "SELECT event_date, " +
          " topics.title as subj, sections.name, groups.title as gtitle, " +
          " lastmod, topics.id as msgid, " +
          " comments.id AS cid, " +
          " comments.postdate AS cDate, " +
          " comments.userid AS cAuthor, " +
          " msgbase.message AS cMessage, bbcode, " +
          " urlname, sections.id as section, comments.deleted," +
          " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
          " INNER JOIN groups ON (groups.id = topics.groupid) " +
          " INNER JOIN sections ON (sections.id = groups.section) " +
          " LEFT JOIN comments ON (comments.id=comment_id) " +
          " LEFT JOIN msgbase ON (msgbase.id = comments.id)" +
      " WHERE user_events.userid = ? " +
          " AND (comments.id is null or NOT comments.topic_deleted)" +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";

  private static final String queryRepliesForUserWihoutPrivate =
      "SELECT event_date, " +
          " topics.title as subj, sections.name, groups.title as gtitle, " +
          " lastmod, topics.id as msgid, " +
          " comments.id AS cid, " +
          " comments.postdate AS cDate, " +
          " comments.userid AS cAuthor, " +
          " msgbase.message AS cMessage, bbcode, " +
          " urlname, sections.id as section, comments.deleted," +
          " type, user_events.message as ev_msg" +
      " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
          " INNER JOIN groups ON (groups.id = topics.groupid) " +
          " INNER JOIN sections ON (sections.id = groups.section) " +
          " LEFT JOIN comments ON (comments.id=comment_id) " +
          " LEFT JOIN msgbase ON (msgbase.id = comments.id)" +
      " WHERE user_events.userid = ? " +
          " AND NOT private " +
          " AND (comments.id is null or NOT comments.topic_deleted)" +
      " ORDER BY event_date DESC LIMIT ?" +
      " OFFSET ?";


  /**
   * Получить список уведомлений для пользователя
   * @param user пользователь
   * @param showPrivate включать ли приватные
   * @param topics кол-во уведомлений
   * @param offset сдвиг относительно начала
   * @param readMessage возвращать ли отрендеренное содержимое уведомлений (используется только для RSS)
   * @param secure является ли текущие соединение https
   * @return список уведомлений
   */
  public List<RepliesListItem> getRepliesForUser(User user, boolean showPrivate, int topics, int offset,
                                                 final boolean readMessage, final boolean secure) {
    String queryString;
    if(showPrivate) {
      queryString = queryAllRepliesForUser;
    } else {
      queryString = queryRepliesForUserWihoutPrivate;
    }
    return jdbcTemplate.query(queryString, new RowMapper<RepliesListItem>() {
      @Override
      public RepliesListItem mapRow(ResultSet resultSet, int i) throws SQLException {
        String subj = StringUtil.makeTitle(resultSet.getString("subj"));
        Timestamp lastmod = resultSet.getTimestamp("lastmod");
        if (lastmod == null) {
          lastmod = new Timestamp(0);
        }
        Timestamp eventDate = resultSet.getTimestamp("event_date");
        int cid = resultSet.getInt("cid");
        User cAuthor;
        Timestamp cDate;
        if (!resultSet.wasNull()) {
          try {
            cAuthor = userDao.getUserCached(resultSet.getInt("cAuthor"));
          } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
          }
          cDate = resultSet.getTimestamp("cDate");
        } else {
          cDate = null;
          cAuthor = null;
        }
        String groupTitle = resultSet.getString("gtitle");
        String groupUrlName = resultSet.getString("urlname");
        String sectionTitle = resultSet.getString("name");
        int sectionId = resultSet.getInt("section");
        int msgid = resultSet.getInt("msgid");
        RepliesListItem.EventType type = RepliesListItem.EventType.valueOf(resultSet.getString("type"));
        String eventMessage = resultSet.getString("ev_msg");
        String messageText;
        if (readMessage) {
          if(cid != 0) { // Комментарий
            messageText = lorCodeService.prepareTextRSS(resultSet.getString("cMessage"), secure, resultSet.getBoolean("bbcode"));
          } else { // Топик
            Topic message;
            try {
              message = messageDao.getById(msgid);
            } catch (MessageNotFoundException e) {
              message = null;
            }
            if(message != null) {
              messageText = lorCodeService.prepareTextRSS(message.getMessage(), secure, message.isLorcode());
            } else {
              messageText = "";
            }
          }
        } else {
          messageText = null;
        }
        return new RepliesListItem(cid, cAuthor, cDate, messageText, groupTitle, groupUrlName,
            sectionTitle, sectionId, subj, lastmod, msgid, type, eventMessage, eventDate);
      }
    }, user.getId(), topics, offset);
  }
}
