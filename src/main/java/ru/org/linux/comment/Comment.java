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

package ru.org.linux.comment;

import ru.org.linux.site.DeleteInfo;
import ru.org.linux.spring.dao.DeleteInfoDao;

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
  private final Timestamp postdate;
  private final DeleteInfo deleteInfo;
  private final String userAgent;
  private final String postIP;
  private final String editNick;
  private final Timestamp editDate;
  private final int editCount;
  public static final int TITLE_LENGTH = 250;

  public Comment(ResultSet rs, DeleteInfoDao deleteInfoDao) throws SQLException {
    msgid=rs.getInt("msgid");
    title=rs.getString("title");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userid=rs.getInt("userid");
    userAgent=rs.getString("useragent");
    postIP=rs.getString("postip");
    editCount = rs.getInt("edit_count");
    editNick = rs.getString("edit_nick");
    editDate =rs.getTimestamp("edit_date");

    if (deleted) {
      deleteInfo = deleteInfoDao.getDeleteInfo(msgid);
    } else {
      deleteInfo = null;
    }
  }

  public Comment(
          Integer replyto,
          String title,
          int topic,
          int msgid,
          int userid,
          String userAgent,
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
    editNick = null;
    deleted =false;
    postdate =new Timestamp(System.currentTimeMillis());
    this.userid=userid;
    deleteInfo = null;
    this.userAgent=userAgent;
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

  public String getTitle() {
    return title;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public int getUserid() {
    return userid;
  }

  public DeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getPostIP() {
    return postIP;
  }

  public String getEditNick() {
    return editNick;
  }

  public Timestamp getEditDate() {
    return editDate;
  }

  public int getEditCount() {
    return editCount;
  }
}
