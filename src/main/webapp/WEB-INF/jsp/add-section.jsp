<%@ page session="false" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2026 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<c:choose>
<c:when test="${not empty section}">
<title>${section.title}: добавление</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Добавить в «${section.title}»</h1>

<c:if test="${addportal!=null}">
  ${addportal}
  <h2>Выберите группу</h2>
</c:if>

Доступные группы:
<ul>
<c:forEach var="choice" items="${groups}">
  <li>
    <c:choose>
    <c:when test="${choice.postable}">
      <a href="${choice.addUrl}">${choice.group.title}</a>
      <c:if test="${choice.group.info != null}">
        - <em><c:out value="${choice.group.info}" escapeXml="false"/></em>
      </c:if>
    </c:when>
    <c:otherwise>
      &#128274; ${choice.group.title} - <c:out value="${choice.postScoreInfo}" escapeXml="false"/>
    </c:otherwise>
    </c:choose>
    (<a href="${choice.group.url}">просмотр...</a>)
  </li>
</c:forEach>
</ul>
</c:when>
<c:otherwise>
<title>Добавить сообщение</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Добавить сообщение</h1>

<p>Выберите раздел:</p>
<ul>
<c:forEach var="choice" items="${sectionList}">
  <li><c:choose><c:when test="${choice.postable}"><a href="${choice.url}">${choice.section.title}</a></c:when><c:otherwise>&#128274; ${choice.section.title} (<c:out value="${choice.postScoreInfo}" escapeXml="false"/>)</c:otherwise></c:choose></li>
</c:forEach>
</ul>
</c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
