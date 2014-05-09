<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
	<title>${ptitle}</title>

<c:if test="${rssLink != null}">
  <link rel="alternate" href="${rssLink}" type="application/rss+xml">
</c:if>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>
  <div class=nav>
    <div id="navPath">
      ${navtitle}
    </div>
    <div class="nav-buttons">
      <ul>

      <c:if test="${sectionList == null and template.moderatorSession and group!=null}">
        <li><a href="groupmod.jsp?group=${group.id}">Править группу</a></li>
      </c:if>
      <c:if test="${sectionList == null and section != null}">
        <c:if test="${section.premoderated}">
          <li><a href="/view-all.jsp?section=${section.id}">Неподтвержденные</a></li>
        </c:if>
        <c:choose>
          <c:when test="${section.pollPostAllowed}">
            <li><a href="add.jsp?group=19387">Добавить</a></li>
          </c:when>
          <c:when test="${group == null}">
            <li><a href="add-section.jsp?section=${section.id}">Добавить</a></li>
          </c:when>
          <c:otherwise>
            <li><a href="add.jsp?group=${group.id}">Добавить</a></li>
          </c:otherwise>
        </c:choose>
        <c:if test="${section.id == Section.SECTION_FORUM}">
          <c:choose>
            <c:when test="${group == null}">
              <li><a href="/forum/">Таблица</a></li>
            </c:when>
            <c:otherwise>
              <li><a href="/forum/${group.urlName}">Таблица</a></li>
            </c:otherwise>
          </c:choose>
        </c:if>
      </c:if>

      <c:if test="${archiveLink != null}">
        <li><a href="${archiveLink}">Архив</a></li>
      </c:if>

      <c:if test="${rssLink != null}">
        <li><a href="${rssLink}">RSS</a></li>
      </c:if>
      </ul>
      <c:if test="${sectionList != null}">
        <form:form commandName="topicListRequest" id="filterForm" action="${url}" method="get">
          <form:select path="section" onchange="$('#group').val('0'); $('#filterForm').submit();">
            <form:option value="0" label="Все" />
            <form:options items="${sectionList}" itemLabel="title" itemValue="id" />
          </form:select>
          <noscript><input type='submit' value='&gt;'></noscript>
        </form:form>
      </c:if>
    </div>
</div>
<c:forEach var="msg" items="${messages}">
  <lor:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="${group==null}" moderateMode="false"/>
</c:forEach>

<c:if test="${offsetNavigation}">
  <c:if test="${params !=null}">
    <c:set var="aparams" value="${params}&"/>
  </c:if>

  <table class="nav">
    <tr>
      <c:if test="${topicListRequest.offset > 20}">
        <td width="35%" align="left">
          <a href="${url}?${aparams}offset=${topicListRequest.offset-20}">← назад</a>
        </td>
      </c:if>
      <c:if test="${topicListRequest.offset == 20}">
        <td width="35%" align="left">
          <c:if test="${params!=null}">
            <a href="${url}?${params}">← назад</a>
          </c:if>
          <c:if test="${params==null}">
            <a href="${url}">← назад</a>
          </c:if>
        </td>
      </c:if>
      <c:choose>
        <c:when test="${topicListRequest.offset < 200 && fn:length(messages) == 20}">
          <td align="right" width="35%">
            <a href="${url}?${aparams}offset=${topicListRequest.offset+20}">вперед →</a>
          </td>
        </c:when>
        <c:otherwise>
          <c:if test="${archiveLink != null}">
            <td align="right" width="35%">
              <a href="${archiveLink}">архив</a>
            </td>
          </c:if>
        </c:otherwise>
      </c:choose>
    </tr>
  </table>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
