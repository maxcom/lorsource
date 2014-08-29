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
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<link rel="search" title="Search L.O.R." href="/search.jsp">
<meta name="referrer" content="always">

<c:if test="${pageContext.request.secure}">
  <base href="${fn:escapeXml(template.secureMainUrl)}">
</c:if>

<c:if test="${not pageContext.request.secure}">
  <base href="${fn:escapeXml(template.mainUrl)}">
</c:if>

<c:if test="${template != null}">
  <jsp:include page="${template.theme.head}"/>
</c:if>

<div id="bd">
