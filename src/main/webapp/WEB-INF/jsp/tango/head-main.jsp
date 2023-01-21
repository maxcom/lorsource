<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
<meta name="viewport" content="initial-scale=1.0">
</head>
<% out.flush(); %>
<body>
<div id="hd">
  <div id="topProfile">
    <c:if test="${template.sessionAuthorized}">
      <c:url var="userUrl" value="/people/${currentUser.nick}/profile"/>
      <a style="text-decoration: none" href="${userUrl}">${currentUser.nick}</a>
    </c:if>
  </div>

  <span id="sitetitle"><a href="/">LINUX.ORG.RU</a></span>

  <div class="menu">
    <div id="loginGreating">
      <c:if test="${not template.sessionAuthorized}">
        <div id="regmenu" class="head">
          <a href="${configuration.secureUrl}register.jsp">Регистрация</a> -
          <a id="loginbutton" href="${configuration.secureUrl}login.jsp">Вход</a>
        </div>

        <form method=POST action="${configuration.secureUrlWithoutSlash}/ajax_login_process" style="display: none" id="regform">
          <lor:csrf/>
          <label>Имя: <input type=text name=nick size=15 placeholder="nick или email" autocapitalize="off"></label>
          <label>Пароль: <input type=password name=passwd size=15></label>
          <input type=submit value="Вход">
          <input id="hide_loginbutton" type="button" value="Отмена">
        </form>
      </c:if>
    </div>

    <ul>
      <li><a href="/news/">Новости</a></li>
      <li><a href="/gallery/">Галерея</a></li>
      <li><a href="/articles/">Статьи</a></li>
      <li><a href="/forum/">Форум</a></li>
      <li><a href="/tracker/">Трекер</a></li>
      <c:if test="${template.sessionAuthorized}">
        <li>
          <lor:events/>
        </li>
      </c:if>

      <li><a href="/search.jsp">Поиск</a></li>
    </ul>
  </div>
</div>
<div style="clear: both"></div>
