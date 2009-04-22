<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.util.Set,ru.org.linux.site.*,ru.org.linux.util.ServletParameterParser,ru.org.linux.util.StringUtil"   buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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
    int msgid = (Integer) request.getAttribute("msgid");

    String mainurl = "view-message.jsp?msgid=" + msgid;

    int filterMode = CommentViewer.FILTER_IGNORED;

    if (!tmpl.getProf().getBoolean("showanonymous")) {
      filterMode += CommentViewer.FILTER_ANONYMOUS;
    }

    db = LorDataSource.getConnection();

    String nick = Template.getNick(session);

    if (nick == null || IgnoreList.getIgnoreListHash(db, nick).isEmpty()) {
      filterMode = filterMode & ~CommentViewer.FILTER_IGNORED;
    }

    int defaultFilterMode = filterMode;

    if (request.getParameter("filter") != null) {
      filterMode = CommentViewer.parseFilterChain(request.getParameter("filter"));
    }

    int npage = 0;
    if (request.getParameter("page") != null) {
      npage = new ServletParameterParser(request).getInt("page");
    }

    boolean showDeleted = (Boolean) request.getAttribute("showDeleted");

    if (showDeleted) {
      npage = -1;
    }

  Message message = (Message) request.getAttribute("message");
  Message prevMessage = (Message) request.getAttribute("prevMessage");
  Message nextMessage = (Message) request.getAttribute("nextMessage"); 
%>

<title>${message.sectionTitle} - ${message.groupTitle} - ${message.title}</title>
<link rel="parent" title="${message.sectionTitle} - ${message.groupTitle}" href="group.jsp?group=${message.groupId}">
<c:if test="${prevMessage != null}">
  <link rel="Previous" id="PrevLink" href="${fn:escapeXml(prevMessage.linkLastmod)}" title="<%= StringUtil.makeTitle(prevMessage.getTitle()) %>">
</c:if>

<c:if test="${nextMessage != null}">
  <link rel="Next" id="NextLink" href="${fn:escapeXml(nextMessage.linkLastmod)}" title="<%= StringUtil.makeTitle(nextMessage.getTitle()) %>">
</c:if>

<LINK REL="alternate" TITLE="Comments RSS" HREF="view-message.jsp?msgid=<%= msgid %>&amp;output=rss" TYPE="application/rss+xml">
<script src="/js/jquery.hotkeys-0.7.8-packed.js" type="text/javascript"></script>
<script type="text/javascript">
  <!--
  $(document).bind('keydown', 'Ctrl+left',function(){ jump(document.getElementById ('PrevLink')) });
  $(document).bind('keydown', 'Ctrl+right',function(){ jump(document.getElementById ('NextLink')) });
  -->
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<div class=messages>

<form method="GET" action="view-message.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
  <table class=nav>
  <tr>
  <td align=left valign=middle>
    <a href="view-section.jsp?section=<%= message.getSectionId() %>"><%= message.getSectionTitle() %></a> -
    <a href="group.jsp?group=<%= message.getGroupId()%>"><%= message.getGroupTitle() %></a>
  </td>

    <td align=right>
      [<a href="view-message.jsp?msgid=<%= msgid %>&amp;output=rss">RSS</a>]

      <c:if test="${!showDeleted}">
<%
    if (npage != 0) {
      out.print("<input type=hidden name=page value=\"" + npage + "\">");
    }

    if (!tmpl.isUsingDefaultProfile()) {
      out.print(" [<a href=\"ignore-list.jsp\">Фильтр</a>]");
    }

    out.print(" <select name=\"filter\" onChange=\"submit()\">");
    out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_NONE) + '\"' + (filterMode == CommentViewer.FILTER_NONE ? " selected=\"selected\"" : "") + ">все комментарии</option>");
    out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_ANONYMOUS) + '\"' + (filterMode == CommentViewer.FILTER_ANONYMOUS ? " selected=\"selected\"" : "") + ">без анонимных</option>");

    if (!tmpl.isUsingDefaultProfile()) {
      out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_IGNORED) + '\"' + (filterMode == CommentViewer.FILTER_IGNORED ? " selected=\"selected\"" : "") + ">без игнорируемых</option>");
      out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_LISTANON) + '\"' + (filterMode == CommentViewer.FILTER_LISTANON ? " selected=\"selected\"" : "") + ">без анонимных и игнорируемых</option>");
    }

    out.print("</select>");
