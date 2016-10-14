<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%--
  ~ Copyright 1998-2016 Linux.org.ru
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
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="items" type="java.util.List<ru.org.linux.topic.ArchiveDao.ArchiveStats>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>
${section.name}
  <c:if test="${group!=null}">
    - ${group.title}
  </c:if>
  - Архив
</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<H1>
${section.name}
  <c:if test="${group!=null}">
    «${group.title}»
  </c:if>
  - Архив
</H1>

<nav>
  <c:if test="${group == null}">
    <a class="btn btn-default" href="${section.sectionLink}">Новые темы</a>
  </c:if>
  <c:if test="${group != null}">
    <a class="btn btn-default" href="${group.url}">Новые темы</a>
  </c:if>

  <c:if test="${section.premoderated}">
    <a class="btn btn-default" href="/view-all.jsp?section=${section.id}">Неподтвержденные</a>
  </c:if>

  <c:if test="${group == null}">
    <a class="btn btn-selected" href="${section.archiveLink}">Архив</a>
  </c:if>
  <c:if test="${group != null}">
    <a class="btn btn-selected" href="${group.url}archive/">Архив</a>
  </c:if>

  <c:choose>
    <c:when test="${section.pollPostAllowed}">
      <a class="btn btn-primary" href="add.jsp?group=19387">Добавить</a>
    </c:when>
    <c:when test="${group == null}">
      <a class="btn btn-primary" href="add-section.jsp?section=${section.id}">Добавить</a>
    </c:when>
    <c:otherwise>
      <a class="btn btn-primary" href="add.jsp?group=${group.id}">Добавить</a>
    </c:otherwise>
  </c:choose>
</nav>

<div class="infoblock">
  <form method="GET" commandName="query" action="search.jsp">
    <div class="control-group">
      <input name="q" type="search" size="50" maxlength="250" placeholder="Поиск"/>&nbsp;
      <button type="submit" class="btn btn-default btn-small">Поиск</button>
    </div>

    <div class="control-group">
      <select name="range">
        <option value="ALL">включая комментарии</option>
        <option value="TOPICS">без комментариев</option>
      </select>
    </div>

    <input type="hidden" name="section" value="${section.urlName}"/>
    <c:if test="${group!=null}">
      <input type="hidden" name="group" value="${group.urlName}"/>
    </c:if>
  </form>
</div>

<c:forEach items="${items}" var="item">
  <c:url value="${item.link}" var="item_url"/>
  <fmt:parseDate var="item_date" value="${item.year} ${item.month}" pattern="yyyy M"/>
  <a href="${fn:escapeXml(item_url)}">${l:getMonthName(item.month)}&nbsp;${item.year} года  (${item.count})</a> <br/>
</c:forEach>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
