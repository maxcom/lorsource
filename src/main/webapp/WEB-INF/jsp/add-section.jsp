<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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

<title>${section.title}: добавление</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<c:if test="${info!=null}">
  ${info}
  <h2>Выберите группу</h2>
</c:if>

<c:if test="${info==null}">
  <h1>${section.title}: добавление</h1>
</c:if>

Доступные группы:
<ul>
<c:forEach var="group" items="${groups}">
  <li>
    <c:if test="${not empty tag}">
      <a href="add.jsp?group=${group.id}&amp;tags=${tag}&amp;noinfo=1">${group.title}</a> (<a href="${group.url}">просмотр...</a>)
    </c:if>
    <c:if test="${empty tag}">
      <a href="add.jsp?group=${group.id}&amp;noinfo=1">${group.title}</a> (<a href="${group.url}">просмотр...</a>)
    </c:if>

    <c:if test="${group.info != null}">
      - <em><c:out value="${group.info}" escapeXml="false"/></em>
    </c:if>
  </li>
</c:forEach>
</ul>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
