<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,javax.servlet.http.HttpServletResponse,ru.org.linux.site.*,ru.org.linux.util.ServletParameterParser,ru.org.linux.util.StringUtil"   buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;

  try {
    int msgid = new ServletParameterParser(request).getInt("msgid");

    String mainurl = "view-message.jsp?msgid=" + msgid;

    boolean showDeleted = request.getParameter("deleted") != null;

    if (showDeleted && !"POST".equals(request.getMethod())) {
      response.setHeader("Location", tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid);
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

      showDeleted = false;
    }

    if (showDeleted) {
      if (!Template.isSessionAuthorized(session)) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }
 %>
  <c:set var="showDeleted" value="<%= showDeleted %>"/>

<%

    int filterMode = CommentViewer.FILTER_NONE;

    if (!tmpl.getProf().getBoolean("showanonymous")) {
      filterMode += CommentViewer.FILTER_ANONYMOUS;
    }

    if (!tmpl.getProf().getBoolean("showignored")) {
      filterMode += CommentViewer.FILTER_IGNORED;
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

    if (showDeleted) {
      npage = -1;
    }

    Message message = new Message(db, msgid);

    if (message.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
      throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
    }
    if (message.isExpired() && message.isDeleted() && !tmpl.isModeratorSession()) {
      throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
    }
    if (message.isDeleted() && !Template.isSessionAuthorized(session)) {
      throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
    }

// count last modified time
  if (!message.isDeleted() && !showDeleted && message.getLastModified() != null) {
    response.setDateHeader("Last-Modified", message.getLastModified().getTime());
  }

  if (message.isExpired()) {
    response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
  } else {
    response.setDateHeader("Expires", System.currentTimeMillis() - 24 * 60 * 60 * 1000);
  }
  %>
<c:set var="message" value="<%= message %>"/>
<c:set var="prevMessage" value="<%= message.getPreviousMessage(db) %>"/>
<c:set var="nextMessage" value="<%= message.getNextMessage(db) %>"/>

<%
  Message prevMessage = message.getPreviousMessage(db);
  Message nextMessage = message.getNextMessage(db);
%>

<title>${message.sectionTitle} - ${message.groupTitle} - ${message.title}</title>
<link rel="parent" title="${message.sectionTitle} - ${message.groupTitle}" href="group.jsp?group=${message.groupId}">
<c:if test="${prevMessage != null}">
  <link rel="Previous" href="${fn:escapeXml(prevMessage.linkLastmod)}" title="<%= StringUtil.makeTitle(prevMessage.getTitle()) %>">
</c:if>

<c:if test="${nextMessage != null}">
  <link rel="Next" href="${fn:escapeXml(nextMessage.linkLastmod)}" title="<%= StringUtil.makeTitle(nextMessage.getTitle()) %>">
</c:if>

<LINK REL="alternate" TITLE="Comments RSS" HREF="topic-rss.jsp?topic=<%= msgid %>" TYPE="application/rss+xml">
<jsp:include page="WEB-INF/jsp/header.jsp"/>
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
      [<a href="topic-rss.jsp?topic=<%= msgid %>">RSS</a>]

      <c:if test="${!showDeleted}">
<%
    if (npage != 0) {
      out.print("<input type=hidden name=page value=\"" + npage + "\">");
    }

    if (!tmpl.isUsingDefaultProfile()) {
      out.print(" [<a href=\"ignore-list.jsp\">Фильтр</a>]");
    }

    out.print(" <select name=\"filter\" onChange=\"submit()\">");
    out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_NONE) + "\"" + (filterMode == CommentViewer.FILTER_NONE ? " selected=\"selected\"" : "") + ">все комментарии</option>");
    out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_ANONYMOUS) + "\"" + (filterMode == CommentViewer.FILTER_ANONYMOUS ? " selected=\"selected\"" : "") + ">без анонимных</option>");

    if (!tmpl.isUsingDefaultProfile()) {
      out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_IGNORED) + "\"" + (filterMode == CommentViewer.FILTER_IGNORED ? " selected=\"selected\"" : "") + ">без игнорируемых</option>");
      out.print("<option value=\"" + CommentViewer.toString(CommentViewer.FILTER_LISTANON) + "\"" + (filterMode == CommentViewer.FILTER_LISTANON ? " selected=\"selected\"" : "") + ">без анонимных и игнорируемых</option>");
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

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
    <table class=nav>
      <tr>
        <td align=left valign=middle width="35%">
          <table>
            <tr valign=middle>
                <c:if test="${prevMessage != null}">
                  <td>
                    <a href="${fn:escapeXml(prevMessage.linkLastmod)}" rel=prev rev=next>&lt;&lt;&lt;</a>
                  </td>
                  <td align=left valign=top>
                    <%= StringUtil.makeTitle(prevMessage.getTitle()) %>
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
                  <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
                  <c:if test="${!scrollGroup}">
                    (${nextMessage.groupTitle})
                  </c:if>
                </td>
                <td align="right" valign="middle">
                  <a href="${fn:escapeXml(nextMessage.linkLastmod)}" rel=next rev=prev>&gt;&gt;&gt;</a>
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
              <td>
