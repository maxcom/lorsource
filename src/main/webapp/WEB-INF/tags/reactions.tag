<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="reactions" required="true" type="ru.org.linux.reaction.PreparedReactions" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="comment" required="false" type="ru.org.linux.comment.PreparedComment" %>
<%@ attribute name="all" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<c:if test="${reactionsEnabled}">
  <c:set var="mainClass"><c:if test="${reactions.emptyMap and not all}">zero-reactions</c:if></c:set>
  <div class="reactions ${mainClass}">
    <form class="reactions-form" action="/reactions" method="POST">
      <lor:csrf/>
      <input type="hidden" name="topic" value="${topic.id}">
      <c:if test="${comment != null}">
        <input type="hidden" name="comment" value="${comment.id}">
      </c:if>

      <c:set var="disabled"><c:if test="${not reactions.allowInteract}">disabled</c:if></c:set>

      <c:forEach var="r" items="${reactions.map}">
        <c:if test="${all || r.value.count > 0}">
          <c:set var="title">
            <c:forEach var="user" items="${r.value.topUsers}">${user.nick}<c:out value=" "/></c:forEach>

            <c:if test="${r.value.hasMore}">...</c:if>
          </c:set>

          <c:set var="clicked">
            <c:if test="${r.value.clicked}">btn-primary</c:if>
          </c:set>

          <button name="reaction" value="${r.key}-${not r.value.clicked}" class="reaction ${clicked}"
                  title="${title}" ${disabled}>
            <c:out value="${r.key}" escapeXml="true"/> <span class="reaction-count">${r.value.count}</span>
          </button>
        </c:if>
      </c:forEach>

      <c:if test="${not all and not reactions.total and reactions.allowInteract}">
        <c:if test="${not reactions.emptyMap}">
          <c:if test="${comment==null}">
            <a class="reaction reaction-show" href="/reactions?topic=${topic.id}">&raquo;</a>
          </c:if>
          <c:if test="${comment!=null}">
            <a class="reaction reaction-show" href="/reactions?topic=${topic.id}&comment=${comment.id}">&raquo;</a>
          </c:if>
        </c:if>

        <c:if test="${not reactions.emptyMap}">
          <span class="zero-reactions">
        </c:if>
            <c:forEach var="r" items="${reactions.map}">
              <c:if test="${!all && r.value.count == 0}">
                <button name="reaction" value="${r.key}-true" class="reaction">
                  <c:out value="${r.key}" escapeXml="true"/> <span class="reaction-count">0</span>
                </button>
              </c:if>
            </c:forEach>
        <c:if test="${not reactions.emptyMap}">
          </span>
        </c:if>
      </c:if>
    </form>
  </div>
</c:if>


