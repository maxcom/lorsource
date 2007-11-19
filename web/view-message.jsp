<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.Statement,javax.servlet.http.HttpServletResponse,ru.org.linux.site.*,ru.org.linux.util.StringUtil" errorPage="/error.jsp" buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
    int msgid = tmpl.getParameters().getInt("msgid");

    String mainurl = "view-message.jsp?msgid=" + msgid;

    boolean showDeleted = request.getParameter("deleted") != null;

    int filterMode = CommentViewer.FILTER_NONE;

    if (!tmpl.getProf().getBoolean("showanonymous")) {
      filterMode += CommentViewer.FILTER_ANONYMOUS;
    }

    if (!tmpl.getProf().getBoolean("showignored")) {
      filterMode += CommentViewer.FILTER_IGNORED;
    }

    db = tmpl.getConnection();

    String nick = Template.getNick(session);

    if (nick == null || IgnoreList.getIgnoreListHash(db, nick).isEmpty()) {
      filterMode = filterMode & ~CommentViewer.FILTER_IGNORED;
    }

    int defaultFilterMode = filterMode;

    if (request.getParameter("filter") != null) {
      filterMode = CommentViewer.parseFilterChain(request.getParameter("filter"));
    }

    Statement st = db.createStatement();

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

    int npage = 0;
    if (request.getParameter("page") != null) {
      npage = tmpl.getParameters().getInt("page");
    }

    if (showDeleted) {
      npage = -1;
    }

    Message message = new Message(db, msgid);

    if (message.isExpired() && showDeleted) {
      throw new AccessViolationException("нельзя посмотреть удаленные комментарии в устаревших темах");
    }
    if (message.isExpired() && message.isDeleted()) {
      throw new AccessViolationException("нельзя посмотреть устаревшие удаленные сообщения");
    }
    if (message.isDeleted() && !Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Сообщение удалено");
    }

    out.print("<title>" + message.getSectionTitle() + " - " + message.getGroupTitle() + " - " + message.getTitle() + "</title>");
    out.print("<link rel=\"parent\" title=\"" + message.getSectionTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">");

// count last modified time
    if (!tmpl.isDebugMode() && !message.isDeleted() && !showDeleted && message.getLastModified() != null) {
      response.setDateHeader("Last-Modified", message.getLastModified().getTime());
    }

    if (message.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    }

    Message prevMessage = message.getPreviousMessage(db);
    Message nextMessage = message.getNextMessage(db);

    if (prevMessage != null) {
      out.print("<link rel=\"Previous\" href=\"" + prevMessage.getLinkLastmod(true) + "\" title=\"" + StringUtil.makeTitle(prevMessage.getTitle()) + "\">");
    }

    if (nextMessage != null) {
      out.print("<link rel=\"Next\" href=\"" + nextMessage.getLinkLastmod(true) + "\" title=\"" + StringUtil.makeTitle(nextMessage.getTitle()) + "\">");
    }
%>
<LINK REL="alternate" TITLE="Comments RSS" HREF="topic-rss.jsp?topic=<%= msgid %>" TYPE="application/rss+xml">
<%= tmpl.DocumentHeader() %>

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

<%
  if (!showDeleted) {
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
  }
%>
    </td>
  </table>
</form>

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
              <td>
<%
  if (prevMessage != null) {
    if (scroll == Section.SCROLL_GROUP) {
      out.print("<a href=\"" + prevMessage.getLinkLastmod(true) + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()));
    } else {
      out.print("<a href=\"" + prevMessage.getLinkLastmod(true) + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()) + " (" + prevMessage.getGroupTitle() + ')');
    }
  }
%>
              </td>
            </tr>
          </table>
        </td>
<!--        <td align=center valign=middle>
          <table>
            <tr valign=middle>
              <td>
                <a title="<%=  message.getSectionTitle() + " - " + message.getGroupTitle() %>"
                   href="group.jsp?group=<%= message.getGroupId() %>">
                  <%= message.getSectionTitle() + " - " + message.getGroupTitle() %>
                </a>
              </td>
            </tr>
          </table> -->
        <td align=left valign=middle width="35%">
          <table width="100%">
            <tr valign=middle align=right>
              <td>
<%
  if (nextMessage != null) {
    if (scroll == Section.SCROLL_GROUP) {
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + "</td><td align=right valign=middle><a href=\"" + nextMessage.getLinkLastmod(true) + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    } else {
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + " (" + nextMessage.getGroupTitle() + ")</td><td valign=middle align=right><a href=\"" + nextMessage.getLinkLastmod(true) + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
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
      out.print("<a href=\"" + prevMessage.getLinkLastmod(true) + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()));
    } else {
      out.print("<a href=\"" + prevMessage.getLinkLastmod(true) + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()) + " (" + prevMessage.getGroupTitle() + ')');
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
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + "</td><td align=right valign=middle><a href=\"" + nextMessage.getLinkLastmod(true) + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    } else {
      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + " (" + nextMessage.getGroupTitle() + ")</td><td valign=middle align=right><a href=\"" + nextMessage.getLinkLastmod(true) + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
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

<c:if test="<%= showDeleted %>">
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
    out.print(message.printMessage(tmpl, db, true, Template.getNick(session), tmpl.getParameters().getInt("highlight")));
  } else {
    out.print(message.printMessage(tmpl, db, true, Template.getNick(session)));
  }
%>

<c:out value="${scroller}" escapeXml="false"/>

<% if (!Template.isSessionAuthorized(session)) { %>
<div style="text-align: center; margin-top: 1em">
<script type="text/javascript"><!--
google_ad_client = "pub-6069094673001350";
google_ad_width = 728;
google_ad_height = 90;
google_ad_format = "728x90_as";
google_ad_type = "text_image";
//2007-06-29: lor-messages
google_ad_channel = "0949716006";
google_color_border = "808080";
google_color_bg = "000030";
google_color_link = "FFFFFF";
google_color_text = "C8C8C8";
google_color_url = "999999";
google_ui_features = "rc:0";
//-->
</script>
<script type="text/javascript"
  src="http://pagead2.googlesyndication.com/pagead/show_ads.js">
</script>
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

      if (tmpl.getProf().getBoolean("newfirst"))
        out.print("сообщения отсортированы в порядке убывания даты их написания");
      else
        out.print("сообщения отсортированы в порядке возрастания даты их написания");

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

<% if (Template.isSessionAuthorized(session) && !message.isExpired() && !showDeleted) { %>
<hr>
<form action="view-message.jsp" method=POST>
<input type=hidden name=msgid value="<%= msgid %>">
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<!--
<p>
<i>Последнее обновление дискуссии: <%= Template.dateFormat.format(message.getLastModified()) %> </i>
-->
<%
   st.close();

  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