<%
  if (prevMessage != null) {
    if (scroll == Section.SCROLL_GROUP) {
      out.print("<a href=\"" + prevMessage.getLinkLastmod() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()));
    } else {
      out.print("<a href=\"" + prevMessage.getLinkLastmod() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()) + " (" + prevMessage.getGroupTitle() + ')');
    }
  }
%>
              </td>
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
              <td>
<%
  if (nextMessage != null) {
    if (scroll == Section.SCROLL_GROUP) {
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + "</td><td align=right valign=middle><a href=\"" + nextMessage.getLinkLastmod() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    } else {
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + " (" + nextMessage.getGroupTitle() + ")</td><td valign=middle align=right><a href=\"" + nextMessage.getLinkLastmod() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    }
  }
%>
              </td>
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
  boolean comment = message.isCommentEnabled();
  int messages = tmpl.getProf().getInt("messages");
  int pages = message.getPageCount(messages);

  String pageInfo = null;
  if (pages > 1) {
    StringBuffer bufInfo = new StringBuffer();

    bufInfo.append("[страница");

    String linkurl = mainurl;

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

    if (Template.isSessionAuthorized(session)) {
      if (npage!=-1) {
        bufInfo.append(" <a href=\"").append(linkurl).append("&amp;page=-1").append("\">все").append("</a>");
      } else {
        bufInfo.append(" <strong>все").append("</strong>");      
      }
    }

    bufInfo.append(']');
    pageInfo = bufInfo.toString();
  }

  if (request.getParameter("highlight") != null) {
    out.print(message.printMessage(tmpl, db, true, Template.getNick(session), new ServletParameterParser(request).getInt("highlight")));
  } else {
    out.print(message.printMessage(tmpl, db, true, Template.getNick(session)));
  }
%>

<c:out value="${scroller}" escapeXml="false"/>

<% if (!Template.isSessionAuthorized(session)) { %>
<div style="text-align: center; margin-top: 1em">
  <jsp:include page="WEB-INF/jsp/adsense.jsp"/>
</div><br>
<% } %>

<c:if test="<%= comment %>">
<%
    int offset = 0;
    int limit = 0;
    boolean reverse = tmpl.getProf().getBoolean("newfirst");

    if (npage != -1) {
      limit = messages;
      offset = messages * npage;
    }

    CommentList comments = CommentList.getCommentList(db, message, showDeleted);

    CommentViewer cv = new CommentViewer(tmpl, db, comments, Template.getNick(session), message.isExpired());

    String outputComments;

    if (filterMode != CommentViewer.FILTER_NONE) {
      outputComments = cv.showFiltered(db, reverse, offset, limit, filterMode, Template.getNick(session));
    } else {
      outputComments = cv.showAll(reverse, offset, limit);
    }

    if (tmpl.getProf().getBoolean("sortwarning") && cv.getOutputCount()>0) {
      out.print("<div class=nav>");

      if (tmpl.getProf().getBoolean("newfirst")) {
        out.print("сообщения отсортированы в порядке убывания даты их написания");
      }
      else {
        out.print("сообщения отсортированы в порядке возрастания даты их написания");
      }

      out.print("</div>");
    }
%>
  <c:if test="<%= pageInfo!=null %>">
    <div class="pageinfo">
      <%= pageInfo %>
    </div>
  </c:if>
  <div class="comment">
    <%= outputComments %>
  </div>
<c:if test="<%= cv.getOutputCount()>0 %>">
  <c:if test="<%= pageInfo!=null %>">
    <div class="pageinfo">
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

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
