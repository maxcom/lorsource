<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date" errorPage="error.jsp" buffer="60kb" %>
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
<title>Последние 50 комментариев пользователя <%= nick %></title>
<%= tmpl.DocumentHeader() %>
<% Connection db = null;
  try {
%>

<h1>Последние 50 комментариев пользователя <%= nick %></h1>
<%
	db = tmpl.getConnectionWhois();

        User user = new User(db, nick);
%>

<div class=forum>
<div class=color1>
<table width="100%" cellspacing=1 cellpadding=0 border=0>
<thead>
<tr class=color1><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as topicid, comments.id as msgid, comments.postdate FROM sections, groups, topics, comments WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND comments.userid="+user.getId()+" AND NOT comments.deleted ORDER BY postdate DESC LIMIT 50;");
  while (rs.next())
	out.print("<tr class=color2><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"jump-message.jsp?msgid="+rs.getInt("topicid")+'#'+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td></tr>");

  rs.close();
  st.close();

%>

</table>
</div>
</div>

<h2>Последние 20 удаленных модераторами комментариев</h2>

<% if (Template.isSessionAuthorized(session) && (tmpl.isModeratorSession() || nick.equals(session.getValue("nick")))) { %>

<div class=forum>
<div class=color1>
<table width="100%" cellspacing=1 cellpadding=0 border=0>
<thead>
<tr class=color1><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Причина удаления</th><th>Дата</th></tr>
<tbody>
<%

  st=db.createStatement();
  rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, comments.postdate FROM sections, groups, topics, comments, del_info WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND del_info.msgid=comments.id AND comments.userid="+user.getId()+" AND del_info.delby!="+user.getId()+" ORDER BY del_info.msgid DESC LIMIT 20;");
  while (rs.next())
	out.print("<tr class=color2><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"view-message.jsp?msgid="+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+rs.getString("reason")+"</td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td></tr>");

  rs.close();
  st.close();

%>

</table>
</div>
</div>

<% } %>

<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
