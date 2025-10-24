<%--
  ~ Copyright 1998-2023 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="messages" required="false" type="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class=tracker>
  <c:forEach var="msg" items="${messages}">
    <a href="${msg.lastPageUrl}" class="tracker-item">
      <div class="tracker-src">
        <p>
          <span class="group-label">${msg.groupTitle}</span>
          <c:if test="${msg.uncommited}">(не подтверждено)</c:if><br class="hideon-phone hideon-tablet">
          <c:if test="${msg.topicAuthor != null}"><lor:user user="${msg.topicAuthor}"/></c:if>
        </p>
      </div>

      <div class="tracker-count">
        <p>
          <c:choose>
            <c:when test="${msg.commentCount==0}">
              -
            </c:when>
            <c:otherwise>
              <i class="icon-comment"></i> ${msg.commentCount}
            </c:otherwise>
          </c:choose>
        </p>
      </div>

      <div class="tracker-title">
        <p>
          <c:if test="${msg.commentsClosed and not msg.deleted}">
            &#128274;
          </c:if>
          <c:if test="${msg.resolved}">
            <img src="/img/solved.png" alt="решено" title="решено" width=15 height=15>
          </c:if>

          <l:title>${msg.title}</l:title>
        </p>
      </div>

      <div class="tracker-tags">
        <p>
          <c:forEach var="tag" items="${msg.tags}">
            <span class="tag">${tag}</span>
          </c:forEach>
        </p>
      </div>

      <div class="tracker-last">
        <p>
          <lor:user user="${msg.author}"/>, <lor:dateinterval date="${msg.postdate}" compact="true"/>
        </p>
      </div>
    </a>
  </c:forEach>
</div>
