<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement" errorPage="/error.jsp" buffer="60kb" %>
<%@ page import="java.util.Date"%>
<%@ page import="com.danga.MemCached.MemCachedClient" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
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

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%
  MemCachedClient mcc=MemCachedSettings.getClient();
  String showCommentsId = MemCachedSettings.getId( "show-comments?id="+URLEncoder.encode(nick));

  String res = (String) mcc.get(showCommentsId);
  if (res==null) {
    db = tmpl.getConnectionWhois();

    res = MessageTable.showComments(db, nick);

    mcc.add(showCommentsId, res, new Date(new Date().getTime()+60*1000));
  }

  out.print(res);

%>

</table>
</div>

<h2>Последние 20 удаленных модераторами комментариев</h2>

<% if (Template.isSessionAuthorized(session) && (tmpl.isModeratorSession() || nick.equals(session.getValue("nick")))) { %>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Причина удаления</th><th>Дата</th></tr>
<tbody>
<%
  if (db==null) {
    db = tmpl.getConnectionWhois();
  }

  User user = User.getUser(db, nick);

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, comments.postdate FROM sections, groups, topics, comments, del_info WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND del_info.msgid=comments.id AND comments.userid="+user.getId()+" AND del_info.delby!="+user.getId()+" ORDER BY del_info.msgid DESC LIMIT 20;");
  while (rs.next())
	out.print("<tr><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"view-message.jsp?msgid="+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+rs.getString("reason")+"</td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td></tr>");

  rs.close();
  st.close();

%>

</table>
</div>

<% } %>

<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
