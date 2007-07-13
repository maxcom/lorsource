<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page
    import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date"
    errorPage="error.jsp" buffer="200kb" %>
<%@ page import="ru.org.linux.site.BadGroupException" %>
<%@ page import="ru.org.linux.site.Group" %>
<%@ page import="ru.org.linux.site.MissingParameterException" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

    if (request.getParameter("month") == null) {
      response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
      response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());
    }
%>
<%
  if (request.getParameter("group") == null) {
    throw new MissingParameterException("group");
  }
  int groupid = Integer.parseInt(request.getParameter("group"));

  db = tmpl.getConnection("view-links");

  Group group = new Group(db, groupid);

  Statement st = db.createStatement();

%>
<title><%= group.getSectionName() + " - " + group.getTitle() %></title>
<%= tmpl.DocumentHeader() %>
<H1><%= group.getTitle() %></H1>
<%

  if (!group.isBrowsable()) {
    throw new BadGroupException();
  }
  if (!group.isLinksUp()) {
    throw new BadGroupException();
  }

%>

<div align=center>[<a
    href="add.jsp?group=<%= groupid %>&return=<%= URLEncoder.encode("view-links.jsp?group="+groupid)%>">добавить
  ссылку</a>]</div>


<ul>
  <%
    ResultSet rs = st.executeQuery("SELECT topics.url, topics.title, topics.id as msgid, postdate, nick, CURRENT_TIMESTAMP-'1 month'::interval<postdate as new, message FROM topics,groups,sections,users,msgbase WHERE msgbase.id=topics.id AND users.id=topics.userid AND groups.id=" + groupid + " AND topics.groupid=groups.id AND groups.section=sections.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted ORDER BY title");

    while (rs.next()) {
      int msgid = rs.getInt("msgid");
      String url = rs.getString("url");
      String nick = rs.getString("nick");
      out.print("<li>");

      if (tmpl.isModeratorSession() || nick.equals(Template.getNick(session))) {
        out.print("[<a href=\"delete.jsp?msgid=" + msgid + "\">Удалить</a>] ");
      }

      out.print("<a href=\"" + url + "\">" + StringUtil.makeTitle(rs.getString("title")) + "</a>. ");
      out.print(rs.getString("message"));
      if (rs.getBoolean("new")) {
        out.print(" [<strong>новая</strong>]");
      }
    }
  %>
</ul>
<%
    rs.close();
    st.close();
  } finally {
      if (db != null) {
        db.close();
      }
  }
%>

<%=        tmpl.DocumentFooter() %>
