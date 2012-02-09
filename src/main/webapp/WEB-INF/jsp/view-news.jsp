<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.group.Group,ru.org.linux.section.Section"   buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

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
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="ptitle" type="java.lang.String"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
	<title>${ptitle}</title>
<%
  Group group = (Group) request.getAttribute("group");
  Section section = (Section) request.getAttribute("section");
%>

<c:if test="${rssLink != null}">
  <LINK REL="alternate" HREF="${rssLink}" TYPE="application/rss+xml">
</c:if>

<c:if test="${meLink != null}">
  <LINK REL="me" HREF="${fn:escapeXml(meLink)}">
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

        <%
          if (section.isVotePoll()) {
            out.print("<li><a href=\"add.jsp?group=19387\">Добавить</a></li>");
          } else {
            if (group == null) {
              out.print("<li><a href=\"add-section.jsp?section=" + section.getId() + "\">Добавить</a></li>");
            } else {
              out.print("<li><a href=\"add.jsp?group=" + group.getId() + "\">Добавить</a></li>");
            }
          }

          if (section.getId() == Section.SECTION_FORUM) {
            if (group == null) {
              out.print("<li><a href=\"/forum/\">Таблица</a></li>");
            } else {
              out.print("<li><a href=\"/forum/" + group.getUrlName() + "/\">Таблица</a></li>");
            }
          }
        %>
      </c:if>

      <c:if test="${archiveLink != null}">
        <li><a href="${archiveLink}">Архив</a></li>
      </c:if>

      <c:if test="${whoisLink != null}">
        <li><a href="${whoisLink}">Профиль</a><li>
      </c:if>

      <c:if test="${rssLink != null}">
        <li><a href="${rssLink}">RSS</a></li>
      </c:if>
      </ul>
      <c:if test="${sectionList != null}">
        <form:form commandName="topicListForm" id="filterForm" action="" method="get">
          <form:select path="section" onchange="$('#filterForm').submit();">
            <form:option value="0" label="Все" />
            <form:options items="${sectionList}" itemLabel="title" itemValue="id" />
          </form:select>
        </form:form>
      </c:if>
    </div>
</div>
<H1 class="optional">${ptitle}</H1>
<c:forEach var="msg" items="${messages}">
  <lor:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="<%= section==null && group==null %>" moderateMode="false"/>
</c:forEach>

<c:if test="${offsetNavigation}">
  <c:if test="${params !=null}">
    <c:set var="aparams" value="${params}&"/>
  </c:if>

  <table class="nav">
    <tr>
      <c:if test="${topicListForm.offset < 200 && fn:length(messages) == 20}">
        <td align="left" width="35%">
          <a href="${url}?${aparams}offset=${topicListForm.offset+20}">← предыдущие</a>
        </td>
      </c:if>
      <c:if test="${topicListForm.offset > 20}">
        <td width="35%" align="right">
          <a href="${url}?${aparams}offset=${topicListForm.offset-20}">следующие →</a>
        </td>
      </c:if>
      <c:if test="${topicListForm.offset == 20}">
        <td width="35%" align="right">
          <c:if test="${params!=null}">
            <a href="${url}?${params}">следующие →</a>
          </c:if>
          <c:if test="${params==null}">
            <a href="${url}">следующие →</a>
          </c:if>
        </td>
      </c:if>
    </tr>
  </table>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

