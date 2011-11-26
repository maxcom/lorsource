<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<!-- head-main -->
<LINK REL="stylesheet" TYPE="text/css" HREF="/tango/combined.css">
<LINK REL="stylesheet" TYPE="text/css" HREF="/tango/tango-dark.css" TITLE="dark">
<LINK REL="alternate stylesheet" TYPE="text/css" HREF="/tango/tango-swamp.css" TITLE="swamp">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
<script type="text/javascript">
  $.stylesheetInit();
</script>

</head>
<body>
<div id="doc3" class="yui-t5">
  	<div id="hd">
        <div id="loginGreating" class="head">
        <c:if test="${template.sessionAuthorized}">
          <c:url var="userUrl" value="/people/${template.nick}/profile"/>
          добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
          <a href="logout.jsp?sessionId=<%= session.getId() %>" title="Выйти">
          <img style="position: relative; bottom: -2px; border: 0" src="/img/logout.png" width="16" height="16" alt="[x]">
          </a>
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
        </div>

        <h1><a id="sitetitle" href="/">LINUX.ORG.RU&nbsp;&#8212; Русская информация об&nbsp;ОС&nbsp;Linux</a></h1>

        <div class="menu">
            <ul class="primary">
                <li class="first"><a href="/news/">Новости</a></li>
                <li><a href="/gallery/">Галерея</a></li>

                <li><a href="/forum/">Форум</a></li>
                <li><a href="/tracker.jsp">Трекер</a></li>
                <li><a href="/wiki/">Wiki</a></li>
                <li class="last"><a href="/search.jsp">Поиск</a></li>
            </ul>
            <ul class="secondary">
                <li class="first"><a href="/wiki/en/Linux">O linux</a></li>
                <li><a href="/wiki/en/%D0%94%D0%B8%D1%81%D1%82%D1%80%D0%B8%D0%B1%D1%83%D1%82%D0%B8%D0%B2%D1%8B">Дистрибутивы</a></li>

                <c:if test="${template.sessionAuthorized}">
                  <li>
                    <lor:events/>
                  </li>
                </c:if>

                <li class="last"><a href="/server.jsp">О сервере</a></li>
            </ul>
        </div>
    </div>



<div style="clear: both"></div>
