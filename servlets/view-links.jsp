<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page
    import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.BadGroupException"
    errorPage="error.jsp" buffer="200kb" %>
<%@ page import="ru.org.linux.site.BadSectionException" %>
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
  int group = Integer.parseInt(request.getParameter("group"));

  db = tmpl.getConnection("view-links");

  Statement st = db.createStatement();

  ResultSet rs = st.executeQuery("SELECT groups.title, name, browsable, linkup FROM sections, groups WHERE sections.id=groups.section AND groups.id=" + group);

  if (!rs.next()) {
    throw new BadGroupException();
  }

  String title = rs.getString("title");
%>
<title><%= rs.getString("name") + " - " + title %></title>
<%= tmpl.DocumentHeader() %>
<H1><%= title %></H1>
<%

  if (!rs.getBoolean("browsable")) {
    throw new BadSectionException();
  }
  if (!rs.getBoolean("linkup")) {
    throw new BadSectionException();
  }

  rs.close();

%>

<div align=center>[<a
    href="add.jsp?group=<%= group %>&return=<%= URLEncoder.encode("view-links.jsp?group="+group)%>">добавить
  ссылку</a>]</div>


<ul>
  <%
    rs = st.executeQuery("SELECT topics.url, topics.title, topics.id as msgid, postdate, nick, CURRENT_TIMESTAMP-'1 month'::interval<postdate as new, message FROM topics,groups,sections,users,msgbase WHERE msgbase.id=topics.id AND users.id=topics.userid AND groups.id=" + group + " AND topics.groupid=groups.id AND groups.section=sections.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted ORDER BY title");

    while (rs.next()) {
      int msgid = rs.getInt("msgid");
      String url = rs.getString("url");
      String nick = rs.getString("nick");
      out.print("<li>");
      if (tmpl.isModeratorSession() || (tmpl.getCookie("NickCookie") != null && tmpl.getCookie("NickCookie").equals(nick)))
      {
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
