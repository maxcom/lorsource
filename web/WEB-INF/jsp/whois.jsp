<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement"   buffer="60kb" %>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Timestamp"%>
<%@ page import="java.util.Map"%>
<%@ page import="ru.org.linux.site.IgnoreList" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

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
<%--@elvariable id="user" type="ru.org.linux.site.User"--%>
<%--@elvariable id="userInfo" type="ru.org.linux.site.UserInfo"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="moderatorOrCurrentUser" type="java.lang.Boolean"--%>

<% Template tmpl = Template.getTemplate(request); %>
<%
  response.setDateHeader("Expires", System.currentTimeMillis()+120000);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  User user = (User) request.getAttribute("user");
  String nick = user.getNick();
%>
<title>Информация о пользователе ${user.nick}</title>
<c:if test="${userInfo.url != null}">
  <link rel="me" href="${fn:escapeXml(userInfo.url)}">
</c:if>
<LINK REL="alternate" HREF="/people/${user.nick}/?output=rss" TYPE="application/rss+xml">

<jsp:include page="header.jsp"/>

<% Connection db = null;
  try {
    db = LorDataSource.getConnection();

%>

<h1>Информация о пользователе <%= nick %></h1>
<%
  PreparedStatement stat1 = db.prepareStatement("SELECT count(*) as c FROM comments WHERE userid=?");
  PreparedStatement stat2 = db.prepareStatement("SELECT sections.name as pname, count(*) as c FROM topics, groups, sections WHERE topics.userid=? AND groups.id=topics.groupid AND sections.id=groups.section AND not deleted GROUP BY sections.name");
  PreparedStatement stat3 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM topics WHERE topics.userid=?");
  PreparedStatement stat4 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?");
  PreparedStatement stat5 = db.prepareStatement("SELECT count(*) as inum FROM ignore_list WHERE ignored=?");

  int userid = user.getId();

  stat1.setInt(1, userid);
  stat2.setInt(1, userid);
  stat3.setInt(1, userid);
  stat4.setInt(1, userid);
  stat5.setInt(1, userid);
%>
<div id="whois_userpic">
  <lor:userpic author="${user}"/>
    <div style="clear: both">
  </div>
<c:if test="${user.photo !=null && moderatorOrCurrentUser}">
  <p><form style="text-align: center" name='f_remove_userpic' method='post' action='remove-userpic.jsp'>
  <input type='hidden' name='id' value='${user.id}'>
  <input type='submit' value='Удалить'>
  </form>
</c:if>

</div>
<div>
<h2>Регистрация</h2>
<b>ID:</b> <%= userid %><br>
<b>Nick:</b> <%= nick %><br>
<%
   String fullname=user.getName();

   if (fullname!=null) {
     if (!"".equals(fullname)) {
       out.println("<b>Полное имя:</b> " + fullname + "<br>");
     }
   }
%>
  <c:if test="${userInfo.url != null}">
    <b>URL:</b> <a href="${fn:escapeXml(userInfo.url)}">${fn:escapeXml(userInfo.url)}</a><br>
  </c:if>
  <c:if test="${userInfo.town != null}">
    <b>Город:</b> <c:out value="${userInfo.town}" escapeXml="true"/><br>
  </c:if>
  <c:if test="${userInfo.registrationDate != null}">
    <b>Дата регистрации:</b> <lor:date date="${userInfo.registrationDate}"/><br>
  </c:if>
  <c:if test="${userInfo.lastLogin != null}">
    <b>Последнее посещение:</b> <lor:date date="${userInfo.lastLogin}"/><br>
  </c:if>

<b>Статус:</b> <%= user.getStatus() %><%
  if (user.canModerate()) {
    out.print(" (модератор)");
  }

  if (user.isCorrector()) {
    out.print(" (корректор)");
  }

  if (user.isBlocked()) {
    out.println(" (заблокирован)\n");
  }
%>
  <br>
  <c:if test="${moderatorOrCurrentUser}">
    <c:if test="${user.email!=null}">
      <b>Email:</b> ${user.email}<br>
      <b>Score:</b> ${user.score}<br>
    <%
      ResultSet rs = stat5.executeQuery();
      rs.next();
      out.println("<b>Игнорируется</b>: " + rs.getInt("inum") + "<br>\n");
      rs.close();
    %>
    </c:if>
  </c:if>
<%
  if (Template.isSessionAuthorized(session) && !session.getValue("nick").equals(nick) && !"anonymous".equals(session.getValue("nick"))) {
    out.println("<br>");
    Map<Integer,String> ignoreList = IgnoreList.getIgnoreList(db, (String) session.getValue("nick"));
    if (ignoreList != null && !ignoreList.isEmpty() && ignoreList.containsValue(nick)) {
      out.print("<form name='i_unblock' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='id' value='" + userid + "'>\n");
      out.print("Вы игнорируете этого пользователя &nbsp; \n");
      out.print("<input type='submit' name='del' value='не игнорировать'>\n");
      out.print("</form>");
    } else {
      out.print("<form name='i_block' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='nick' value='" + nick + "'>\n");
      out.print("Вы не игнорируете этого пользователя &nbsp; \n");
      out.print("<input type='submit' name='add' value='игнорировать'>\n");
      out.print("</form>");
    }
  }
%>
  <br>
  <c:if test="${template.moderatorSession and user.blockable}">
    <form method='post' action='usermod.jsp'>
      <input type='hidden' name='id' value='${user.id}'>
      <c:if test="${user.blocked}">
        <input type='submit' name='action' value='unblock'>
      </c:if>
      <c:if test="${not user.blocked}">
        <input type='submit' name='action' value='block'>
        <input type='submit' name='action' value='block-n-delete-comments'>
      </c:if>
    </form>
  </c:if>
<br>
<p>
<cite>
<%
  out.print(HTMLFormatter.nl2br(user.getUserinfo(db)));
%>
  </cite>
  <c:if test="${template.moderatorSession}">

  <p>

  <form name='f_remove_userinfo' method='post' action='usermod.jsp'>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='remove_userinfo'>
    <input type='submit' value='Удалить текст'>
  </form>

  <p>

    <c:if test="<%= user.isCorrector() || user.getScore() > User.CORRECTOR_SCORE %>">
  <form name='f_toggle_corrector' method='post' action='usermod.jsp'>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='toggle_corrector'>
    <%
      out.print("<input type='submit' value='" + (user.isCorrector() ? "Убрать права корректора" : "Сделать корректором") + "'>\n");
    %>
  </form>
  </c:if>
  </c:if>
  <%
  if (Template.isSessionAuthorized(session) && (session.getValue("nick").equals(nick))) {
    out.print("<p><a href=\"register.jsp?mode=change\">Изменить регистрацию</a>.");
  }
%>

<h2>Статистика</h2>
<% ResultSet rs=stat3.executeQuery(); rs.next();
  Timestamp first = rs.getTimestamp("first");
  Timestamp last = rs.getTimestamp("last");
 %>
<b>Первая созданная тема:</b> <%= first==null?"нет":tmpl.dateFormat.format(first) %><br>
<b>Последняя созданная тема:</b> <%= last==null?"нет":tmpl.dateFormat.format(last) %><br>
<% rs.close(); %>
<% rs=stat4.executeQuery(); rs.next();
  Timestamp firstComment = rs.getTimestamp("first");
  Timestamp lastComment = rs.getTimestamp("last");
%>
<b>Первый комментарий:</b> <%= firstComment==null?"нет":tmpl.dateFormat.format(firstComment) %><br>
<b>Последний комментарий:</b> <%= lastComment==null?"нет":tmpl.dateFormat.format(lastComment) %>
<% rs.close(); %>
<p>

  <c:if test="${user.id!=2}">

<div class="forum">
<table class="message-table">
<thead>
<tr><th>Раздел</th><th>Число сообщений (тем)</th></tr>
<tbody>
<% rs=stat2.executeQuery(); %>
<%
   while (rs.next()) {
   	out.print("<tr><td>"+rs.getString("pname")+"</td><td>"+rs.getInt("c")+"</td></tr>");
   }
%>
<% rs.close(); %>
<%
rs=stat1.executeQuery();
rs.next(); %>
<tr><td>Комментарии</td><td valign='top'><%= rs.getInt("c") %></td></tr>
</table>
</div>

<h2>Сообщения пользователя</h2>
<ul>
  <li>
    <a href="/people/${user.nick}/">Темы</a>
  </li>

  <li>
    <a href="show-comments.jsp?nick=<%= nick %>">Комментарии</a>
  </li>
<c:if test="${moderatorOrCurrentUser}">
  <li>
    <a href="show-replies.jsp?nick=<%= nick %>">Ответы на комментарии</a>
  </li>
</c:if>
</ul>
</c:if>

</div>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="footer.jsp"/>
