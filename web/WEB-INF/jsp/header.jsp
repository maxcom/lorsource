<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

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

<LINK REL=STYLESHEET TYPE="text/css" HREF="/common.css" TITLE="Normal">
<link rel="search" title="Search L.O.R." href="/search.jsp">
<link rel="top" title="Linux.org.ru" href="/">
<script src="/js/lor.js" type="text/javascript">;</script>

<c:if test="${template.hover}">
  <LINK REL=STYLESHEET TYPE="text/css" HREF="/${template.style}/hover.css" TITLE="Normal">
</c:if>

<base href="${fn:escapeXml(template.mainUrl)}">

<jsp:include page="${template.style}/head.jsp"/>

<!--TopList COUNTER-->
<img height=1 width=1 src="http://top.list.ru/counter?id=71642" alt="">
<!--TopList COUNTER-->
<div id="bd">
