<%@ tag pageEncoding="UTF-8"%>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ attribute name="poll" required="true" type="ru.org.linux.poll.Poll" %>
<%@ attribute name="enabled" required="true" type="java.lang.Boolean" %>

<form action="/vote.jsp" method="POST">
  <lor:csrf/>
  <input type="hidden" name="voteid" value="${poll.id}">

  <c:forEach var="variant" items="${poll.variants}">
    <label>
      <c:choose>
        <c:when test="${poll.multiSelect}">
          <input type="checkbox" <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
        </c:when>
        <c:otherwise>
          <input type="radio" <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
        </c:otherwise>
      </c:choose>
        ${fn:escapeXml(variant.label)}
    </label><br>
  </c:forEach>

  <c:if test="${enabled}">
    <button type="submit">Голосовать</button>
  </c:if>
</form>