<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.Group,ru.org.linux.site.LorDataSource,ru.org.linux.site.Template,ru.org.linux.site.User"   buffer="200kb"%>
<%@ page import="ru.org.linux.spring.GroupController" %>
<%@ page import="ru.org.linux.util.BadImageException" %>
<%@ page import="ru.org.linux.util.DateUtil" %>
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

<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.spring.TopicsListItem>"--%>
<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>
<%--@elvariable id="firstPage" type="java.lang.Boolean"--%>
<%--@elvariable id="groupList" type="java.util.List<ru.org.linux.site.Group>"--%>
<%--@elvariable id="lastmod" type="java.lang.Boolean"--%>
<%--@elvariable id="count" type="java.lang.Integer"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="showDeleted" type="java.lang.Boolean"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="year" type="java.lang.Integer"--%>
<%--@elvariable id="month" type="java.lang.Integer"--%>
<%--@elvariable id="url" type="java.lang.String"--%>

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
    boolean showIgnored = (Boolean) request.getAttribute("showIgnored");

    boolean firstPage = (Boolean) request.getAttribute("firstPage");
    int offset = (Integer) request.getAttribute("offset");

    Group group = (Group) request.getAttribute("group");

    int count = (Integer) request.getAttribute("count");
    int topics = tmpl.getProf().getInt("topics");
    Integer year = (Integer) request.getAttribute("year");
    String url = (String) request.getAttribute("url");

    int pages = count / topics;
    if (count % topics != 0) {
      count = (pages + 1) * topics;
    }

    response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
%>
<title>${group.sectionName} - ${group.title}
  <c:if test="${year != null}">
    - Архив ${year}, <%= DateUtil.getMonth((Integer) request.getAttribute("month")) %>
  </c:if>
</title>
    <LINK REL="alternate" HREF="/section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}" TYPE="application/rss+xml">
    <link rel="parent" title="${group.title}" href="${group.sectionLink}">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<form>
  <table class=nav>
    <tr>
    <td align=left valign=middle id="navPath">
      <a href="${group.sectionLink}">${group.sectionName}</a> - ${group.title}
      <c:if test="${year != null}">
        - Архив ${year}, <%= DateUtil.getMonth((Integer) request.getAttribute("month")) %>
      </c:if>

    </td>

    <td align=right valign=middle>
      [<a href="${group.url}archive/">Архив</a>]
      <c:if test="${year==null}">
      <% if (tmpl.isModeratorSession()) { %>
        [<a href="groupmod.jsp?group=${group.id}">Править группу</a>]
      <% } %>
      [<a href="/wiki/en/lor-faq">FAQ</a>]
<%
  User currentUser = tmpl.getCurrentUser();

  if (group.isTopicPostingAllowed(currentUser)) {
%>
      [<a href="add.jsp?group=${group.id}">Добавить сообщение</a>]
<%
  }
%>
  [<a href="section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}">RSS</a>]
      <select name=group onchange="goto(this);" title="Быстрый переход">
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
      </c:if>
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

  db = LorDataSource.getConnection();
  db.setAutoCommit(false);
  
%>
<lor:groupinfo group="${group}" db="<%= db %>"/>
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr>
  <th>Тема<br>
    <form action="${url}" method="GET" style="font-weight: normal; display: inline;">
      фильтр:
      <c:if test="${lastmod}">
        <input type=hidden name=lastmod value=true>
      </c:if>
      <% if (!firstPage) { %>
        <input type=hidden name=offset value="${offset}">
      <% } %>
        <select name="showignored" onchange="submit();">
          <option value="t" <%= (showIgnored?"selected":"") %>>все темы</option>
          <option value="f" <%= (showIgnored?"":"selected") %>>без игнорируемых</option>
          </select> [<a style="text-decoration: underline" href="ignore-list.jsp">настроить</a>]
    </form>
  </th>
  <th>Последнее сообщение<br>
    <c:if test="${year==null}">
    <c:if test="${lastmod}">
      <span style="font-weight: normal">[<a href="${url}" style="text-decoration: underline">отменить</a>]</span>
    </c:if>
    <c:if test="${not lastmod}">
      <span style="font-weight: normal">[<a href="${url}?lastmod=true" style="text-decoration: underline">упорядочить</a>]</span>
    </c:if>
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
      <c:choose>
        <c:when test="${template.moderatorSession}">
          <a href="/undelete.jsp?msgid=${topic.msgid}"><img src="/img/del.png" border="0" alt="[X]" width="15" height="15"></a>
        </c:when>
        <c:otherwise>
          <img src="/img/del.png" border="0" alt="[X]" width="15" height="15">
        </c:otherwise>
      </c:choose>
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
      <c:forEach var="i" begin="1" end="${topic.pages-1}"> <c:if test="${i==(topic.pages-1) and firstPage and year==null}"><a href="${group.url}${topic.msgid}/page${i}?lastmod=${topic.lastmod.time}">${i+1}</a></c:if><c:if test="${i!=(topic.pages-1) or not firstPage or year!=null}"><a href="${group.url}${topic.msgid}/page${i}">${i+1}</a></c:if></c:forEach>)
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

  if (offset - topics > 0) {
    out.print("<a rel=prev rev=next href=\"" + url + "?offset=" + (offset - topics) + urlAdd + "\">← назад</a>");
  } else if (offset - topics == 0) {
    out.print("<a rel=prev rev=next href=\"" + url + (urlAdd.length() > 0 ? ('?' + urlAdd.substring(5)) : "") + "\">← назад</a>");
  }
%>
</div>
<div style="float: right">
  <%
    if (offset + topics < count) {
      if (year!=null || (offset+topics) < GroupController.MAX_OFFSET) {
        out.print("<a rel=next rev=prev href=\"" + url + "?offset=" + (offset + topics) + urlAdd + "\">вперед →</a>");
      }
    }
  %>
</div>
</tfoot>
</table>
</div>
<c:if test="${not lastmod and not showDeleted and year==null and template.sessionAuthorized}">
  <hr>
  <form action="${url}" method=POST>
  <input type=hidden name=deleted value=1>
  <input type=submit value="Показать удаленные сообщения">
  </form>
  <hr>
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