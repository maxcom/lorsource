<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" body-content="scriptless" %>
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
<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="style" required="false" type="java.lang.String" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="currentUri" value="${requestScope['jakarta.servlet.forward.request_uri']}"/>
<c:if test="${empty currentUri}">
  <c:set var="currentUri" value="${pageContext.request.requestURI}"/>
</c:if>
<c:url var="loginLinkUrl" value="/login.jsp">
  <c:param name="from" value="${currentUri}"/>
</c:url>
<a<c:if test="${not empty id}"> id="${fn:escapeXml(id)}"</c:if><c:if test="${not empty style}"> style="${fn:escapeXml(style)}"</c:if> href="${fn:escapeXml(loginLinkUrl)}"><jsp:doBody/></a>