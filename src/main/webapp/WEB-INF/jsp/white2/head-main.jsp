<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
<meta name = "viewport" content = "initial-scale=1.0">
</head>
<body>
  	<div id="hd">
  	    <div id="hdtux">
  	        <img src="/img/Tux.svg" height="100%" />
  	    </div>
        <a id="sitetitle" href="/">LINUX.ORG.RU</a>
        <ul class="menu">
          <li id="loginGreating">
            <c:if test="${template.sessionAuthorized}">
              добро пожаловать,&nbsp;
              <c:url var="userUrl" value="/people/${template.nick}/profile"/>
              <a style="text-decoration: none" href="${userUrl}">anonymous</a>
            </c:if>

            <c:if test="${not template.sessionAuthorized}">
              <div id="regmenu" class="head">
                <a href="/register.jsp">Регистрация</a> -
                <a id="loginbutton" href="/login.jsp">Вход</a>
              </div>

              <form method=POST action="login.jsp" style="display: none" id="regform">
                <label>Имя: <input type=text name=nick size=15></label><br>
                <label>Пароль: <input type=password name=passwd size=15></label><br>
                <input type=submit value="Вход">
                <input id="hide_loginbutton" type="button" value="Отмена">
              </form>
            </c:if>
          </li>

          <li><a href="/news/">Новости</a></li>
          <li><a href="/gallery/">Галерея</a></li>

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
      <div style="clear: both"></div>
