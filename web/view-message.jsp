<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.Statement,java.sql.Timestamp,javax.servlet.http.HttpServletResponse,ru.org.linux.site.*" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
    int msgid = tmpl.getParameters().getInt("msgid");

    int npage = -1;
    if (request.getParameter("page") != null) {
      npage = tmpl.getParameters().getInt("page");
    }

    String returnUrl = "view-message.jsp?msgid=" + msgid;
    String mainurl = returnUrl;
    if (npage != -1) {
      returnUrl += "&page=" + npage;
    }

    boolean showDeleted = request.getParameter("deleted") != null;
    boolean showAnonymous = tmpl.getProf().getBoolean("showanonymous");
    String showAnonymousParam = null;

    if (request.getParameter("anonymous") != null) {
      if ("show".equals(request.getParameter("anonymous"))) {
        showAnonymous = true;
        showAnonymousParam = "show";
      } else if ("hide".equals(request.getParameter("anonymous"))) {
        showAnonymous = false;
        showAnonymousParam = "hide";
      }
    }

    db = tmpl.getConnection("view-message");
    Statement st = db.createStatement();

    if (showDeleted && !"POST".equals(request.getMethod())) {
      response.setHeader("Location", tmpl.getRedirectUrl() + "view-message.jsp?msgid=" + msgid);
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

      showDeleted = false;
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized(session)) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    Message message = new Message(db, msgid);

    if (message.isExpired() && showDeleted) {
      throw new AccessViolationException("нельзя посмотреть удаленные комментарии в устаревших темах");
    }
    if (message.isExpired() && message.isDeleted()) {
      throw new AccessViolationException("нельзя посмотреть устаревшие удаленные сообщения");
    }
    if (message.isDeleted() && !tmpl.isSessionAuthorized(session)) {
      throw new AccessViolationException("Сообщение удалено");
    }

    out.print("<title>" + message.getPortalTitle() + " - " + message.getGroupTitle() + " - " + message.getTitle() + "</title>");
    out.print("<link rel=\"parent\" title=\"" + message.getPortalTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">");

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
      out.print("<link rel=\"Previous\" href=\"jump-message.jsp?msgid=" + prevMessage.getMessageId() + "&amp;lastmod=" + prevMessage.getLastModified().getTime() + "\" title=\"" + StringUtil.makeTitle(prevMessage.getTitle()) + "\">");
    }

    if (nextMessage != null) {
      out.print("<link rel=\"Next\" href=\"jump-message.jsp?msgid=" + nextMessage.getMessageId() + "&amp;lastmod=" + nextMessage.getLastModified().getTime() + "\" title=\"" + StringUtil.makeTitle(nextMessage.getTitle()) + "\">");
    }
%>
<%= tmpl.DocumentHeader() %>

<div class=messages>

<%
  int scroll = Section.getScrollMode(message.getSectionId());

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
<div class=nav>
  <div class="color1">
    <table width="100%" cellspacing=1 cellpadding=0 border=0>
      <tr class=body>
