<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<%--@elvariable id="section" type="ru.org.linux.site.Section"--%>
<%--@elvariable id="items" type="ru.org.linux.spring.NewsArchiveController.NewsArchiveListItem"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${section.name} - Архив</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<H1>${section.name} - Архив</H1>
<c:forEach items="${items}" var="item">
  <c:url value="/view-news.jsp" var="item_url">
    <c:param name="year" value="${item.year}"/>
    <c:param name="month" value="${item.month}"/>
    <c:param name="section" value="${section.id}"/>
  </c:url>
  <fmt:parseDate var="item_date" value="${item.year} ${item.month}" pattern="yyyy M"/>
  <a href="${fn:escapeXml(item_url)}"><fmt:formatDate value="${item_date}" pattern="yyyy MMMM"/>
    (${item.count})</a> <br/>
</c:forEach>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
