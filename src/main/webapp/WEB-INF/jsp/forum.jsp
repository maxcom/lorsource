<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2023 Linux.org.ru
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${section.name}</title>
<link rel="parent" title="Linux.org.ru" href="/">
<link rel="alternate" href="/section-rss.jsp?section=${section.id}" type="application/rss+xml">
<meta name="format-detection" content="telephone=no">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Технический форум</h1>

<ul>
  <c:forEach var="group" items="${tech}">
    <li>
      <a class="navLink" href="${group.url}">${group.title}</a>

      (${group.stat3} за сутки)

      <c:if test="${group.info != null}">
        — <em><c:out value="${group.info}" escapeXml="false"/></em>
      </c:if>

    </li>

  </c:forEach>
</ul>

<h1>Остальное</h1>

<ul>
  <c:forEach var="group" items="${other}">
    <li>
      <a class="navLink" href="${group.url}">${group.title}</a>

      (${group.stat3} за сутки)

      <c:if test="${group.info != null}">
        — <em><c:out value="${group.info}" escapeXml="false"/></em>
      </c:if>

    </li>

  </c:forEach>
</ul>

<h1>Лента форума</h1>

<p>
  Все разделы форума также доступны в виде единой
  <a class="navLink" href="/forum/lenta/">ленты</a>.
</p>
<h1>RSS подписки</h1>

<p>
  <i class="icon-rss"></i><a href="section-rss.jsp?section=${section.id}">RSS-подписка на весь форум</a><br>
  <i class="icon-rss"></i><a href="section-rss.jsp?section=${section.id}&filter=tech">RSS-подписка на технические форум</a><br>
  <i class="icon-rss"></i><a href="section-rss.jsp?section=${section.id}&filter=notalks">RSS-подписка на форум без Talks</a>
</p>

<p>Отдельные разделы форума также имеют RSS подписки, ссылки на них расположены на страницах разделов.</p>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
