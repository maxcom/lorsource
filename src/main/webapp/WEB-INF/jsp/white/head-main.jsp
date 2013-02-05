<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
</head>
<body bgcolor="#ffffff" text="#000000" link="#0000ee" vlink="#551a8b" ALINK="#ff0000">

<c:if test="${template.sessionAuthorized}">
<div style="float: right">
  [<a href="/logout?sessionId=<%= session.getId() %>">выйти</a>]
</div>
</c:if>

<div align="center">
<img src="/white/linux_main.gif" width=469 height=81 alt="Русская Информация об ОС Linux">
</div>


<div align="center">
<table width="80%">
<tr>
<td valign="top">
<a href="/">Новости</a><br>
<a href="/wiki/">Wiki</a><br>
<a href="/about">О Сервере</a><br>
</td><td valign="top">
<a href="/wiki/en/%D0%94%D0%B8%D1%81%D1%82%D1%80%D0%B8%D0%B1%D1%83%D1%82%D0%B8%D0%B2%D1%8B">Дистрибутивы</a><br>
<a href="/tracker.jsp">Трекер</a><br>
</td><td valign="top">
<a href="/gallery/">Галерея</a><br>
<a href="/forum/">Форум</a><br>
<a href="search.jsp">Поиск</a><br>
</td>
</tr>
</table>
</div>

<div style="margin-bottom: 1em"></div>

