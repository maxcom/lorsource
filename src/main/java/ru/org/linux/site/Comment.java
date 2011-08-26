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

package ru.org.linux.site;

import java.io.Serializable;
import java.sql.*;
import java.util.Date;
import java.util.Map;

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
  public static final int TITLE_LENGTH = 250;

  public Comment(Connection db, ResultSet rs) throws SQLException {
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
      deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);
    } else {
      deleteInfo = null;
    }
  }

  public Comment(Integer replyto, String title, int topic, int userid, String userAgent, String postIP) {
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

  public boolean isIgnored(Map<Integer, String> ignoreList) {
    return ignoreList != null && !ignoreList.isEmpty() && ignoreList.keySet().contains(userid);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public int getTopic() {
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

  public DeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getPostIP() {
    return postIP;
  }

  public int saveNewMessage(Connection db, String remoteAddr, String userAgent, String message) throws SQLException {
    PreparedStatement pstMsgbase = null;
    PreparedStatement pst = null;
    try {
      // allocation MSGID
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      int msgid = rs.getInt("msgid");

      // insert headers
      pst = db.prepareStatement("INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, '" + remoteAddr + "',create_user_agent(?))");
      pst.setInt(1, msgid);
      pst.setInt(2, userid);
      pst.setString(3, title);
      pst.setInt(5, topic);
      pst.setString(6, userAgent);

      if (replyto != 0) {
        pst.setInt(4, replyto);
      } else {
        pst.setNull(4, Types.INTEGER);
      }

      //pst.setString(6, request.getRemoteAddr());
      pst.executeUpdate();

      // insert message text
      pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message, bbcode) values (?,?,?)");
      pstMsgbase.setLong(1, msgid);
      pstMsgbase.setString(2, message);
      pstMsgbase.setBoolean(3, true);
      pstMsgbase.executeUpdate();

      rs.close();
      st.close();

      return msgid;
    } finally {
      if (pst != null) {
        pst.close();
      }

      if (pstMsgbase!=null) {
        pstMsgbase.close();
      }
    }
  }
}
