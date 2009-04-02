<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement"   %>
<%@ page import="ru.org.linux.site.LorDataSource"%>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

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
