<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.Statement,java.sql.Timestamp,java.util.*,ru.org.linux.site.*,ru.org.linux.spring.TopicsListItem,ru.org.linux.util.BadImageException,ru.org.linux.util.ImageInfo"   buffer="200kb"%>
<%@ page import="ru.org.linux.util.ServletParameterParser"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>


<%
  Connection db = null;
  try {
    boolean showDeleted = (Boolean) request.getAttribute("showDeleted");
    boolean showIgnored = (Boolean) request.getAttribute("showIgnored");

    boolean firstPage = (Boolean) request.getAttribute("firstPage");
    int offset = (Integer) request.getAttribute("offset");

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Group group = (Group) request.getAttribute("group");
    int groupId = group.getId();

    Statement st = db.createStatement();

    int count = group.calcTopicsCount(db, showDeleted);
    int topics = tmpl.getProf().getInt("topics");

    int pages = count / topics;
    if (count % topics != 0) {
      count = (pages + 1) * topics;
    }

    Section section = (Section) request.getAttribute("section");

    if (firstPage || offset >= pages * topics) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    if (firstPage) {
      out.print("<title>" + group.getSectionName() + " - " + group.getTitle() + " (последние сообщения)</title>");
    } else {
      out.print("<title>" + group.getSectionName() + " - " + group.getTitle() + " (сообщения " + (count - offset) + '-' + (count - offset - topics) + ")</title>");
    }
%>
    <LINK REL="alternate" HREF="section-rss.jsp?section=<%= group.getSectionId() %>&amp;group=<%= group.getId()%>" TYPE="application/rss+xml">
    <link rel="parent" title="${group.title}" href="/view-section.jsp?section=${group.sectionId}">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<c:if test="${lastmod}">
  <c:set var="self" value="group-lastmod.jsp"/>
</c:if>
<c:if test="${not lastmod}">
  <c:set var="self" value="group.jsp"/>
</c:if>
<form action="${self}">
  <table class=nav>
    <tr>
    <td align=left valign=middle>
      <a href="view-section.jsp?section=<%= group.getSectionId() %>"><%= group.getSectionName() %></a> - <strong><%= group.getTitle() %></strong>
    </td>

    <td align=right valign=middle>
      [<a href="/wiki/en/lor-faq">FAQ</a>]
      [<a href="rules.jsp">Правила форума</a>]
<%
  User currentUser = User.getCurrentUser(db, session);

  if (group.isTopicPostingAllowed(currentUser)) {
%>
      [<a href="add.jsp?group=<%= groupId %>">Добавить сообщение</a>]
<%
  }
%>
  [<a href="section-rss.jsp?section=<%= group.getSectionId() %>&amp;group=<%=group.getId()%>">RSS</a>]
      <select name=group onchange="submit();" title="Быстрый переход">
<%
        List<Group> groups = Group.getGroups(db, section);

        for (Group g: groups) {
		int id = g.getId();
%>
        <option value=<%= id %> <%= id==groupId?"selected":"" %> ><%= g.getTitle() %></option>
<%
	}
%>
      </select>
     </td>
    </tr>
 </table>

</form>

<h1>${group.sectionName}: ${group.title}</h1>
<%
  if (group.getImage() != null) {
    out.print("<div align=center>");
    try {
      ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + tmpl.getStyle() + group.getImage());
      out.print("<img src=\"/" + tmpl.getStyle() + group.getImage() + "\" " + info.getCode() + " border=0 alt=\"Группа " + group.getTitle() + "\">");
    } catch (BadImageException ex) {
      out.print("[bad image]");
    }
    out.print("</div>");
  }
%>
<c:if test="${group.info != null}">
  <p style="margin-top: 0"><em>${group.info}</em></p>
</c:if>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr>
  <th>Тема<br>
    <form action="${self}" method="GET" style="font-weight: normal; display: inline;">
      фильтр: 
      <input type=hidden name=group value=<%= groupId %>>
      <% if (!firstPage) { %>
        <input type=hidden name=offset value="<%= offset %>">
      <% } %>
        <select name="showignored" onchange="submit();">
          <option value="t" <%= (showIgnored?"selected":"") %>>все темы</option>
          <option value="f" <%= (showIgnored?"":"selected") %>>без игнорируемых</option>
          </select> [<a style="text-decoration: underline" href="ignore-list.jsp">настроить</a>]
    </form>
  </th>
  <th>Последнее сообщение<br>
    <c:if test="${lastmod}">
      <span style="font-weight: normal">[<a href="group.jsp?group=${group.id}" style="text-decoration: underline">отменить</a>]</span>
    </c:if>
    <c:if test="${not lastmod}">
      <span style="font-weight: normal">[<a href="group-lastmod.jsp?group=${group.id}" style="text-decoration: underline">упорядочить</a>]</span>
    </c:if>
  </th>
  <th>Число ответов<br>всего/день/час</th>
</tr>
</thead>
<tbody>

<c:forEach var="topic" items="${topicsList}">

