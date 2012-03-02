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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%

  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

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
    <li>
      <c:url value="/view-news.jsp" var="tag_url">
          <c:param name="tag" value="${tag.key}"/>
      </c:url>
      <a href="${fn:escapeXml(tag_url)}">${tag.key}</a>

      (${tag.value})

    </li>

  </c:forEach>

</ul>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
