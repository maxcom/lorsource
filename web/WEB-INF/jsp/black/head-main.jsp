<%@ page contentType="text/html; charset=utf-8"%>
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

<!-- head-main -->
<LINK REL=STYLESHEET TYPE="text/css" HREF="/black/combined.css" TITLE="Normal">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
</head>
<body style="margin-top: 0">
<a href="/">
<img style="float: left; border: 0" src="/black/lorlogo-try.png" alt="Русская информация об ОС LINUX" width="270" height="208">
</a>
<div id="head-main">
<table>
<tr>
  <td><a href="/news/">Новости</a></td>
  <td><a href="/tracker.jsp">Трекер</a></td>
  <td><a href="server.jsp">О сервере</a></td>
</tr>
<tr>
  <td><a href="/gallery/">Галерея</a></td>
  <td><a href="/forum/">Форум</a></td>
  <td><a href="/wiki">Wiki</a></td>
</tr>
<tr>
  <td></td>
  <td></td>
  <td><a href="search.jsp">Поиск</a></td>
</tr>
</table>
  <br>
  <%--
  <div>
    <div style="border: 1px solid #777; padding: 25px; background-color: #000040; margin: 10px 0px 10px 0px">DEVCONF 17 мая : <a href="http://devconf.ru/phpconf/page/programm/" target=_blank>PHP</a> | <a href="http://devconf.ru/python/page/programm/" target=_blank>Python</a> | <a href="http://devconf.ru/perl/page/programm/" target=_blank>Perl</a> | <a href="http://devconf.ru/ruby/page/programm/" target=_blank>Ruby</a> | <a href="http://devconf.ru/asp.net/page/programm/" target=_blank>.NET</a> | <a href="http://devconf.ru/richclient/page/programm/" target=_blank>RichClient</a> и многое другое...</div>
  </div>

  <a href="http://devconf.ru/register/">
    <img src="/adv/devconf2010.gif" alt="DevConf 2010" width="468" height="60">
  </a>
--%>
  <div class="infoblock" style="margin:0; text-align: justify">
    <a href="http://job.samsung.ru/"><img width="130" height="43" src="/adv/Samsung_Logo.png" alt="" style="float: left; border: 0"></a>
    <div style="margin-left: 135px">
    <h2>Вакансии</h2>
    Samsung Electronics приглашает разработчиков LINUX на работу в R&D центры в Южной Корее (г. Сувон)<br>
    Область специализации: Linux Kernel, TV Server SW, Open OS, Mobile Platform SW, Service
    Platform, Convergence, SW Platform, Multicore SW, System SW Tool.
    <a href="http://job.samsung.ru/">http://job.samsung.ru</a>
    </div>
  </div>
</div>

<div style="right: 5px; text-align: right; top: 5px; position: absolute" class="head">
<c:if test="${template.sessionAuthorized}">
  <c:url var="userUrl" value="/people/${template.nick}/profile"/>
  добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
  [<a href="logout.jsp?sessionId=<%= session.getId() %>" title="Выйти">x</a>]
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


<div style="clear: both"></div>
