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

<LINK REL=STYLESHEET TYPE="text/css" HREF="/white2/combined.css" TITLE="Normal">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
</head>
<body bgcolor="#ffffff" text="#000000" link="#0000ee" vlink="#551a8b" ALINK="#ff0000">

<!-- #defefc #aea6f2 -->

<table width="100%" border=0 cellpadding=0 cellspacing=0>
<tr><td bgcolor="#defefc"><img src="/white2/h1.png" alt="Русская информация об ОС Linux" width=452 height=72></td><td bgcolor="#defefc" align=right valign=top><span class=head></span>
</td>
  <td align="right" valign="top" bgcolor="#defefc">
    <c:if test="${template.sessionAuthorized}">
      <c:url var="userUrl" value="/people/${template.nick}/profile"/>
  добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
  [<a href="logout.jsp?sessionId=<%= session.getId() %>" title="Выйти">x</a>]
  <br>
</c:if>

<c:if test="${not template.sessionAuthorized}">
  <div id="regmenu" class="head">
    <a href="/register.jsp">Регистрация</a> -
    <a href="/login.jsp" id="loginbutton">Вход</a>
    <br>
    <img src="/black/pingvin.gif" alt="Linux Logo" height=114 width=102>
  </div>

  <form method=POST action="login.jsp" style="display: none" id="regform">
    Имя: <input type=text name=nick size=15><br>
    Пароль: <input type=password name=passwd size=15><br>
    <input type=submit value="Вход">
    <input type="button" value="Отмена" id="hide_logginbutton">
  </form>
</c:if>
</td>
</tr>
<tr><td><img src="/white2/h2.png" alt="" width=452 height=49></td></tr>
</table>

<center>
<table width="80%">
<tr>
<td valign="top">
<a href="/">Новости</a><br>
<a href="/wiki/">Wiki</a><br>
<a href="server.jsp">О Сервере</a><br>
</td><td valign="top">
<a href="/wiki/en/%D0%94%D0%B8%D1%81%D1%82%D1%80%D0%B8%D0%B1%D1%83%D1%82%D0%B8%D0%B2%D1%8B">Дистрибутивы</a><br>
<a href="/books">Документация</a><br>
</td><td valign="top">
<a href="/gallery/">Галерея</a><br>
<a href="/forum/">Форум</a><br>
<a href="search.jsp">Поиск</a><br>
</td>
</tr>
</table>
</center>
<div style="margin-bottom: 1em"></div>
