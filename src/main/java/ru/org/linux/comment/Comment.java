/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.comment;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

/**
 * DTO-объект для хранения одного  комментария из DAO
 */
public class Comment implements Serializable {
  private final int msgid;
  private final String title;
  private final int userid;
  private final int replyto;
  private final int topic;
  private final boolean deleted;
  private final Timestamp postDate;
  private final int userAgentId;
  private final String postIP;
  private final int editorId;
  private final Timestamp editDate;
  private final int editCount;
  public static final int TITLE_LENGTH = 250;

  public Comment(ResultSet rs) throws SQLException {
    msgid=rs.getInt("msgid");
    title=rs.getString("title");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postDate=rs.getTimestamp("postDate");
    userid=rs.getInt("userid");
    userAgentId=rs.getInt("ua_id");
    postIP=rs.getString("postip");
    editCount = rs.getInt("edit_count");
    editorId = rs.getInt("editor_id");
    editDate =rs.getTimestamp("edit_date");
  }

  public Comment(
          Integer replyto,
          String title,
          int topic,
          int msgid,
          int userid,
          String postIP
  ) {
    this.msgid = msgid;
    this.title=title;
    this.topic=topic;

    if (replyto!=null) {
      this.replyto=replyto;
    } else {
      this.replyto=0;
    }

    editCount = 0;
    editDate = null;
    editorId = 0;
    deleted =false;
    postDate =new Timestamp(System.currentTimeMillis());
    this.userid=userid;
    userAgentId =0;
    this.postIP=postIP;
  }

  public int getId() {
    return msgid;
  }

  public int getReplyTo() {
    return replyto;
  }

  public boolean isIgnored(Set<Integer> ignoreList) {
    return ignoreList != null && !ignoreList.isEmpty() && ignoreList.contains(userid);
  }

  public boolean isDeleted() {
    return deleted;
  }

  /**
   * @return id топика в котором находится сообщение
   */
  public int getTopicId() {
    return topic;
  }

  @Nonnull
  public String getTitle() {
    return title;
  }

  public Timestamp getPostDate() {
    return postDate;
  }

  public int getUserid() {
    return userid;
  }

  public int getUserAgentId() {
    return userAgentId;
  }

  public String getPostIP() {
    return postIP;
  }

  public int getEditorId() {
    return editorId;
  }

  public Timestamp getEditDate() {
    return editDate;
  }

  public int getEditCount() {
    return editCount;
  }
}
