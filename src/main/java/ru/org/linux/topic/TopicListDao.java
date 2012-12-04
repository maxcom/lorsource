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

import ru.org.linux.user.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface TopicListDao {
  enum CommitMode {
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

  /**
   * Получение список топиков.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return список топиков
   */
  List<Topic> getTopics(TopicListDto topicListDto);

  /**
   *
   * @param sectionId
   * @return
   */
  List<TopicListDto.DeletedTopic> getDeletedTopics(Integer sectionId);

  /**
   * Удаленные топики пользователя user
   * @param user пользователь
   * @return список удаленных топиков
   */
  List<DeletedTopicForUser> getDeletedTopicsForUser(User user, int offset, int limit);

  public static class DeletedTopicForUser {
    private final int id;
    private final String title;
    private final String reason;
    private final int bonus;
    private final int moderatorId;
    private final Timestamp date;

    public DeletedTopicForUser(ResultSet rs) throws SQLException {
      id = rs.getInt("msgid");
      title = rs.getString("subj");
      reason = rs.getString("reason");
      bonus = rs.getInt("bonus");
      moderatorId = rs.getInt("delby");
      date = rs.getTimestamp("del_date");
    }

    public DeletedTopicForUser(int id, String title, String reason, int bonus, int moderatorId, Timestamp date) {
      this.id = id;
      this.title = title;
      this.reason = reason;
      this.bonus = bonus;
      this.moderatorId = moderatorId;
      this.date = date;
    }

    public int getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public String getReason() {
      return reason;
    }

    public int getBonus() {
      return bonus;
    }

    public int getModeratorId() {
      return moderatorId;
    }

    public Timestamp getDate() {
      return date;
    }
  }
}
