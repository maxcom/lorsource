<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.File,java.io.IOException,java.net.URLEncoder,java.sql.*,java.util.*,java.util.Date,java.util.logging.Logger,javax.mail.Session"   buffer="200kb"%>
<%@ page import="javax.mail.Transport"%>
<%@ page import="javax.mail.internet.InternetAddress"%>
<%@ page import="javax.mail.internet.MimeMessage"%>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.storage.StorageNotFoundException" %>
<%@ page import="ru.org.linux.util.*" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {
    int groupId = Integer.parseInt(request.getParameter("group"));
    boolean showDeleted = request.getParameter("deleted") != null;

    if (showDeleted && !"POST".equals(request.getMethod())) {
      response.setHeader("Location", tmpl.getMainUrl() + "/group.jsp?group=" + groupId);
      response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

      showDeleted = false;
    }

    if (showDeleted && !Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Вы не авторизованы");
    }

    boolean showIgnored = false;
    if (request.getParameter("showignored") != null) {
      showIgnored = "t".equals(request.getParameter("showignored"));
    }

    if (request.getParameter("group") == null) {
      throw new MissingParameterException("group");
    }

    boolean firstPage;
    int offset;

    if (request.getParameter("offset") != null) {
      offset = Integer.parseInt(request.getParameter("offset"));
      firstPage = false;
    } else {
      firstPage = true;
      offset = 0;
    }

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Group group = new Group(db, groupId);

    Statement st = db.createStatement();

    int count = group.calcTopicsCount(db, showDeleted);
    int topics = tmpl.getProf().getInt("topics");

    int pages = count / topics;
    if (count % topics != 0) {
      count = (pages + 1) * topics;
    }

    if (group.getSectionId() == 0) {
      throw new BadGroupException();
    }

    Section section = new Section(db, group.getSectionId());

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
<%
    out.print("<link rel=\"parent\" title=\"" + group.getTitle() + "\" href=\"view-section.jsp?section=" + group.getSectionId() + "\">");
%>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<form action="group.jsp">
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
      <select name=group onChange="submit()" title="Быстрый переход">
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

<%
  String ignq = "";

  Map<Integer,String> ignoreList = IgnoreList.getIgnoreListHash(db, (String) session.getValue("nick"));

  if (!showIgnored && Template.isSessionAuthorized(session) && !session.getValue("nick").equals("anonymous")) {
    if (firstPage && ignoreList != null && !ignoreList.isEmpty()) {
      ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list, users WHERE userid=users.id and nick='" + session.getValue("nick") + "')";
    }
  }

  out.print("<h1>");

  out.print(group.getSectionName() + ": " + group.getTitle() + "</h1>");

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

  String des = group.getInfo();
  if (des != null) {
    out.print("<p style=\"margin-top: 0px\"><em>");
    out.print(des);
    out.print("</em></p>");
  }
%>
<form action="group.jsp" method="GET">

  <input type=hidden name=group value=<%= groupId %>>
  <!-- input type=hidden name=deleted value=<%= (showDeleted?"t":"f")%> -->
  <% if (!firstPage) { %>
    <input type=hidden name=offset value="<%= offset %>">
  <% } %>
  <div class=nav>
    фильтр тем: <select name="showignored">
      <option value="t" <%= (showIgnored?"selected":"") %>>все темы</option>
      <option value="f" <%= (showIgnored?"":"selected") %>>без игнорируемых</option>
      </select> <input type="submit" value="Обновить"> [<a href="ignore-list.jsp">настроить</a>]
  </div>

</form>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Заголовок
<%
  out.print("<span style=\"font-weight: normal\">[порядок: ");

  out.print("<b>дата отправки</b> <a href=\"group-lastmod.jsp?group=" + groupId + "\" style=\"text-decoration: underline\">дата изменения</a>");

  out.print("]</span>");
