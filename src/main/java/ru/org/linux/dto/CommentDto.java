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

package ru.org.linux.dto;

import ru.org.linux.dao.DeleteInfoDao;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

public class CommentDto implements Serializable {
  private final int msgid;
  private final String title;
  private final int userid;
  private final int replyto;
  private final int topic;
  private final boolean deleted;
  private final Timestamp postdate;
  private final DeleteInfoDto deleteInfo;
  private final String userAgent;
  private final String postIP;
  public static final int TITLE_LENGTH = 250;

  public CommentDto(ResultSet rs, DeleteInfoDao deleteInfoDao) throws SQLException {
    msgid=rs.getInt("msgid");
    title=rs.getString("title");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userid=rs.getInt("userid");
    userAgent=rs.getString("useragent");
    postIP=rs.getString("postip");

    if (deleted) {
      deleteInfo = deleteInfoDao.getDeleteInfo(msgid);
    } else {
      deleteInfo = null;
    }
  }

  public CommentDto(
    Integer replyto,
    String title,
    int topic,
    int userid,
    String userAgent,
    String postIP
  ) {
    msgid =0;
    this.title=title;
    this.topic=topic;

    if (replyto!=null) {
      this.replyto=replyto;
    } else {
      this.replyto=0;
    }

    deleted =false;
    postdate =new Timestamp(System.currentTimeMillis());
    this.userid=userid;
    deleteInfo = null;
    this.userAgent=userAgent;
    this.postIP=postIP;
  }

  public int getMessageId() {
    return msgid;
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

  @Deprecated
  public int getTopic() {
    return topic;
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

  public Date getPostdate() {
    return postdate;
  }

  public int getUserid() {
    return userid;
  }

  public DeleteInfoDto getDeleteInfo() {
    return deleteInfo;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getPostIP() {
    return postIP;
  }
}
