/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.admin.ipmanage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

class SameIpDto {

  static class TopicItem {
    private final String ptitle;
    private final String gtitle;
    private final int commentId;
    private final String title;
    private final Timestamp postdate;
    private final int topicId;
    private final boolean deleted;

    public TopicItem(ResultSet rs) throws SQLException {
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      commentId = rs.getInt("comment_id");
      title = rs.getString("title");
      postdate = rs.getTimestamp("postdate");
      topicId = rs.getInt("topic_id");
      deleted = rs.getBoolean("deleted");
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public int getCommentId() {
      return commentId;
    }

    public String getTitle() {
      return title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public int getTopicId() {
      return topicId;
    }

    public boolean isDeleted() {
      return deleted;
    }
  }


  static class UserItem {
    private final Timestamp lastdate;
    private final String nick;
    private final String userAgent;
    private final int uaId;

    public UserItem(ResultSet rs) throws SQLException {
      lastdate = rs.getTimestamp("lastdate");
      nick = rs.getString("nick");
      this.uaId = rs.getInt("ua_id");
      userAgent = rs.getString("user_agent");
    }

    public int getUaId() {
      return uaId;
    }

    public Timestamp getLastdate() {
      return lastdate;
    }

    public String getNick() {
      return nick;
    }

    public String getUserAgent() {
      return userAgent;
    }
  }
}
