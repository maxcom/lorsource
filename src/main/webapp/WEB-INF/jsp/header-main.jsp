<%@ page import="ru.org.linux.site.Template" %>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>

<link rel="search" title="Search L.O.R." href="/search.jsp">

<base href="${fn:escapeXml(configuration.secureUrl)}">

<jsp:include page="${template.theme.headMain}"/>

  <c:if test="${template.moderatorSession or template.correctorSession}">
<div class="nav" style="border-bottom: none">
  <c:if test="${uncommited > 0}">
    [<a href="view-all.jsp">Неподтверждённых</a>: ${uncommited},
      в том числе 
    <c:if test="${uncommitedGallery > 0}">
      <a href="view-all.jsp?section=3">изображений</a>:&nbsp;${uncommitedGallery};
    </c:if>
    <c:if test="${uncommitedNews > 0}">
      <a href="view-all.jsp?section=1">новостей</a>:&nbsp;${uncommitedNews};
    </c:if>
    <c:if test="${uncommitedPolls > 0}">
      <a href="view-all.jsp?section=5">опросов</a>:&nbsp;${uncommitedPolls};
    </c:if>
    <c:if test="${uncommitedArticles > 0}">
      <a href="view-all.jsp?section=6">статей</a>:&nbsp;${uncommitedArticles};
    </c:if>]
  </c:if>
</div>
</c:if>

<main id="bd">
