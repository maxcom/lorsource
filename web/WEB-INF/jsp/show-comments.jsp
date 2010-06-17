<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp"   buffer="60kb" %>
<%@ page import="ru.org.linux.site.LorDataSource"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<%--@elvariable id="firstPage" type="Boolean"--%>
<%--@elvariable id="user" type="ru.org.linux.site.User"--%>
<%--@elvariable id="topics" type="Integer"--%>
<%--@elvariable id="offset" type="Integer"--%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  User user=(User) request.getAttribute("user");
  String nick = user.getNick();


  int offset = (Integer) request.getAttribute("offset");
  int topics = (Integer) request.getAttribute("topics");
  String res = (String) request.getAttribute("list");
%>
<c:if test="${firstPage}">
  <title>Последние ${topics} комментариев пользователя ${user.nick}</title>
</c:if>
<c:if test="${not firstPage}">
  <title>Последние ${offset + topics} - ${offset} комментариев пользователя ${user.nick}</title>
</c:if>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<c:if test="${firstPage}">
  <h1>Последние ${topics} комментариев пользователя ${user.nick}</h1>
</c:if>
<c:if test="${not firstPage}">
  <h1>Последние ${offset + topics} - ${offset} комментариев пользователя ${user.nick}</h1>
</c:if>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th colspan=5>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>  
</div>
<div style="float: right">
    <%
  if (res!=null && !"".equals(res)) {
	out.print("<a rel=next rev=prev href=\"show-comments.jsp?nick=" + nick + "&amp;offset=" + (offset + topics) + "\">вперед →</a>");
  }
%>
</div>
</th></tr>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%

  out.print(res);

%>
</tbody>
<tfoot>
  <tr><td colspan=5><p>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>  
</div>
<div style="float: right">
    <%
  if (res!=null && !"".equals(res)) {
	out.print("<a rel=next rev=prev href=\"show-comments.jsp?nick=" + nick + "&amp;offset=" + (offset + topics) + "\">вперед →</a>");
  }
%>
</div>
  </td></tr>
</tfoot>
</table>
</div>

<% if (Template.isSessionAuthorized(session) && (tmpl.isModeratorSession() || nick.equals(session.getValue("nick")))) { %>

<h2>Последние 20 удаленных модераторами комментариев</h2>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Причина удаления</th><th>Дата</th></tr>
<tbody>
<%
	Connection db = null;

	try {
      db = LorDataSource.getConnection();

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, deldate FROM sections, groups, topics, comments, del_info WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND del_info.msgid=comments.id AND comments.userid="+user.getId()+" AND del_info.delby!="+user.getId()+" ORDER BY del_info.delDate DESC NULLS LAST, del_info.msgid DESC LIMIT 20;");
  while (rs.next()) {
%>
  <tr>
    <td><%= rs.getString("ptitle") %></td>
    <td><%= rs.getString("gtitle") %></td>
    <td>
      <%
      out.print("<a href=\"view-message.jsp?msgid=" + rs.getInt("msgid") + "\" rev=contents>" + StringUtil.makeTitle(rs.getString("title")) + "</a>");
      %>
    </td>
    <td><%= rs.getString("reason") %></td>
    <td>
<%
  Timestamp delDate = rs.getTimestamp("deldate");
  if (delDate!=null) {
%>
      <lor:dateinterval date="<%= delDate %>"/>
<%
  }
%>
    </td>
  </tr>
<%  }

  rs.close();
  st.close();

%>

</table>
</div>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