<%
  if (scroll == Section.SCROLL_GROUP) {
    out.print("<td align=left valign=middle width=\"35%\"><table><tr valign=middle><td>");

    if (prevMessage != null) {
      Timestamp lastmod = prevMessage.getLastModified();

      out.print("<a href=\"jump-message.jsp?msgid=" + prevMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()));
    }

    out.print("</td></table></td>");

    out.print("<td align=center valign=middle><table><tr valign=middle><td><a title=\"" + message.getPortalTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">" + message.getPortalTitle() + " - " + message.getGroupTitle() + "</a></td></tr></table>");

    out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle align=right><td>");

    if (nextMessage != null) {
      Timestamp lastmod = nextMessage.getLastModified();

      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + "</td><td align=right valign=middle><a href=\"jump-message.jsp?msgid=" + nextMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    }

    out.print("</td></table></td>");
  } else if (scroll == Section.SCROLL_SECTION) {
    out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle><td>");

    if (prevMessage != null) {
      Timestamp lastmod = prevMessage.getLastModified();
      if (lastmod == null) lastmod = new Timestamp(0);

      out.print("<a href=\"jump-message.jsp?msgid=" + prevMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()) + " (" + prevMessage.getGroupTitle() + ')');
    }

    out.print("</td></table></td>");

    out.print("<td align=center valign=middle><table><tr valign=middle><td><a title=\"" + message.getPortalTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">" + message.getPortalTitle() + " - " + message.getGroupTitle() + "</a></td></tr></table>");

    out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle align=right><td>");
    if (nextMessage != null) {
      Timestamp lastmod = nextMessage.getLastModified();
      if (lastmod == null) lastmod = new Timestamp(0);

      out.print(StringUtil.makeTitle(nextMessage.getTitle()) + " (" + nextMessage.getGroupTitle() + ")</td><td valign=middle align=right><a href=\"jump-message.jsp?msgid=" + nextMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
    }

    out.print("</td></table></td>");
  }
%>
      </tr>
    </table>
  </div>
</div>
<%
   }
%>

<h1><%= message.getTitle() %>
<% if (showDeleted) out.print("<br>Режим показа удаленных комментариев"); %>
</h1>

