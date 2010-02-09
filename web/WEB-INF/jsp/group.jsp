<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.Group,ru.org.linux.site.LorDataSource,ru.org.linux.site.Template,ru.org.linux.site.User"   buffer="200kb"%>
<%@ page import="ru.org.linux.util.BadImageException" %>
<%@ page import="ru.org.linux.util.ImageInfo" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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

<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.site.TopicsListItem>"--%>
<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>
<%--@elvariable id="firstPage" type="java.lang.Boolean"--%>
<%--@elvariable id="groupList" type="java.util.List<ru.org.linux.site.Group>"--%>
<%--@elvariable id="lastmod" type="java.lang.Boolean"--%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<script type="text/javascript">
  <!--
  function goto(mySelect) {
      window.location.href = mySelect.options[mySelect.selectedIndex].value;
  }
  -->
</script>

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

    int count = group.calcTopicsCount(db, showDeleted);
    int topics = tmpl.getProf().getInt("topics");

    int pages = count / topics;
    if (count % topics != 0) {
      count = (pages + 1) * topics;
    }

    if (firstPage || offset >= pages * topics) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }
%>
<title>${group.sectionName} - ${group.title}
  <c:if test="${not firstPage}">
<%
    out.print(" (сообщения " + (count - offset) + '-' + (count - offset - topics) + ")");
%>
</c:if>
</title>
    <LINK REL="alternate" HREF="section-rss.jsp?section=${sectionId}&amp;group=${group.id}" TYPE="application/rss+xml">
    <link rel="parent" title="${group.title}" href="${group.sectionLink}">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<form>
  <table class=nav>
    <tr>
    <td align=left valign=middle id="navPath">
      <a href="${group.sectionLink}">${group.sectionName}</a> - <strong>${group.title}</strong>
    </td>

    <td align=right valign=middle>
      <% if (tmpl.isModeratorSession()) { %>
        [<a href="groupmod.jsp?group=${group.id}">Править группу</a>]
      <% } %>
      [<a href="/wiki/en/lor-faq">FAQ</a>]
      [<a href="rules.jsp">Правила форума</a>]
<%
  User currentUser = User.getCurrentUser(db, session);

  if (group.isTopicPostingAllowed(currentUser)) {
%>
      [<a href="add.jsp?group=${group.id}">Добавить сообщение</a>]
<%
  }
%>
  [<a href="section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}">RSS</a>]
      <select name=group onchange="goto(this)" title="Быстрый переход">
        <c:forEach items="${groupList}" var="item">
          <c:if test="${item.id == group.id}">
            <c:if test="${lastmod}">
              <option value="${item.url}?lastmod=true" selected>${item.title}</option>
            </c:if>
            <c:if test="${not lastmod}">
              <option value="${item.url}" selected>${item.title}</option>
            </c:if>
          </c:if>
          <c:if test="${item.id != group.id}">
            <c:if test="${lastmod}">
              <option value="${item.url}?lastmod=true">${item.title}</option>
            </c:if>
            <c:if test="${not lastmod}">
              <option value="${item.url}">${item.title}</option>
            </c:if>
          </c:if>
        </c:forEach>
      </select>
     </td>
    </tr>
 </table>

</form>

