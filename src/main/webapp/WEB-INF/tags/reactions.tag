<%@ tag import="ru.org.linux.site.DateFormats" %>
<%@ tag import="ru.org.linux.util.StringUtil" %><%--
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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="reactions" required="true" type="ru.org.linux.reaction.PreparedReactions" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="comment" required="false" type="ru.org.linux.comment.PreparedComment" %>
<%@ attribute name="reactionList" required="false" type="ru.org.linux.reaction.PreparedReactionList" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<c:set var="all" value="${reactionList != null}"/>
<c:set var="mainClass"><c:if test="${reactions.emptyMap and not all}">zero-reactions</c:if></c:set>
<div class="reactions ${mainClass}">
  <form class="reactions-form" action="/reactions" method="POST">
    <lor:csrf/>
    <input type="hidden" name="topic" value="${topic.id}">
    <c:if test="${comment != null}">
      <input type="hidden" name="comment" value="${comment.id}">
    </c:if>

    <c:set var="disabled"><c:if test="${not reactions.allowInteract}">disabled</c:if></c:set>
    <c:set var="anonymous"><c:if test="${currentUser == null}">reaction-anonymous</c:if></c:set>

    <c:forEach var="r" items="${reactions.map}">
      <c:if test="${all || r.value.count > 0}">
        <c:set var="title">
          Реакция "<c:out escapeXml="true" value="${r.value.description}"/>": <c:forEach
                var="user" items="${r.value.topUsers}">${user.nick}<c:out value=" "/></c:forEach><c:if test="${r.value.hasMore}">...</c:if>
        </c:set>

        <c:set var="clicked">
          <c:if test="${r.value.clicked}">btn-primary</c:if>
        </c:set>

        <button name="reaction" value="${r.key}-${not r.value.clicked}" class="reaction ${clicked} ${anonymous}"
                title="${fn:escapeXml(title)}" ${disabled}>
          <c:out value="${r.key} " escapeXml="true"/> <span class="reaction-count">${r.value.count}</span>
        </button>
      </c:if>
    </c:forEach>

    <c:if test="${not all and not reactions.emptyMap and currentUser != null}">
      <c:if test="${comment==null}">
        <a class="reaction reaction-show-list" href="/reactions?topic=${topic.id}">?</a>
      </c:if>
      <c:if test="${comment!=null}">
        <a class="reaction reaction-show-list" href="/reactions?topic=${topic.id}&comment=${comment.id}">?</a>
      </c:if>
    </c:if>

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
              <c:set var="title">
                Реакция "<c:out escapeXml="true" value="${r.value.description}"/>"
              </c:set>

              <button name="reaction" value="${r.key}-true" title="${fn:escapeXml(title)}"
                  ${disabled} class="reaction ${anonymous}">
                <c:out value="${r.key} " escapeXml="true"/> <span class="reaction-count">0</span>
              </button>
            </c:if>
          </c:forEach>
      <c:if test="${not reactions.emptyMap}">
        </span>
      </c:if>
    </c:if>
  </form>
</div>

<c:if test="${all}">
  <div class="reactions">
    <c:forEach var="r" items="${reactionList.list}">
      <span class="reaction" title="${fn:escapeXml(r.dateFormatted(timezone))}">
        <c:out value="${r.reaction} " escapeXml="true"/> <lor:user user="${r.user}" link="true"/>
      </span>
    </c:forEach>
  </div>
</c:if>