%>
      </c:if>
    </td>
  </table>
</form>
<c:set var="scrollGroup" value="<%= Section.getScrollMode(message.getSectionId())==Section.SCROLL_GROUP %>"/>

<c:set var="scroller">
<%
  int scroll = Section.getScrollMode(message.getSectionId());

  if (prevMessage == null && nextMessage == null) {
    scroll = Section.SCROLL_NOSCROLL;
  }

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
    <table class=nav>
      <tr>
        <td align=left valign=middle width="35%">
          <table>
            <tr valign=middle>
                <c:if test="${prevMessage != null}">
                  <td style="padding-right: 5px">
                    <a href="${fn:escapeXml(prevMessage.linkLastmod)}" rel=prev rev=next>←</a>
                  </td>
                  <td align=left valign=top>
                    <a href="${fn:escapeXml(prevMessage.linkLastmod)}" rel=prev rev=next>
                    <%= StringUtil.makeTitle(prevMessage.getTitle()) %></a>
                    <c:if test="${!scrollGroup}">
                      (${prevMessage.groupTitle})
                    </c:if>
                  </td>
                </c:if>
            </tr>
          </table>
        </td>
        <td align=left valign=middle width="35%">
          <table width="100%">
            <tr valign=middle align=right>
              <c:if test="${nextMessage != null}">
                <td>
                  <a href="${fn:escapeXml(nextMessage.linkLastmod)}" rel=next rev=prev>
                  <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
                  </a>
                  <c:if test="${!scrollGroup}">
                    (${nextMessage.groupTitle})
                  </c:if>
                </td>
                <td align="right" valign="middle" style="padding-left: 5px">
                  <a href="${fn:escapeXml(nextMessage.linkLastmod)}" rel=next rev=prev>→</a>
                </td>
              </c:if>
            </tr>
          </table>
        </td>
      </tr>
    </table>
<%
   }
%>

</c:set>

<c:set var="bottomScroller">
<%
  int scroll = Section.getScrollMode(message.getSectionId());

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
    <table class=nav>
      <tr>
        <td align=left valign=middle width="35%">
          <table>
            <tr valign=middle>
                <c:if test="${prevMessage != null}">
                  <td style="padding-right: 5px">
                    <a href="${fn:escapeXml(prevMessage.linkLastmod)}" rel=prev rev=next>←</a>
                  </td>
                  <td align=left valign=top>
                    <a href="${fn:escapeXml(prevMessage.linkLastmod)}" rel=prev rev=next>
                    <%= StringUtil.makeTitle(prevMessage.getTitle()) %></a>
                    <c:if test="${!scrollGroup}">
                      (${prevMessage.groupTitle})
                    </c:if>
                  </td>
                </c:if>
            </tr>
          </table>
        </td>
        <td align=center valign=middle>
          <table>
            <tr valign=middle>
              <td>
                <a title="<%=  message.getSectionTitle() + " - " + message.getGroupTitle() %>"
                   href="group.jsp?group=<%= message.getGroupId() %>">
                  <%= message.getSectionTitle() + " - " + message.getGroupTitle() %>
                </a>
              </td>
            </tr>
          </table>
        <td align=left valign=middle width="35%">
          <table width="100%">
            <tr valign=middle align=right>
              <c:if test="${nextMessage != null}">
                <td>
                  <a href="${fn:escapeXml(nextMessage.linkLastmod)}" rel=next rev=prev>
                  <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
                  </a>
                  <c:if test="${!scrollGroup}">
                    (${nextMessage.groupTitle})
                  </c:if>
                </td>
                <td align="right" valign="middle" style="padding-left: 5px">
                  <a href="${fn:escapeXml(nextMessage.linkLastmod)}" rel=next rev=prev>→</a>
                </td>
              </c:if>
            </tr>
          </table>
        </td>
      </tr>
    </table>
<%
   }
%>

</c:set>

<c:if test="${showDeleted}">
<%
  out.print("<h1>Режим показа удаленных комментариев</h1>");
%>
</c:if>