<tr>
  <td>
    <c:if test="${topic.deleted}">
      [<a href="/undelete.jsp?msgid=${topic.msgid}">X</a>]
    </c:if>
    <c:if test="${topic.sticky and not topic.deleted}">
      <img src="/img/paper_clip.gif" width="15" height="15" alt="Прикреплено" title="Прикреплено">
    </c:if>

    <c:if test="${firstPage and topic.pages<=1}">
        <a href="view-message.jsp?msgid=${topic.msgid}&amp;lastmod=${topic.lastmod.time}" rev="contents">
          ${topic.subj}
        </a>
    </c:if>

    <c:if test="${not firstPage or topic.pages>1}">
      <a href="view-message.jsp?msgid=${topic.msgid}" rev="contents">
          ${topic.subj}
      </a>
    </c:if>

    <c:if test="${topic.pages>1}">
      (стр.
      <c:forEach var="i" begin="1" end="${topic.pages-1}"> <c:if test="${i==(topic.pages-1) and firstPage}"><a href="view-message.jsp?msgid=${topic.msgid}&amp;page=${i}&amp;lastmod=${topic.lastmod.time}">${i+1}</a></c:if><c:if test="${i!=(topic.pages-1) or not firstPage}"><a href="view-message.jsp?msgid=${topic.msgid}&amp;page=${i}">${i+1}</a></c:if></c:forEach>)
    </c:if>
    (<lor:user id="${topic.author}" db="<%= db %>" decorate="true"/>)
  </td>

  <td class="dateinterval">
    <lor:dateinterval date="${topic.lastmod}"/>
  </td>

  <td align=center>
      <c:if test="${topic.stat1==0}">-</c:if><c:if test="${topic.stat1>0}"><b>${topic.stat1}</b></c:if>/<c:if test="${topic.stat3==0}">-</c:if><c:if test="${topic.stat3>0}"><b>${topic.stat3}</b></c:if>/<c:if test="${topic.stat4==0}">-</c:if><c:if test="${topic.stat4>0}"><b>${topic.stat4}</b></c:if> 
  </td> </tr>
</c:forEach>
</tbody>
<tfoot>
<%
  out.print("<tr><td colspan=3><p>");

  String ignoredAdd = showIgnored ?("&amp;showignored=t"):"";

  out.print("<div style=\"float: left\">");

  boolean lastmod = (Boolean) request.getAttribute("lastmod");
  String self = lastmod?"group-lastmod.jsp":"group.jsp";

  // НАЗАД
  if (firstPage) {
    out.print("");
  } else if ((!lastmod && offset == pages * topics) || (lastmod && offset == topics)) {
    out.print("<a href=\""+self+"?group=" + groupId + ignoredAdd + "\">← начало</a> ");
  } else if (!lastmod) {
    out.print("<a rel=prev rev=next href=\""+self+"?group=" + groupId + "&amp;offset=" + (offset + topics) + ignoredAdd + "\">← назад</a>");
  } else {
    out.print("<a rel=prev rev=next href=\""+self+"?group=" + groupId + "&amp;offset=" + (offset - topics) + ignoredAdd + "\">← назад</a>");
  }

  out.print("</div>");

  // ВПЕРЕД
  out.print("<div style=\"float: right\">");

  if (firstPage && !lastmod) {
    out.print("<a rel=next rev=prev href=\""+self+"?group=" + groupId + "&amp;offset=" + (pages * topics) + ignoredAdd + "\">архив →</a>");
  } else if (offset == 0 && !firstPage) {
  } else if (!lastmod) {
    out.print("<a rel=next rev=prev href=\""+self+"?group=" + groupId + "&amp;offset=" + (offset - topics) + ignoredAdd + "\">вперед →</a>");
  } else {
    out.print("<a rel=next rev=prev href=\""+self+"?group=" + groupId + "&amp;offset=" + (offset + topics) + ignoredAdd + "\">вперед →</a>");
  }

  out.print("</div>");
%>
</tfoot>
</table>
</div>
<c:if test="${not lastmod}">
<div align=center><p>
<%
  for (int i=0; i<=pages+1; i++) {
    if (firstPage) {
      if (i != 0 && i != (pages + 1) && i > 7) {
        continue;
      }
    } else {
      if (i != 0 && i != (pages + 1) && Math.abs((pages + 1 - i) * topics - offset) > 7 * topics) {
        continue;
      }
    }

    if (i==pages+1) {
      if (offset != 0 || firstPage) {
        out.print("[<a href=\"group.jsp?group=" + groupId + "&amp;offset=0" + ignoredAdd + "\">последняя</a>] ");
      } else {
        out.print("[<b>последняя</b>] ");
      }
    } else if (i==0) {
      if (firstPage) {
        out.print("[<b>первая</b>] ");
      } else {
        out.print("[<a href=\"group.jsp?group=" + groupId + ignoredAdd + "\">первая</a>] ");
      }
    } else if ((pages + 1 - i) * topics == offset) {
      out.print("<b>" + (pages + 1 - i) + "</b> ");
    } else {
      out.print("<a href=\"group.jsp?group=" + groupId + "&amp;offset=" + ((pages + 1 - i) * topics) + ignoredAdd + "\">" + (pages + 1 - i) + "</a> ");
    }
  }
%>
<p>
</div>

<% if (Template.isSessionAuthorized(session) && !showDeleted) { %>
  <hr>
  <form action="group.jsp" method=POST>
  <input type=hidden name=group value=<%= groupId %>>
  <input type=hidden name=deleted value=1>
  <% if (!firstPage) { %>
    <input type=hidden name=offset value="<%= offset %>">
  <% } %>
  <input type=submit value="Показать удаленные сообщения">
  </form>
  <hr>
<% } %>

</c:if>

<%
	st.close();
	db.commit();
%>
<%
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
