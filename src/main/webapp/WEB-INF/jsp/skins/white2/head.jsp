<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>

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

<LINK REL=STYLESHEET TYPE="text/css" HREF="/skins/white2/combined.css" TITLE="Normal">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
</head>
<body bgcolor="#ffffff" text="#000000" link="#0000ee" vlink="#551a8b" ALINK="#ff0000">

<!-- #defefc #aea6f2 -->

<table width="100%" border=0 cellpadding=0 cellspacing=0>
<tr><td class="bluehead"><img src="/skins/white2/h1.png" alt="Русская информация об ОС Linux" width=452 height=72></td>
<td align="right" valign="top" class="bluehead">
  <c:if test="${template.sessionAuthorized}">
    <c:url var="userUrl" value="/people/${template.nick}/profile"/>
    добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
    [<a href="logout.jsp?sessionId=<%= session.getId() %>" title="Выйти">x</a>]
  </c:if>

  <c:if test="${not template.sessionAuthorized}">
    <div id="regmenu">
      <a style="text-decoration: none" href="/register.jsp">Регистрация</a> -
      <a style="text-decoration: none" href="/" id="loginbutton">Вход</a>
    </div>

    <form method=POST action="login.jsp" style="display: none" id="regform">
      Имя: <input type=text name=nick size=15>
      Пароль: <input type=password name=passwd size=15>
      <input type=submit value="Вход">
      <input type="button" value="Отмена" id="hide_loginbutton">
    </form>
  </c:if>
</td>
</tr>
<tr><td><img src="/skins/white2/h2.png" alt="" width=452 height=49></td></tr>
</table>

<div align="center">
<table width="80%">
<tr>
<td valign="top">
  [<a href="/">Новости</a>]
</td>
<td valign="top">
  [<a href="/gallery/">Галерея</a>]
</td>
<td valign="top">
  [<a href="/forum/">Форум</a>]
</td>
<td valign="top">
  [<a href="/tracker.jsp">Трекер</a>]
</td>
<td valign="top">
  [<a href="/wiki/">Wiki</a>]
</td>
<td valign="top">
  [<a href="search.jsp">Поиск</a>]
</td>
</tr>
</table>
</div>
<div style="margin-bottom: 1em"></div>