<%
  int messages = tmpl.getProf().getInt("messages");
  int pages = message.getPageCount(messages);

  String pageInfo = null;
  if (pages > 1) {
    StringBuilder bufInfo = new StringBuilder();

    bufInfo.append("[страница");

    String linkurl = mainurl;

    if (npage!=-1 && npage!=0) {
      bufInfo.append("<a href=\"").append(linkurl).append("&amp;page=").append(npage-1).append("\">");
      bufInfo.append("&emsp;←");
      bufInfo.append("</a>");
    } else {
      bufInfo.append("&emsp;←");
    }

    for (int i = 0; i < pages; i++) {
      bufInfo.append(' ');

      if ((i==pages-1) && !(message.isExpired()) ) {
        linkurl += "&amp;lastmod="+message.getLastModified().getTime();
      }

      if (i != npage) {
        if (i>0) {
          bufInfo.append("<a href=\"").append(linkurl).append("&amp;page=").append(i);
        } else {
          bufInfo.append("<a href=\"").append(linkurl);
        }

        if (filterMode!=defaultFilterMode) {
          bufInfo.append("&filter=").append(CommentViewer.toString(filterMode));
        }

        bufInfo.append("\">").append(i + 1).append("</a>");
      } else {
        bufInfo.append("<strong>").append(i + 1).append("</strong>");
      }
    }

    if (npage!=-1 && npage+1!=pages) {
      bufInfo.append("<a href=\"").append(linkurl).append("&amp;page=").append(npage+1).append("\">");
      bufInfo.append(" →");
      bufInfo.append("</a>");        
    } else {
      bufInfo.append(" →");
    }

    if (Template.isSessionAuthorized(session)) {
      if (npage!=-1) {
        bufInfo.append("&emsp;<a href=\"").append(linkurl).append("&amp;page=-1").append("\">все").append("</a>");
      } else {
        bufInfo.append("&emsp;<strong>все").append("</strong>");      
      }
    }

    bufInfo.append(']');
    pageInfo = bufInfo.toString();
  }

  if (request.getParameter("highlight") != null) {
%>
<lor:message db="<%= db %>" message="<%= message %>" showMenu="true" user="<%= nick %>" highlight="<%= new ServletParameterParser(request).getInt(&quot;highlight&quot;)%>"/>
<%
  } else {
%>
<lor:message db="<%= db %>" message="<%= message %>" showMenu="true" user="<%= nick %>"/>
<%
  }
%>

<c:out value="${scroller}" escapeXml="false"/>

<% if (!Template.isSessionAuthorized(session)) { %>
<div style="text-align: center; margin-top: 1em">
  <jsp:include page="/WEB-INF/jsp/adsense.jsp"/>
</div><br>
<% } %>

<c:if test="${message.commentEnabled}">
<%
    int offset = 0;
    int limit = 0;
    boolean reverse = tmpl.getProf().getBoolean("newfirst");

    if (npage != -1) {
      limit = messages;
      offset = messages * npage;
    }

    CommentList comments = (CommentList) request.getAttribute("comments");
    Set<Integer> hideSet = CommentList.makeHideSet(db, comments, filterMode, nick);

    CommentViewer cv = new CommentViewer(tmpl, db, comments, Template.getNick(session), message.isExpired());

    if (tmpl.getProf().getBoolean("sortwarning") && cv.getOutputCount()>0) {
      out.print("<div class=nav>");

      if (tmpl.getProf().getBoolean("newfirst")) {
        out.print("сообщения отсортированы в порядке убывания даты их написания");
      } else {
        out.print("сообщения отсортированы в порядке возрастания даты их написания");
      }

      out.print("</div>");
    }
%>
  <c:if test="<%= pageInfo!=null %>">
    <div class="nav">
      <%= pageInfo %>
    </div>
  </c:if>
  <div class="comment">
    <%= cv.show(reverse, offset, limit, hideSet) %>
  </div>
<c:if test="<%= cv.getOutputCount()>0 %>">
  <c:if test="<%= pageInfo!=null %>">
    <div class="nav">
      <%= pageInfo %>
    </div>
  </c:if>

  <c:out value="${bottomScroller}" escapeXml="false"/>
</c:if>
</c:if>
</div>

<% if (Template.isSessionAuthorized(session) && (!message.isExpired() || tmpl.isModeratorSession()) && !showDeleted) { %>
<hr>
<form action="view-message.jsp" method=POST>
<input type=hidden name=msgid value="<%= msgid %>">
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>

<div align=center>
  <iframe src="dw.jsp?width=728&amp;height=90&amp;main=0" width="728" height="90" scrolling="no" frameborder="0"></iframe>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
