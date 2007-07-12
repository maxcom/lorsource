<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.PreparedStatement,java.sql.Timestamp,java.util.Calendar,java.util.Date, javax.servlet.http.HttpServletResponse" errorPage="error.jsp" buffer="60kb" %>
<%@ page import="ru.org.linux.site.IPBlockInfo"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>banip</title>
<%= tmpl.DocumentHeader() %>
<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  String ip = tmpl.getParameters().getIP("ip");
  String reason = tmpl.getParameters().getString("reason");
  String time = tmpl.getParameters().getString("time");

  Calendar calendar = Calendar.getInstance();
  calendar.setTime(new Date());

  if ("hour".equals(time)) {
    calendar.add(Calendar.HOUR_OF_DAY, 1);
  } else if ("day".equals(time)) {
    calendar.add(Calendar.DAY_OF_MONTH, 1);
  } else if ("month".equals(time)) {
    calendar.add(Calendar.MONTH, 1);
  } else if ("3month".equals(time)) {
    calendar.add(Calendar.MONTH, 3);
  } else if ("6month".equals(time)) {
    calendar.add(Calendar.MONTH, 6);
  } else if ("remove".equals(time)) {
  }

  Timestamp ts;
  if ("unlim".equals(time)) {
    ts = null;
  } else {
    ts = new Timestamp(calendar.getTimeInMillis());
  }

  Connection db = null;
  try {
    db = tmpl.getConnection("sameip");
    db.setAutoCommit(false);

    User user = new User(db, (String) session.getAttribute("nick"));

    IPBlockInfo blockInfo = IPBlockInfo.getBlockInfo(db, ip);
    user.checkCommit();

    PreparedStatement pst;

    if (blockInfo == null) {
      pst = db.prepareStatement("INSERT INTO b_ips (ip, mod_id, date, reason, ban_date) VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?)");
    } else {
      pst = db.prepareStatement("UPDATE b_ips SET ip=?::inet, mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=? WHERE ip=?::inet");
      pst.setString(5, ip);
    }

    pst.setString(1, ip);
    pst.setInt(2, user.getId());
    pst.setString(3, reason);
    pst.setTimestamp(4, ts);

    pst.executeUpdate();

    db.commit();

    response.setHeader("Location", tmpl.getRedirectUrl() + "sameip.jsp?ip=" + URLEncoder.encode(ip));
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
