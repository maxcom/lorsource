<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="activeTags" type="java.util.List<java.lang.String>"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
	<title>${ptitle}</title>

<c:if test="${rssLink != null}">
  <link rel="alternate" href="${rssLink}" type="application/rss+xml">
</c:if>
<script type="text/javascript">
  <!--
  function goto(mySelect) {
    window.location.href = mySelect.options[mySelect.selectedIndex].value;
  }
  -->
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${navtitle}</h1>

<nav>
  <c:if test="${section!=null and section.premoderated}">
    <c:if test="${offsetNavigation}">
      <a class="btn btn-selected" href="${section.sectionLink}">Новые темы</a>
    </c:if>
    <c:if test="${not offsetNavigation}">
      <a class="btn btn-default" href="${section.sectionLink}">Новые темы</a>
    </c:if>
  </c:if>

  <c:if test="${sectionList == null and template.moderatorSession and group!=null}">
    <a class="btn btn-default" href="groupmod.jsp?group=${group.id}">Править группу</a>
  </c:if>
  <c:if test="${sectionList == null and section != null}">
    <c:if test="${section.premoderated}">
      <a class="btn btn-default" href="/view-all.jsp?section=${section.id}">Неподтвержденные</a>
    </c:if>
    <c:if test="${section.id == Section.SECTION_FORUM}">
      <c:choose>
        <c:when test="${group == null}">
          <a class="btn btn-default" href="/forum/">Таблица</a>
        </c:when>
        <c:otherwise>
          <a class="btn btn-default" href="/forum/${group.urlName}">Таблица</a>
        </c:otherwise>
      </c:choose>
    </c:if>
  </c:if>

  <c:if test="${archiveLink != null}">
    <c:if test="${offsetNavigation}">
      <a class="btn btn-default" href="${archiveLink}">Архив</a>
    </c:if>
    <c:if test="${not offsetNavigation}">
      <a class="btn btn-selected" href="${archiveLink}">Архив</a>
    </c:if>
  </c:if>

  <c:if test="${section != null}">
    <c:choose>
      <c:when test="${section.pollPostAllowed}">
        <a class="btn btn-primary" href="add.jsp?group=19387">Добавить</a>
      </c:when>
      <c:when test="${group == null}">
        <a class="btn btn-primary" href="add-section.jsp?section=${section.id}">Добавить</a>
      </c:when>
      <c:otherwise>
        <a class="btn btn-primary" href="add.jsp?group=${group.id}">Добавить</a>
      </c:otherwise>
    </c:choose>
  </c:if>

  <c:if test="${fn:length(groupList)>1 and offsetNavigation}">
  <div class="nav-buttons">
    <form>
      <select name=group onchange="goto(this);" title="Быстрый переход" class="btn btn-default">
        <c:choose>
          <c:when test="${group == null}">
            <option value="${section.newsViewerLink}" selected>Все темы</option>
          </c:when>
          <c:otherwise>
            <option value="${section.newsViewerLink}">Все темы</option>
          </c:otherwise>
        </c:choose>

        <c:forEach items="${groupList}" var="item">
          <c:if test="${item.id == group.id}">
            <option value="${item.url}" selected>${item.title}</option>
          </c:if>
          <c:if test="${item.id != group.id}">
            <option value="${item.url}">${item.title}</option>
          </c:if>
        </c:forEach>
      </select>
    </form>
  </div>
  </c:if>
</nav>

<c:if test="${not empty activeTags}">
  <div class="infoblock" style="font-size: medium">
    Активные теги: <l:tags list="${activeTags}"/>
  </div>
</c:if>

<c:forEach var="msg" items="${messages}">
  <lor:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="${group==null}" moderateMode="false"/>
</c:forEach>

<c:if test="${offsetNavigation}">
  <table class="nav">
    <tr>
      <c:if test="${topicListRequest.offset > 20}">
        <td width="35%" align="left">
          <c:url var="prevUrl" value="${url}">
            <c:param name="offset" value="${topicListRequest.offset-20}"/>
          </c:url>
          <a href="${prevUrl}">← назад</a>
        </td>
      </c:if>
      <c:if test="${topicListRequest.offset == 20}">
        <td width="35%" align="left">
          <a href="${url}">← назад</a>
        </td>
      </c:if>
      <c:choose>
        <c:when test="${topicListRequest.offset < 200 && fn:length(messages) == 20}">
          <td align="right" width="35%">
            <c:url var="nextUrl" value="${url}">
              <c:param name="offset" value="${topicListRequest.offset+20}"/>
            </c:url>
            <a href="${nextUrl}">вперед →</a>
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

<c:if test="${rssLink != null}">
<p>
  <i class="icon-rss"></i>
  <a href="${rssLink}">
    RSS подписка на новые темы
  </a>
</p>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
