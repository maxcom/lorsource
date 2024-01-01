<%@ tag pageEncoding="UTF-8" %>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ attribute name="poll" required="true" type="ru.org.linux.poll.Poll" %>
<%@ attribute name="enabled" required="true" type="java.lang.Boolean" %>
<%@ attribute name="votedVariants" required="false" type="java.util.List" %>

<c:if test="${enabled}">
  <form action="/vote.jsp" method="POST" style="margin-bottom: 1em">
  <lor:csrf/>
  <input type="hidden" name="voteid" value="${poll.id}">
</c:if>

<c:forEach var="variant" items="${not empty votedVariants ? votedVariants : poll.variants}">
  <c:set var="lineClass">
    <c:choose>
      <c:when test="${poll.multiSelect}">checkbox</c:when>
      <c:otherwise>radio</c:otherwise>
    </c:choose>
  </c:set>
  <div class="${lineClass}">
    <label>

      <c:choose>
        <c:when test="${not empty votedVariants}">
          <c:choose>
            <c:when test="${!variant.userVoted}">
              <c:choose>
                <c:when test="${poll.multiSelect}">
                  <input type="checkbox"
                         <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
                </c:when>
                <c:otherwise>
                  <input type="radio"
                         <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
                </c:otherwise>
              </c:choose>
            </c:when>
            <c:otherwise>
              <span class="penguin_progress" style="width:1em;margin-left: 3px; margin-right: 3px"><span></span></span>
            </c:otherwise>
          </c:choose>

        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${poll.multiSelect}">
              <input type="checkbox"
                     <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
            </c:when>
            <c:otherwise>
              <input type="radio"
                     <c:if test="${not enabled}">disabled</c:if> name="vote" value="${variant.id}">
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>


        ${fn:escapeXml(variant.label)}
    </label>
  </div>
</c:forEach>

<c:if test="${enabled}">
  <button type="submit" class="btn btn-small btn-default">Голосовать</button>
  </form>
</c:if>
