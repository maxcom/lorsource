<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement"   buffer="60kb" %>
<%@ page import="java.util.Date"%>
<%@ page import="com.danga.MemCached.MemCachedClient" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<% String nick=request.getParameter("nick");
	if (nick==null) {
          throw new MissingParameterException("nick");
        }

	Connection db = null;

	try {
          User user = User.getUser(db, nick);

	  boolean firstPage = true;
	  int offset = 0;
	  
	  if (request.getParameter("offset") != null) {
		offset = Integer.parseInt(request.getParameter("offset"));
		firstPage = false;
	  }
	  if (offset>0) {
		firstPage = false;
	  }
	  
	  int topics = 50;
	  int count = offset + topics;
	  
	  MemCachedClient mcc=MemCachedSettings.getClient();
	  String showCommentsId = MemCachedSettings.getId( "show-comments?id="+URLEncoder.encode(nick)+"&offset="+offset);
	  
	  if (firstPage) {
		//response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
		response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
		out.print("<title>Последние " + topics + " комментариев пользователя " + nick + "</title>");
            %>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
		out.print("<h1>Последние " + topics + " комментариев пользователя " + nick + "</h1>");
	  } else {
		response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
		out.print("<title>Последние " + count + '-' + offset + " комментариев пользователя " + nick + "</title>");
                  %>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
		out.print("<h1>Последние " + count + '-' + offset + " комментариев пользователя " + nick + "</h1>");
	  }
%>
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%

  String res = (String) mcc.get(showCommentsId);
  if (res==null) {
    db = LorDataSource.getConnection();

    res = MessageTable.showComments(db, user, offset, topics);
	
	if (firstPage) {
  	  mcc.add(showCommentsId, res, new Date(new Date().getTime()+90*1000));
	} else {
	  mcc.add(showCommentsId, res, new Date(new Date().getTime()+30 * 24 * 60 * 60 * 1000L));
	}
  }

  out.print(res);

%>
</tbody>
<tfoot>
  <tr><td colspan=5><p>
<%
  out.print("<div style=\"float: left\">");
  if (firstPage || (offset - topics)<0) {
	out.print("");
  } else {
	out.print("<a rel=prev rev=next href=\"show-comments.jsp?nick=" + nick + "&amp;offset=" + (offset - topics) + "\">← назад</a>");
  }
  out.print("</div>");
  
  out.print("<div style=\"float: right\">");
  if (res!=null && !"".equals(res)) {
	out.print("<a rel=next rev=prev href=\"show-comments.jsp?nick=" + nick + "&amp;offset=" + (offset + topics) + "\">вперед →</a>");
  } else {
	out.print("<a rel=next rev=prev href=\"show-comments.jsp?nick=" + nick + "&amp;offset=0\">первая →</a>");    
  }
  out.print("</div>");
%>
  </td></tr>
</tfoor>
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
    db = LorDataSource.getConnection();
  }

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, comments.postdate FROM sections, groups, topics, comments, del_info WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND del_info.msgid=comments.id AND comments.userid="+user.getId()+" AND del_info.delby!="+user.getId()+" ORDER BY del_info.msgid DESC LIMIT 20;");
  while (rs.next()) {
    out.print("<tr><td>" + rs.getString("ptitle") + "</td><td>" + rs.getString("gtitle") + "</td><td><a href=\"view-message.jsp?msgid=" + rs.getInt("msgid") + "\" rev=contents>" + StringUtil.makeTitle(rs.getString("title")) + "</a></td><td>" + rs.getString("reason") + "</td><td>" + Template.dateFormat.format(rs.getTimestamp("postdate")) + "</td></tr>");
  }

  rs.close();
  st.close();

%>

</table>
</div>

<% } %>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
