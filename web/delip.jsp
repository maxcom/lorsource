<%@ page import="java.sql.*" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date,ru.org.linux.site.*,ru.org.linux.util.HTMLFormatter,ru.org.linux.util.ServletParameterParser" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<%@ page pageEncoding="koi8-r" contentType="text/html;charset=utf-8" language="java"   %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>delip</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  String reason = new ServletParameterParser(request).getString("reason");
  String ip = new ServletParameterParser(request).getString("ip");
  String time = new ServletParameterParser(request).getString("time");

  Calendar calendar = Calendar.getInstance();
  calendar.setTime(new Date());

  if ("hour".equals(time)) {
    calendar.add(Calendar.HOUR_OF_DAY, -1);
  } else if ("day".equals(time)) {
    calendar.add(Calendar.DAY_OF_MONTH, -1);
  } else if ("3day".equals(time)) {
    calendar.add(Calendar.DAY_OF_MONTH, -3);
  } else {
    throw new UserErrorException("Invalid count");
  }

  Timestamp ts = new Timestamp(calendar.getTimeInMillis());
  out.println("Удаляем темы и сообщения после "+ts.toString()+" с IP "+ip+"<br>");

  if (!request.getMethod().equals("POST")) {
    throw new IllegalAccessException("Invalid method");
  }

  Connection db = null;

  try {
    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    User moderator = User.getUser(db, (String) session.getValue("nick"));

    PreparedStatement st = null;
    ResultSet rs = null;
    CommentDeleter deleter = null;
    
    StringBuilder output = new StringBuilder();

    try {
      // Delete IP topics
      PreparedStatement lock = db.prepareStatement("SELECT id FROM topics WHERE postip=?::inet AND not deleted AND postdate>? FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
      lock.setString(1, ip);
      lock.setTimestamp(2, ts);
      st2.setInt(2, moderator.getId());
      st2.setString(3, reason);
     
      ResultSet lockResult = lock.executeQuery(); // lock another delete on this row
      while (lockResult.next()) {
        int mid = lockResult.getInt("id");
        st1.setInt(1,mid);
        st2.setInt(1,mid);
        st1.executeUpdate();
        st2.executeUpdate();
      }
      st1.close();
      st2.close();
      lockResult.close();
      lock.close(); 

      // Delete user comments
      deleter = new CommentDeleter(db);

      st = db.prepareStatement("SELECT id FROM comments WHERE postip=?::inet AND not deleted AND postdate>? ORDER BY id DESC FOR update");
      st.setString(1,ip);
      st.setTimestamp(2, ts);

      rs = st.executeQuery();

      while(rs.next()) {
        int msgid = rs.getInt("id");
        output.append("Удаляется #").append(msgid).append("<br>");

        output.append(deleter.deleteReplys(msgid, moderator, true));
        output.append(deleter.deleteComment(msgid, reason, moderator, -20));

        output.append("<br>");
      }
    } finally {
      if (deleter!=null) {
        deleter.close();
      }
      
      if (rs!=null) {
        rs.close();
      }
      
      if (st!=null) {
        st.close();
      }
    }
    
    out.println(output.toString());

    db.commit();
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>