<h1 class="optional">${group.sectionName}: ${group.title}</h1>
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
<lor:groupinfo group="${group}" db="<%= db %>"/>
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr>
  <th>Тема<br>
    <form action="${group.url}" method="GET" style="font-weight: normal; display: inline;">
      фильтр:
      <c:if test="${lastmod}">
        <input type=hidden name=lastmod value=true>
      </c:if>
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
      <span style="font-weight: normal">[<a href="${group.url}" style="text-decoration: underline">отменить</a>]</span>
    </c:if>
    <c:if test="${not lastmod}">
      <span style="font-weight: normal">[<a href="${group.url}?lastmod=true" style="text-decoration: underline">упорядочить</a>]</span>
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
    <c:if test="${topic.resolved}">
      <img src="/img/solved.png" width="15" height="15" alt="Решено" title="Решено"/>
    </c:if>

    <c:if test="${firstPage and topic.pages<=1}">
        <a href="${group.url}${topic.msgid}?lastmod=${topic.lastmod.time}" rev="contents">
          ${topic.subj}
        </a>
    </c:if>

    <c:if test="${not firstPage or topic.pages>1}">
      <a href="${group.url}${topic.msgid}" rev="contents">
          ${topic.subj}
      </a>
    </c:if>

    <c:if test="${topic.pages>1}">
      (стр.
      <c:forEach var="i" begin="1" end="${topic.pages-1}"> <c:if test="${i==(topic.pages-1) and firstPage}"><a href="${group.url}${topic.msgid}/page${i}?lastmod=${topic.lastmod.time}">${i+1}</a></c:if><c:if test="${i!=(topic.pages-1) or not firstPage}"><a href="${group.url}${topic.msgid}/page${i}">${i+1}</a></c:if></c:forEach>)
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
<tr><td colspan=3><p>
<div style="float: left">
<%
  String urlAdd = showIgnored ?("&amp;showignored=t"):"";

  boolean lastmod = (Boolean) request.getAttribute("lastmod");
  if (lastmod) {
    urlAdd+="&amp;lastmod=true";
  }

  // НАЗАД
  if (!firstPage) {
    if ((!lastmod && offset == pages * topics) || (lastmod && offset == topics)) {
      if (urlAdd.length()>0) {
        out.print("<a href=\""+group.getUrl()+"?"+urlAdd.substring(5) + "\">← начало</a> ");
      } else {
        out.print("<a href=\""+group.getUrl()+ "\">← начало</a> ");
      }
    } else if (!lastmod) {
      out.print("<a rel=prev rev=next href=\""+group.getUrl()+ "?offset=" + (offset + topics) + urlAdd + "\">← назад</a>");
    } else {
      out.print("<a rel=prev rev=next href=\""+group.getUrl()+"?offset=" + (offset - topics) + urlAdd + "\">← назад</a>");
    }
  }
%>
</div>
<div style="float: right">
  <%
  // ВПЕРЕД
    if (offset != 0 || firstPage) {
      if (firstPage && !lastmod) {
        out.print("<a rel=next rev=prev href=\""+group.getUrl()+"?offset=" + (pages * topics) + urlAdd + "\">архив →</a>");
      } else  if (!lastmod) {
        out.print("<a rel=next rev=prev href=\""+group.getUrl()+"?offset=" + (offset - topics) + urlAdd + "\">вперед →</a>");
      } else {
        out.print("<a rel=next rev=prev href=\""+group.getUrl()+"?offset=" + (offset + topics) + urlAdd + "\">вперед →</a>");
      }
    }
  %>
</div>
</tfoot>
</table>
</div>
<c:if test="${not lastmod and not showDeleted}">
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
        out.print("[<a href=\""+group.getUrl()+"?offset=0" + urlAdd + "\">последняя</a>] ");
      } else {
        out.print("[<b>последняя</b>] ");
      }
    } else if (i==0) {
      if (firstPage) {
        out.print("[<b>первая</b>] ");
      } else {
        if (urlAdd.length()>0) {
          out.print("[<a href=\""+group.getUrl()+"?" + urlAdd.substring(5) + "\">первая</a>] ");
        } else {
          out.print("[<a href=\""+group.getUrl()+ "\">первая</a>] ");
        }
      }
    } else if ((pages + 1 - i) * topics == offset) {
      out.print("<b>" + (pages + 1 - i) + "</b> ");
    } else {
      out.print("<a href=\""+group.getUrl()+"?offset=" + ((pages + 1 - i) * topics) + urlAdd + "\">" + (pages + 1 - i) + "</a> ");
    }
  }
%>
<p>
</div>

<% if (Template.isSessionAuthorized(session) && !showDeleted) { %>
  <hr>
  <form action="${group.url}" method=POST>
  <input type=hidden name=deleted value=1>
  <input type=submit value="Показать удаленные сообщения">
  </form>
  <hr>
<% } %>

</c:if>

<%
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
