<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<body>
<table border="0" cellspacing="0" cellpadding="0" width="100%" class="head">
<tr>
        <td rowspan="2" align=left><a href="/"><img src="/black/lor-new.png" width=282 height=60 alt="Linux.org.ru"></a></td>
        <td align="right">
          <c:if test="${template.sessionAuthorized}">
            <c:url var="userUrl" value="/people/${template.nick}/profile"/>
            добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
          </c:if>

          <c:if test="${not template.sessionAuthorized}">
            <div id="regmenu">
              <a style="text-decoration: none" href="/register.jsp">Регистрация</a> -
              <a style="text-decoration: none" href="/login.jsp" id="loginbutton">Вход</a>
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
  <tr>
        <td align=right valign=bottom>
                <a style="text-decoration: none" href="/news/">Новости</a> -
                <a style="text-decoration: none" href="/gallery/">Галерея</a> -
                <a style="text-decoration: none" href="/forum/">Форум</a> -
                <c:if test="${template.sessionAuthorized}">
                    <lor:events/> - 
                </c:if>
                <a style="text-decoration: none" href="/tracker/">Трекер</a> -
                <a style="text-decoration: none" href="/wiki">Wiki</a> -
                <a style="text-decoration: none" href="search.jsp">Поиск</a>
        </td>
</tr>
</table>

