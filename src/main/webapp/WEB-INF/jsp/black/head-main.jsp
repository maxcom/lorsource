<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
</head>
<body style="margin-top: 0">
<a href="/">
<img style="float: left; border: 0" src="/black/lorlogo-try.png" alt="Русская информация об ОС LINUX" width="270" height="208">
</a>
<div id="hd">
<div id="head-main">
<table>
<tr>
  <td><a href="/news/">Новости</a></td>
  <td><a href="/tracker/">Трекер</a></td>
  <td><a href="/about">О сервере</a></td>
</tr>
<tr>
  <td><a href="/gallery/">Галерея</a></td>
  <td><a href="/forum/">Форум</a></td>
  <td><lor:events/></td>
</tr>
<tr>
  <td></td>
  <td></td>
  <td><a href="search.jsp">Поиск</a></td>
</tr>
</table>
  <br>
</div>

<div style="right: 5px; text-align: right; top: 5px; position: absolute" class="head">
<c:if test="${template.sessionAuthorized}">
  <c:url var="userUrl" value="/people/${template.nick}/profile"/>
  добро пожаловать, <a style="text-decoration: none" href="${userUrl}">anonymous</a>
  <%--<br>--%>
  <%--<img src="/black/pingvin.gif" alt="Linux Logo" height=114 width=102>--%>
</c:if>

<c:if test="${not template.sessionAuthorized}">
  <div id="regmenu" class="head">
    <a href="/register.jsp">Регистрация</a> -
    <a id="loginbutton" href="/login.jsp">Вход</a>
    <%--<br>--%>
    <%--<img src="/black/pingvin.gif" alt="Linux Logo" height=114 width=102>--%>
  </div>

  <form method=POST action="login.jsp" style="display: none" id="regform">
    Имя: <input type=text name=nick size=15><br>
    Пароль: <input type=password name=passwd size=15><br>
    <input type=submit value="Вход">
    <input id="hide_loginbutton" type="button" value="Отмена">
  </form>
</c:if>
</div>

</div>
