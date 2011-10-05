<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Map,ru.org.linux.site.Template"   buffer="60kb" %>
<%@ page import="ru.org.linux.site.User"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="java.util.Set" %>
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
<%--@elvariable id="userStat" type="ru.org.linux.site.UserStatistics"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="moderatorOrCurrentUser" type="java.lang.Boolean"--%>
<%--@elvariable id="banInfo" type="ru.org.linux.site.BanInfo"--%>
<%--@elvariable id="ignoreList" type="java.lang.Set<Integer>"--%>

<% Template tmpl = Template.getTemplate(request); %>
<%
  response.setDateHeader("Expires", System.currentTimeMillis()+120000);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  User user = (User) request.getAttribute("user");
%>
<title>Информация о пользователе ${user.nick}</title>
<c:if test="${userInfo.url != null}">
  <c:if test="${user.score >= 100 && not user.blocked && user.activated}">
      <link rel="me" href="${fn:escapeXml(userInfo.url)}">
  </c:if>
</c:if>
<LINK REL="alternate" HREF="/people/${user.nick}/?output=rss" TYPE="application/rss+xml">

<jsp:include page="header.jsp"/>

<h1>Информация о пользователе ${user.nick}</h1>
<%
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
<div class="vcard">
<b>ID:</b> ${user.id}<br>
<b>Nick:</b> <span class="nickname">${user.nick}</span><br>
<c:if test="${user.name!=null and not empty user.name}">
  <b>Полное имя:</b> <span class="fn">${user.name}</span><br>
</c:if>

<c:if test="${userInfo.url != null and (template.sessionAuthorized or user.maxScore>=50)}">
  <b>URL:</b>

  <c:choose>
    <c:when test="${user.score < 100 || user.blocked || not user.activated}">
      <a class="url" href="${fn:escapeXml(userInfo.url)}" rel="nofollow">${fn:escapeXml(userInfo.url)}</a><br>
    </c:when>
    <c:otherwise>
      <a class="url" href="${fn:escapeXml(userInfo.url)}">${fn:escapeXml(userInfo.url)}</a><br>
    </c:otherwise>
  </c:choose>
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

  if (user.isAdministrator()) {
    out.print(" (администратор)");
  }

  if (user.isCorrector()) {
    out.print(" (корректор)");
  }

  if (user.isBlocked()) {
    out.println(" (заблокирован)\n");
  }
%>
  <br>
  <c:if test="${banInfo != null}">
    Блокирован <lor:date date="${banInfo.date}"/>, модератором <lor:user link="true" decorate="true" user="${banInfo.moderator}"/> по причине:
    <c:out escapeXml="true" value="${banInfo.reason}"/>
  </c:if>
</div>
  <c:if test="${moderatorOrCurrentUser}">
    <div>
    <c:if test="${user.email!=null}">
      <b>Email:</b> <a href="mailto:${user.email}">${user.email}</a> (виден только вам и модераторам) <br>
      <b>Score:</b> ${user.score}<br>
      <b>Игнорируется</b>: ${userStat.ignoreCount}<br>
    </c:if>
    </div>      
  </c:if>
<c:if test="${ignoreList != null}">
<%
    Set<Integer> ignoreList = (Set<Integer>) request.getAttribute("ignoreList");
    if (!ignoreList.isEmpty() && ignoreList.contains(user.getId())) {
      out.print("<form name='i_unblock' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='id' value='" + user.getId() + "'>\n");
      out.print("Вы игнорируете этого пользователя &nbsp; \n");
      out.print("<input type='submit' name='del' value='не игнорировать'>\n");
      out.print("</form>");
    } else {
      out.print("<form name='i_block' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='nick' value='" + user.getNick() + "'>\n");
      out.print("Вы не игнорируете этого пользователя &nbsp; \n");
      out.print("<input type='submit' name='add' value='игнорировать'>\n");
      out.print("</form>");
    }
%>
</c:if>
  <br>
  <c:if test="${(template.moderatorSession and user.blockable) or template.currentUser.administrator}">
    <div style="border: 1px dotted; padding: 1em;">
    <form method='post' action='usermod.jsp'>
      <input type='hidden' name='id' value='${user.id}'>
      <c:if test="${user.blocked}">
        <input type='submit' name='action' value='unblock'>
      </c:if>
      <c:if test="${not user.blocked}">
        Причина: <input type="text" name="reason"><br>
        <input type='submit' name='action' value='block'>
        <input type='submit' name='action' value='block-n-delete-comments'>
      </c:if>
    </form>
    </div>
  </c:if>
<br>
<p>
<c:if test="${template.sessionAuthorized or user.maxScore>=50}">
<cite>
  ${userInfoText}
</cite>
</c:if>

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
  if (Template.isSessionAuthorized(session) && (tmpl.getNick().equals(user.getNick()))) {
    out.print("<p><a href=\"register.jsp?mode=change\">Изменить регистрацию</a>.");
  }
%>

<h2>Статистика</h2>
<c:if test="${userStat.firstTopic != null}">
  <b>Первая созданная тема:</b> <lor:date date="${userStat.firstTopic}"/><br>
  <b>Последняя созданная тема:</b> <lor:date date="${userStat.lastTopic}"/><br>
</c:if>
<c:if test="${userStat.firstComment != null}">
  <b>Первый комментарий:</b> <lor:date date="${userStat.firstComment}"/><br>
  <b>Последний комментарий:</b> <lor:date date="${userStat.lastComment}"/><br>
</c:if>
<c:if test="${not user.anonymous}">
  <b>Число комментариев: ${userStat.commentCount}</b>
</c:if>
<p>

  <c:if test="${user.id!=2}">

<div class="forum">
<table class="message-table">
<thead>
<tr><th>Раздел</th><th>Число сообщений (тем)</th></tr>
<tbody>
<c:forEach items="${userStat.commentsBySection}" var="i">
  <tr><td>${i.key}</td><td>${i.value}</td></tr>
</c:forEach>
</table>
</div>

<h2>Сообщения пользователя</h2>
<ul>
  <li>
    <a href="/people/${user.nick}/">Темы</a>
  </li>

  <li>
    <a href="show-comments.jsp?nick=${user.nick}">Комментарии</a>
  </li>
<c:if test="${moderatorOrCurrentUser}">
  <li>
    <a href="show-replies.jsp?nick=${user.nick}">Уведомления</a>
  </li>
</c:if>
  <li>
    <a href="/people/${user.nick}/favs">Избранные темы</a>
  </li>
</ul>
</c:if>

</div>

<jsp:include page="footer.jsp"/>
