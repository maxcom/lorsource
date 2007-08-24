<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date" errorPage="/error.jsp" buffer="60kb" %>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<% String nick=request.getParameter("nick");
	if (nick==null) throw new MissingParameterException("nick");
	response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
%>
<title>Последние 50 тем пользователя <%= nick %></title>
<%= tmpl.DocumentHeader() %>
<% Connection db = null;
  try {
%>

<%
  db = tmpl.getConnectionWhois();

  User user = User.getUser(db, nick);
%>

<h1>Последние 50 тем пользователя <%= nick %></h1>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие</th><th>Дата</th><th>Последнее добавление</th></tr>
<tbody>
<%

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title as title, topics.id as msgid, postdate, lastmod FROM topics, groups, sections, users WHERE topics.groupid=groups.id AND sections.id=groups.section AND users.id=topics.userid AND users.id="+user.getId()+" AND NOT deleted ORDER BY msgid DESC LIMIT 50");
  while (rs.next())
	out.print("<tr><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"view-message.jsp?msgid="+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td><td>"+Template.dateFormat.format(rs.getTimestamp("lastmod"))+"</td></tr>");

  rs.close();
  st.close();

%>
</table>
</div>

<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
