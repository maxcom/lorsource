<%@ page session="false" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
<meta name="viewport" content="initial-scale=1.0">
</head>
<% out.flush(); %>
<body>
<header id="hd">
  <div id="topProfile">
    <c:if test="${template.sessionAuthorized}">
      <c:if test="${not disable_event_header}">
        <c:if test="${currentUser.unreadEvents > 0}">
          <a href="/notifications"> <i class="icon-bell"></i><span id="main_events_count_number" class="set">${currentUser.unreadEvents}</span></a>
        </c:if>
        <c:if test="${currentUser.unreadEvents == 0}">
          <a href="/notifications"> <i class="icon-bell"></i><span id="main_events_count_number"></span></a>
        </c:if>
        <c:url var="userUrl" value="/people/${currentUser.nick}/profile"/>
      </c:if>
      <a style="text-decoration: none" title="${fn:escapeXml(currentUser.nick)}" href="${userUrl}"><i class="icon-user-circle-o"></i></a>
    </c:if>
  </div>

  <span id="sitetitle"><a href="/">LINUX.ORG.RU</a></span>

  <nav class="menu">
    <div id="loginGreating">
      <c:if test="${not template.sessionAuthorized}">
        <div id="regmenu" class="head">
          <a href="${configuration.secureUrl}register.jsp">Регистрация</a> -
          <lor:login-link id="loginbutton">Вход</lor:login-link>
        </div>
      </c:if>
    </div>

    <ul>
      <li><a href="/news/">Новости</a></li>
      <li><a href="/gallery/">Галерея</a></li>
      <li><a href="/articles/">Статьи</a></li>
      <li><a href="/forum/">Форум</a></li>
      <li><a href="/polls/">Опросы</a></li>
      <li><a href="/tracker/">Трекер</a></li>
      <li><a href="/search.jsp">Поиск</a></li>
    </ul>
  </nav>
</header>
<div style="clear: both"></div>
