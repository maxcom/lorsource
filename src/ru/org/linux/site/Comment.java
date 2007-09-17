package ru.org.linux.site;

import java.io.Serializable;
import java.sql.*;
import java.util.Map;

import ru.org.linux.util.StringUtil;

public class Comment implements Serializable {
  private final int msgid;
  private final String title;
  private int userid;
  private final int replyto;
  private final int topic;
  private final boolean deleted;
  private final Timestamp postdate;
  private final String message;
  private final DeleteInfo deleteInfo;

  public Comment(Connection db, ResultSet rs) throws SQLException {
    msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userid=rs.getInt("userid");
    message=rs.getString("message");

    if (deleted) {
      deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);
    } else {
      deleteInfo = null;
    }
  }

  public Comment(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs=st.executeQuery("SELECT " +
        "postdate, topic, users.id as userid, comments.id as msgid, comments.title, " +
        "deleted, replyto, message " +
        "FROM comments, users, msgbase " +
        "WHERE comments.id="+msgid+" AND comments.id=msgbase.id AND comments.userid=users.id");

    if (!rs.next()) throw new MessageNotFoundException(msgid);

    this.msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    message=rs.getString("message");
    userid=rs.getInt("userid");

    st.close();

    if (deleted) {
      deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);
    } else {
      deleteInfo = null;
    }
  }

  public Comment(int replyto, String title, String message, int topic, int userid) {
    msgid =0;
    this.title=title;
    this.topic=topic;
    this.replyto=replyto;
    deleted =false;
    postdate =new Timestamp(0);
    this.message=message;
    this.userid=userid;
    deleteInfo = null;
  }

  public int getMessageId() {
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

  public Timestamp getPostdate() {
    return postdate;
  }

  public int getUserid() {
    return userid;
  }

  public String getMessageText() {
    return message;
  }

  public DeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  public int saveNewMessage(Connection db, String remoteAddr) throws SQLException {
    PreparedStatement pst = null;
    PreparedStatement pstMsgbase = null;

    try {
      // allocation MSGID
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      int msgid = rs.getInt("msgid");

      // insert headers
      pst = db.prepareStatement("INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, '" + remoteAddr + "')");
      pst.setInt(1, msgid);
      pst.setInt(2, userid);
      pst.setString(3, title);
      pst.setInt(5, topic);

      if (replyto != 0) {
        pst.setInt(4, replyto);
      } else {
        pst.setNull(4, Types.INTEGER);
      }

      //pst.setString(6, request.getRemoteAddr());
      pst.executeUpdate();

      // insert message text
      pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message) values (?,?)");
      pstMsgbase.setLong(1, msgid);
      pstMsgbase.setString(2, message);
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

  public void setAuthor(int userid) {
    if (msgid!=0) {
      throw new IllegalArgumentException("cant change author to stored message");
    }

    this.userid = userid;
  }
}
