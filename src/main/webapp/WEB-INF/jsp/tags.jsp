<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date"   %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="tags" type="java.util.Map<ru.org.linux.tag.TagRef,java.lang.Integer>"--%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%
  response.setDateHeader("Expires", new Date(System.currentTimeMillis() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(System.currentTimeMillis() - 2 * 1000).getTime());
%>
<title>Список меток</title>
<link rel="parent" title="Linux.org.ru" href="/">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Список меток</h1>

<div class="tags-first-letters">
<c:forEach var="firstLetter" items="${firstLetters}" varStatus = "status">
${status.first ? '' : ', '}
  <c:choose>
    <c:when test="${firstLetter == currentLetter}">
      <span>${firstLetter}</span>
    </c:when>
    <c:otherwise>
      <c:url var="tagLetterUrl" value="/tags/${firstLetter}" />
      <a href="${tagLetterUrl}">${firstLetter}</a>
    </c:otherwise>
  </c:choose>
</c:forEach>
</div>

<ul>

  <c:forEach var="tag" items="${tags}">
    <c:if test="${tag.value != 0 || template.moderatorSession}">
      <li>
        <c:choose>
          <c:when test="${tag.value != 0 and tag.key.url.defined}">
            <c:url value="${tag.key.url.get()}" var="tag_url"/>
            <a href="${fn:escapeXml(tag_url)}">${tag.key.name}</a>
          </c:when>
          <c:otherwise>
            <span>${tag.key.name}</span>
          </c:otherwise>
        </c:choose>

        (${tag.value})
        <c:if test="${template.moderatorSession}">
          <span class="action-buttons">
              <c:url var="edit_url" value="/tags/change">
                <c:param name="firstLetter" value="${currentLetter}"/>
                <c:param name="tagName" value="${tag.key.name}"/>
              </c:url>
              [<a href="${edit_url}">Изменить</a>]

              <c:url var="delete_url" value="/tags/delete">
                <c:param name="firstLetter" value="${currentLetter}"/>
                <c:param name="tagName" value="${tag.key.name}"/>
              </c:url>
              [<a href="${delete_url}">Удалить</a>]
          </span>
        </c:if>
      </li>
    </c:if>
  </c:forEach>

</ul>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
