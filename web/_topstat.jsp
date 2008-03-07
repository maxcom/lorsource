<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement"   %>
<%@ page import="ru.org.linux.site.LorDataSource"%>
<%@ page import="ru.org.linux.site.Template" %>
<% Template tmpl = new Template(request, config.getServletContext(), response); %>
<%= tmpl.getHead() %>
<title>we make new db information for mess_priority</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%  Connection db=null;
    try { %>
<%
  db = LorDataSource.getConnection();
  db.setAutoCommit(false);
  Statement stmtFrom = db.createStatement();
  Statement stmtDel = db.createStatement();
  PreparedStatement stmtTo = db.prepareStatement("INSERT INTO top10 (msgid, mess_order) VALUES (?,?)");
  stmtDel.executeUpdate("DELETE FROM top10");
  ResultSet rs = stmtFrom.executeQuery("select topics.id as msgid, topics.title, lastmod, stat1 as c from topics where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and not deleted and notop is null and groupid!=8404 and groupid!=4068 order by c desc, msgid limit 10");

  int c = 0;
  while (rs.next()) {
    c++;
    int msgid = rs.getInt("msgid");
    stmtTo.setInt(1, msgid);
    stmtTo.setInt(2, c);
    stmtTo.executeUpdate();
  }
  rs.close();
  stmtFrom.close();
  stmtDel.close();
  stmtTo.close();
  db.commit();
%>

<% } finally {
  if (db!=null) {
    db.close();
  }
} %>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
