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
    private String ptitle;
    private String gtitle;
    private int commentId;
    private String title;
    private Timestamp postdate;
    private int topicId;
    private boolean deleted;

    public TopicItem() {

    }

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

    public void setPtitle(String ptitle) {
      this.ptitle = ptitle;
    }

    public void setGtitle(String gtitle) {
      this.gtitle = gtitle;
    }

    public void setCommentId(int commentId) {
      this.commentId = commentId;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public void setPostdate(Timestamp postdate) {
      this.postdate = postdate;
    }

    public void setTopicId(int topicId) {
      this.topicId = topicId;
    }

    public void setDeleted(boolean deleted) {
      this.deleted = deleted;
    }
  }


  static class UserItem {
    private Timestamp lastdate;
    private String nick;
    private String userAgent;
    private int uaId;

    public UserItem() {

    }

    public UserItem(ResultSet rs) throws SQLException {
      lastdate = rs.getTimestamp("lastdate");
      nick = rs.getString("nick");
      this.uaId = rs.getInt("ua_id");
      userAgent = rs.getString("user_agent");
    }

    public void setLastdate(Timestamp lastdate) {
      this.lastdate = lastdate;
    }

    public void setNick(String nick) {
      this.nick = nick;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public void setUaId(int uaId) {
      this.uaId = uaId;
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
