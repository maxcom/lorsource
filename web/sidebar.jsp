<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.BadSectionException,ru.org.linux.site.LorDataSource"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Template" %>
<% Template tmpl = new Template(request, config.getServletContext(), response); %>
<%
  response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime()-120*1000).getTime());
%>
<META HTTP-EQUIV="Refresh" CONTENT="600; URL=http://www.linux.org.ru/sidebar.jsp">
<%
  Connection db = null;
  try {

    db = LorDataSource.getConnection();

    Statement st = db.createStatement();

    int section = 1;
    ResultSet rs = st.executeQuery("SELECT name, browsable, imagepost FROM sections WHERE id=" + section);

    if (!rs.next()) {
      throw new BadSectionException(section);
    }

%>
<strong><a href="http://www.linux.org.ru/" target="_content">LINUX.ORG.RU</a></strong><br>
Последние новости
<p style="font-size: small">
<%
 	rs.close();
	rs=st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired FROM topics,groups, users, sections WHERE sections.id="+section+" AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + section + " AND NOT deleted ORDER BY commitdate DESC LIMIT 10");

	while (rs.next()) {%>
		* <a target="_content" href="view-message.jsp?msgid=<%= rs.getInt("msgid") %>"> <%= rs.getString("subj")%></a> (<%= rs.getInt("stat1") %> комментариев)<br>
<%	}


	rs.close();
%>
<%
	st.close();
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