%></th><th>Число ответов<br>всего/день/час</th></tr>
</thead>
<tbody>
<%
  String delq = showDeleted ? "" : " AND NOT deleted ";

  ResultSet rs;

  if (firstPage) {
    rs = st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND groups.id=" + groupId + delq + ignq + " AND (postdate>(CURRENT_TIMESTAMP-'3 month'::interval) or sticky) ORDER BY sticky desc,msgid DESC LIMIT " + topics);
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND groups.id=" + groupId + delq + " ORDER BY sticky,msgid ASC LIMIT " + topics + " OFFSET " + offset);
  }

  List<String> outputList = new ArrayList<String>();
  double messages = tmpl.getProf().getInt("messages");

  while (rs.next()) {
    StringBuffer outbuf = new StringBuffer();
    int stat1 = rs.getInt("stat1");

    Timestamp lastmod = rs.getTimestamp("lastmod");
    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }

    outbuf.append("<tr><td>");
    if (rs.getBoolean("deleted")) {
      outbuf.append("[X] ");
    } else if (rs.getBoolean("sticky")) {
      outbuf.append("<img src=\"img/paper_clip.gif\" width=\"15\" height=\"15\" alt=\"Прикреплено\" title=\"Прикреплено\"> ");
    }

    int pagesInCurrent = (int) Math.ceil(stat1 / messages);

    if (firstPage) {
      if (pagesInCurrent <= 1) {
        outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("&amp;lastmod=").append(lastmod.getTime()).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      } else {
        outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      }
    } else {
      outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
    }

    if (pagesInCurrent > 1) {
      outbuf.append("&nbsp;(стр.");

      for (int i = 1; i < pagesInCurrent; i++) {
        outbuf.append(" <a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid"));
        if ((i == pagesInCurrent - 1) && firstPage) {
          outbuf.append("&amp;lastmod=").append(lastmod.getTime());
        }
        outbuf.append("&amp;page=").append(i).append("\">");
        outbuf.append(i + 1).append("</a>");
      }
      outbuf.append(')');
    }

    outbuf.append(" (").append(rs.getString("nick")).append(')');
    outbuf.append("</td>");

    outbuf.append("<td align=center>");
    int stat3 = rs.getInt("stat3");
    int stat4 = rs.getInt("stat4");

    if (stat1 > 0) {
      outbuf.append("<b>").append(stat1).append("</b>/");
    } else {
      outbuf.append("-/");
    }

    if (stat3 > 0) {
      outbuf.append("<b>").append(stat3).append("</b>/");
    } else {
      outbuf.append("-/");
    }

    if (stat4 > 0) {
      outbuf.append("<b>").append(stat4).append("</b>");
    } else {
      outbuf.append('-');
    }


    outbuf.append("</td></tr>");

    if (!firstPage && ignoreList != null && !ignoreList.isEmpty() && ignoreList.containsValue(rs.getString("nick"))) {
      outbuf = new StringBuffer();
      //new StringBuffer().append("<tr><td colspan=2>Тема создана игнорируемым пользователем</td></tr>");
    }

    outputList.add(outbuf.toString());
  }
  rs.close();

  if (!firstPage) {
    Collections.reverse(outputList);
  }

  for (Object anOutputList : outputList) {
    out.print((String) anOutputList);
  }
%>
</tbody>
<tfoot>
<%
  out.print("<tr><td colspan=2><p>");

  String ignoredAdd = showIgnored ?("&amp;showignored=t"):"";

  out.print("<div style=\"float: left\">");

  // НАЗАД
  if (firstPage) {
    out.print("");
  } else if (offset == pages * topics) {
    out.print("<a href=\"group.jsp?group=" + groupId + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">Начало</a> ");
  } else {
    out.print("<a rel=prev rev=next href=\"group.jsp?group=" + groupId + "&amp;offset=" + (offset + topics) + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">Назад</a>");
  }

  out.print("</div>");

  // ВПЕРЕД
  out.print("<div style=\"float: right\">");

  if (firstPage) {
    out.print("<a rel=next rev=prev href=\"group.jsp?group=" + groupId + "&amp;offset=" + (pages * topics) + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">Архив</a>");
  } else if (offset == 0 && !firstPage) {
    out.print("<b>Вперед</b>");
  } else {
    out.print("<a rel=next rev=prev href=\"group.jsp?group=" + groupId + "&amp;offset=" + (offset - topics) + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">Вперед</a>");
  }

  out.print("</div>");
%>
</tfoot>
</table>
</div>
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
        out.print("[<a href=\"group.jsp?group=" + groupId + "&amp;offset=0" + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">конец</a>] ");
      } else {
        out.print("[<b>конец</b>] ");
      }
    } else if (i==0) {
      if (firstPage) {
        out.print("[<b>начало</b>] ");
      } else {
        out.print("[<a href=\"group.jsp?group=" + groupId + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">начало</a>] ");
      }
    } else if ((pages + 1 - i) * topics == offset) {
      out.print("[<b>" + (pages + 1 - i) + "</b>] ");
    } else {
      out.print("[<a href=\"group.jsp?group=" + groupId + "&amp;offset=" + ((pages + 1 - i) * topics) + (showDeleted ? "&amp;deleted=t" : "") + ignoredAdd + "\">" + (pages + 1 - i) + "</a>] ");
    }
  }
%>
<p>

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

</div>
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
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
