<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<LINK REL="stylesheet" TYPE="text/css" HREF="/tango/combined.css">
<LINK REL="stylesheet" TYPE="text/css" ID="theme" HREF="/tango/tango-dark.css">
<c:if test="${template.style == 'tango'}">
  <script type="text/javascript">
	function readCookie(name) {
		var nameEQ = name + "=";
		var ca = document.cookie.split(';');
		for(var i=0;i < ca.length;i++) {
			var c = ca[i];
			while (c.charAt(0)==' ') c = c.substring(1,c.length);
			if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
		}
		return null;
	}
    var isCookie = readCookie("style_selected");
    if(isCookie){
        var newStyle = "/tango/" + isCookie + ".css"
        $("link[id=theme]").attr("href",newStyle);
    }
  </script>
  </c:if>
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
<meta name = "viewport" content = "initial-scale=1.0">
</head>
<body>
  	<div id="hd">
        <a id="sitetitle" href="/">LINUX.ORG.RU</a>

        <ul class="menu">
          <li id="loginGreating">
            <c:if test="${template.sessionAuthorized}">
              <c:url var="userUrl" value="/people/${template.nick}/profile"/>
              <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
              <a href="logout.jsp?sessionId=<%= session.getId() %>" title="Выйти">
                <img style="position: relative; bottom: -2px; border: 0" src="/img/logout.png" width="16" height="16"
                     alt="[x]">
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
          </li>

          <li><a href="/news/">Новости</a></li>
          <li><a href="/gallery/">Галерея</a></li>

          <li><a href="/forum/">Форум</a></li>
          <li><a href="/tracker.jsp">Трекер</a></li>
          <li><a href="/wiki/">Wiki</a></li>
          <c:if test="${template.sessionAuthorized}">
            <li>
              <lor:events/>
            </li>
          </c:if>

          <li><a href="/search.jsp">Поиск</a></li>

        </ul>
      </div>
      <div style="clear: both"></div>
