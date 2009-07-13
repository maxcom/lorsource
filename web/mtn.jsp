<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.LorDataSource,ru.org.linux.site.Template"   buffer="60kb" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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
<%
  Template tmpl = (Template) request.getAttribute("template");

  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }
  int msgid = new ServletParameterParser(request).getInt("msgid");
  %>
<title>Перенос новости</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
перенос новости <strong><%= msgid %></strong> в группу:
<%
  Connection db = null;

  try {
    db = LorDataSource.getConnection();
    Statement st1 = db.createStatement();
    ResultSet rs = st1.executeQuery("SELECT id,title FROM groups WHERE section=1 ORDER BY id");
%>
<form method="post" action="mt.jsp">
<input type=hidden name="msgid" value='<%= msgid %>'>
<select name="moveto">
<%
        while (rs.next()) {
          out.println("<option value='"+rs.getInt("id")+"'>"+rs.getString("title")+"</option>");
        }

        rs.close();
        st1.close();
 %>
</select>
<input type='submit' name='move' value='move'>
</form>
  <%
    } finally {
        if (db!=null) {
          db.close();
        }
    }
%>

  <jsp:include page="/WEB-INF/jsp/footer.jsp"/>