<%
  boolean comment = message.isCommentEnabled();
  int messages = tmpl.getProf().getInt("messages");
  int pages = message.getPageCount(messages);

  String pageInfo = null;
  if (pages > 1) {
    StringBuffer bufInfo = new StringBuffer();

    bufInfo.append("[страница");

    for (int i = 0; i < pages; i++) {
      bufInfo.append(' ');

      if (i != npage) {
        bufInfo.append("<a href=\"").append(mainurl).append("&page=").append(i);
        if (showAnonymousParam != null) {
          bufInfo.append("&anonymous=").append(showAnonymousParam);
        }
        bufInfo.append("\">").append(i + 1).append("</a>");
      } else {
        bufInfo.append("<strong>").append(i + 1).append("</strong>");
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

<% if (!Template.isSessionAuthorized(session)) { %>
<div style="text-align: center">
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

<%
  if (comment) {
    out.print("<div class=comment>");
    if (tmpl.getProf().getBoolean("sortwarning")) {
      out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body><td align=\"center\">");

      if (tmpl.getProf().getBoolean("newfirst"))
        out.print("сообщения отсортированы в порядке убывания даты их написания");
      else
        out.print("сообщения отсортированы в порядке возрастания даты их написания");

      out.print("</td></tr></table></div></div>");
    }

    if (!showDeleted && message.getCommentCount() > 0) {
      out.print("<form method=\"GET\" action=\"view-message.jsp\">");
      out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body><td>");

      out.print("<div align=\"center\">");
      out.print("<input type=hidden name=msgid value=\"" + msgid + "\">");
      if (npage!=-1) {
        out.print("<input type=hidden name=page value=\"" + npage + "\">");
      }
      out.print("фильтр комментариев: <select name=\"anonymous\">");
      out.print("<option value=\"show\"" + (showAnonymous ? " selected=\"selected\"" : "") + ">все комментарии</option>");
      out.print("<option value=\"hide\"" + (showAnonymous ? "" : " selected=\"selected\"") + ">без анонимных комментариев и ответов на них</option>");
      out.print("</select>");
      out.print(" <input type=\"submit\" value=\"Обновить\">");
      out.print("</div>");
      out.print("</td></tr></table></div></div>");
      out.print("</form>");
    }

    if (pageInfo != null) {
      out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body><td>");
      out.print("<div align=\"center\">");
      out.print(pageInfo);
      out.print("</div>");
      out.print("</td></tr></table></div></div>");
    }

    int offset = 0;
    int limit = 0;
    boolean reverse = tmpl.getProf().getBoolean("newfirst");

    if (npage != -1) {
      limit = messages;
      offset = messages * npage;
    }

    CommentList comments = CommentList.getCommentList(tmpl, db, message, showDeleted); 

    String urladd = "&amp;return=" + URLEncoder.encode(returnUrl);

    CommentViewer cv = new CommentViewer(tmpl, comments, urladd, Template.getNick(session), message.isExpired());

    if (!showAnonymous)
      out.print(cv.showFiltered(reverse, offset, limit));
    else
      out.print(cv.showAll(reverse, offset, limit));

    out.print("</div>");

    if (pageInfo != null) {
      out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body><td>");
      out.print("<div align=\"center\">");
      out.print(pageInfo);
      out.print("</div>");
      out.print("</td></tr></table></div></div>");
    }
  }
%>
<%
  if (scroll != Section.SCROLL_NOSCROLL) {
    out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body>");

    if (scroll == Section.SCROLL_GROUP) {
      out.print("<td align=left valign=middle width=\"35%\"><table><tr valign=middle><td>");

      if (prevMessage != null) {
        Timestamp lastmod = prevMessage.getLastModified();

        out.print("<a href=\"jump-message.jsp?msgid=" + prevMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()));
      }

      out.print("</td></table></td>");

      out.print("<td align=center valign=middle><table><tr valign=middle><td><a title=\"" + message.getPortalTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">" + message.getPortalTitle() + " - " + message.getGroupTitle() + "</a></td></tr></table>");

      out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle align=right><td>");

      if (nextMessage != null) {
        Timestamp lastmod = nextMessage.getLastModified();

        out.print(StringUtil.makeTitle(nextMessage.getTitle()) + "</td><td align=right valign=middle><a href=\"jump-message.jsp?msgid=" + nextMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
      }

      out.print("</td></table></td>");
    } else if (scroll == Section.SCROLL_SECTION) {
      out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle><td>");

      if (prevMessage != null) {
        Timestamp lastmod = prevMessage.getLastModified();
        if (lastmod == null) lastmod = new Timestamp(0);

        out.print("<a href=\"jump-message.jsp?msgid=" + prevMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rel=prev rev=next>&lt;&lt;&lt;</a></td><td align=left valign=top>" + StringUtil.makeTitle(prevMessage.getTitle()) + " (" + prevMessage.getGroupTitle() + ')');
      }

      out.print("</td></table></td>");

      out.print("<td align=center valign=middle><table><tr valign=middle><td><a title=\"" + message.getPortalTitle() + " - " + message.getGroupTitle() + "\" href=\"group.jsp?group=" + message.getGroupId() + "\">" + message.getPortalTitle() + " - " + message.getGroupTitle() + "</a></td></tr></table>");

      out.print("<td align=left valign=middle width=\"35%\"><table width=\"100%\"><tr valign=middle align=right><td>");
      if (nextMessage != null) {
        Timestamp lastmod = nextMessage.getLastModified();
        if (lastmod == null) lastmod = new Timestamp(0);

        out.print(StringUtil.makeTitle(nextMessage.getTitle()) + " (" + nextMessage.getGroupTitle() + ")</td><td valign=middle align=right><a href=\"jump-message.jsp?msgid=" + nextMessage.getMessageId() + "&amp;lastmod=" + lastmod.getTime() + "\" rev=prev rel=next>&gt;&gt;&gt;</a>");
      }

      out.print("</td></table></td>");
    }
    out.print("</tr></table></div></div>");
  }
%>



</div>

<% if (tmpl.isSessionAuthorized(session) && !message.isExpired() && !showDeleted) { %>
<hr>
<form action="view-message.jsp" method=POST>
<input type=hidden name=msgid value=<%= msgid %>>
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<p>
<i>
<% String masterUrl = "http://www.linux.org.ru/jump-message.jsp?msgid="+msgid; %>
Пожалуйста, для ссылок на дискуссию используйте URL: <br>
<a href="<%= masterUrl %>"><%= masterUrl %></a></i>
<p>
<i>Последнее обновление дискуссии: <%= Template.dateFormat.format(message.getLastModified()) %> </i>

<%
   st.close();

  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
