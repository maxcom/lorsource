<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,ru.org.linux.site.Group,ru.org.linux.site.LorDataSource"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Section" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
	<title>${ptitle}</title>
<%
  Group group = (Group) request.getAttribute("group");
  Section section = (Section) request.getAttribute("section");
  String tag = (String) request.getAttribute("tag");
  User user = (User) request.getAttribute("user");
%>

<c:if test="${rssLink != null}">
  <LINK REL="alternate" HREF="${rssLink}" TYPE="application/rss+xml">
</c:if>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>
  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      <strong>${navtitle}</strong>
    </td>
    <td align=right valign=middle>
      <c:if test="${section != null}">
        <%
          if (section.isVotePoll()) {
            out.print("[<a href=\"add-poll.jsp?group=19387\">Добавить голосование</a>]");
          } else {
            if (group == null) {
              out.print("[<a href=\"add-section.jsp?section=" + section.getId() + "\">Добавить</a>]");
            } else {
              out.print("[<a href=\"add.jsp?group=" + group.getId() + "\">Добавить</a>]");
            }
          }

          if (group == null) {
            out.print("[<a href=\"view-section.jsp?section=" + section.getId() + "\">Таблица</a>]");
          } else {
            out.print("[<a href=\"group.jsp?group=" + group.getId() + "\">Таблица</a>]");
          }
        %>
        [<a href="view-news-archive.jsp?section=${section.id}">Архив</a>]
      </c:if>
      <c:if test="${rssLink != null}">
        [<a href="${rssLink}">RSS</a>]
      </c:if>
    </td>
  </tr>
</table>

<H1 class="optional">${ptitle}</H1>
<%

  Connection db = null;
  try {
    db = LorDataSource.getConnection();
%>
<c:forEach var="msg" items="${messages}">
  <lor:news db="<%= db %>" message="${msg}" multiPortal="<%= section==null && group==null %>" moderateMode="false"/>
</c:forEach>

<%
  String params = "";
  if (section!=null) {
    params += "section="+section.getId();
  }

  if (tag!=null) {
    if (params.length()>0) {
      params += "&amp;";
    }

    params += "tag="+ URLEncoder.encode(tag, "UTF-8");
  }

  if (group!=null) {
    if (params.length()>0) {
      params += "&amp;";
    }

    params += "group="+group.getId();
  }

  if (user!=null) {
    if (params.length()>0) {
      params += "&amp;";
    }

    params += "nick="+user.getNick();    
  }

  String url = user==null?"view-news.jsp":"show-topics.jsp";
%>
<c:if test="${offsetNavigation}">
  <table class="nav">
    <tr>
      <c:if test="${offset < 200}">
        <td align="left" width="35%">
          <a href="<%= url %>?<%= params %>&amp;offset=${offset+20}">← предыдущие</a>
        </td>
      </c:if>
      <c:if test="${offset > 20}">
        <td width="35%" align="right">
          <a href="<%= url %>?<%= params %>&amp;offset=${offset-20}">следующие →</a>
        </td>
      </c:if>
      <c:if test="${offset == 20}">
        <td width="35%" align="right">
          <a href="<%= url %>?<%= params %>">следующие →</a>
        </td>
      </c:if>
    </tr>
  </table>
</c:if>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

