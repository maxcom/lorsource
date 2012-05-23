<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date"   %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.group.Group>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%

  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<title>${section.name}</title>
<link rel="parent" title="Linux.org.ru" href="/">
<LINK REL="alternate" HREF="/section-rss.jsp?section=${section.id}" TYPE="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <div class=nav>
      <div id="navPath">
        <strong>${section.name}</strong>
      </div>

      <div class="nav-buttons">
        <ul>
          <li><a href="add-section.jsp?section=${section.id}">Добавить сообщение</a></li>

          <li><a href="/forum/lenta/">Лента</a></li>
        </ul>

        [<a href="section-rss.jsp?section=${section.id}">RSS</a>
            <span id="rss-select">
                <a href="section-rss.jsp?section=${section.id}&filter=notalks">без talks</a>
                <a href="section-rss.jsp?section=${section.id}&filter=tech">тех. разделы форума</a></span>]
      </div>
  </div>

<h1 class="optional">${section.name}</h1>

Группы:
<ul>

  <c:forEach var="group" items="${groups}">
    <li>
      <a class="navLink" href="${group.url}">${group.title}</a>

      (всего ${group.stat1}, сегодня ${group.stat3})

      <c:if test="${group.info != null}">
        - <em><c:out value="${group.info}" escapeXml="false"/></em>
      </c:if>

    </li>

  </c:forEach>

</ul>

<c:if test="${not template.sessionAuthorized}">
<p>Если вы еще не зарегистрировались - вам <a href="/register.jsp">сюда</a>.</p>
</c:if>
<c:if test="${template.sessionAuthorized}">
<h1>Настройки</h1>
<ul>
<li><a href="addphoto.jsp">Добавить фотографию</a>
<li><a href="/people/${template.nick}/edit">Изменение регистрации</a>
<li><a href="lostpwd.jsp">Получить забытый пароль</a>
<li><a href="/people/${template.nick}/settings">Персональные настройки сайта</a>
</ul>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
