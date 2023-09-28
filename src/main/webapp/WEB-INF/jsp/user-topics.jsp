<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<%--@elvariable id="user" type="ru.org.linux.user.User"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
	<title>${ptitle}</title>

<c:if test="${rssLink != null}">
  <link rel="alternate" href="${rssLink}" type="application/rss+xml">
</c:if>

<c:if test="${meLink != null}">
  <link rel="me" href="${fn:escapeXml(meLink)}">
</c:if>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${navtitle}<%= " "%><a href="${whoisLink}">${user.nick}</a></h1>

<nav>
  <c:if test="${sectionList != null}">
    <c:if test="${section == null}">
      <a href="${url}" class="btn btn-selected">Все</a>
    </c:if>
    <c:if test="${section != null}">
      <a href="${url}" class="btn btn-default">Все</a>
    </c:if>

    <c:forEach items="${sectionList}" var="cursection">
      <c:if test="${section == cursection}">
        <a href="${url}?section=${cursection.id}" class="btn btn-selected">${cursection.name}</a>
      </c:if>
      <c:if test="${section != cursection}">
        <a href="${url}?section=${cursection.id}" class="btn btn-default">${cursection.name}</a>
      </c:if>
    </c:forEach>
  </c:if>
</nav>

<c:if test="${showSearch}">
<div class="infoblock">
  <form method="GET" commandName="query" action="search.jsp">
    <div class="control-group">
      <input name="q" type="search" size="50" maxlength="250" placeholder="Поиск в темах пользователя"/>&nbsp;
      <button type="submit" class="btn btn-default btn-small">Поиск</button>
    </div>

    <div class="control-group">
      <select name="range">
        <option value="ALL">включая комментарии</option>
        <option value="TOPICS">без комментариев</option>
      </select>
    </div>

    <input type="hidden" name="user" value="${nick}"/>
    <input type="hidden" name="usertopic" value="true"/>
  </form>
</div>
</c:if>

<c:forEach var="msg" items="${messages}">
  <lor:news
          preparedMessage="${msg.preparedTopic}"
          messageMenu="${msg.topicMenu}"
          multiPortal="${section==null && group==null}"
          moderateMode="false"
          minorAsMajor="true"
  />
</c:forEach>

<c:if test="${params !=null}">
  <c:set var="aparams" value="${params}&"/>
</c:if>

<table class="nav">
  <tr>
    <c:if test="${offset > 20}">
      <td width="35%" align="left">
        <a href="${url}?${aparams}offset=${offset-20}">← назад</a>
      </td>
    </c:if>
    <c:if test="${offset == 20}">
      <td width="35%" align="left">
        <c:if test="${params!=null}">
          <a href="${url}?${params}">← назад</a>
        </c:if>
        <c:if test="${params==null}">
          <a href="${url}">← назад</a>
        </c:if>
      </td>
    </c:if>
    <c:if test="${offset < 200 && fn:length(messages) == 20}">
      <td align="right" width="35%">
        <a href="${url}?${aparams}offset=${offset+20}">вперед →</a>
      </td>
    </c:if>
  </tr>
</table>

<c:if test="${rssLink != null}">
<p>
  <i class="icon-rss"></i>
  <a href="${rssLink}">
    RSS подписка на новые темы
  </a>
</p>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
