<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement"   buffer="60kb" %>
<%@ page import="java.sql.Timestamp"%>
<%@ page import="java.util.Date" %>
<%@ page import="com.danga.MemCached.MemCachedClient" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<% String nick=request.getParameter("nick");
	if (nick==null) {
          throw new MissingParameterException("nick");
        }

	Connection db = null;

	try {
          db = LorDataSource.getConnection();

          User user = User.getUser(db, nick);

	  int offset = 0;
	  
	  if (request.getParameter("offset") != null) {
		offset = Integer.parseInt(request.getParameter("offset"));
	  }

      boolean firstPage = true;

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
		response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 1000L);
		out.print("<title>Последние " + count + '-' + offset + " комментариев пользователя " + nick + "</title>");
                  %>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
		out.print("<h1>Последние " + count + '-' + offset + " комментариев пользователя " + nick + "</h1>");
	  }
%>
<div class=forum>
<%
  String res = (String) mcc.get(showCommentsId);
  
    if (res==null) {
    res = MessageTable.showComments(db, user, offset, topics);
	
	if (firstPage) {
  	  mcc.add(showCommentsId, res, new Date(new Date().getTime()+90*1000));
	} else {
	  mcc.add(showCommentsId, res, new Date(new Date().getTime()+60 * 60 * 1000L));
	}
  }
  
%>
<table width="100%" class="message-table">
<thead>
<tr><td colspan=5>
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
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%

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

<% if (Template.isSessionAuthorized(session) && (tmpl.isModeratorSession() || nick.equals(session.getValue("nick")))) { %>

<h2>Последние 20 удаленных модераторами комментариев</h2>


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
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, deldate FROM sections, groups, topics, comments, del_info WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND del_info.msgid=comments.id AND comments.userid="+user.getId()+" AND del_info.delby!="+user.getId()+" ORDER BY del_info.delDate DESC NULLS LAST, del_info.msgid DESC LIMIT 20;");
  while (rs.next()) {
%>
  <tr>
    <td><%= rs.getString("ptitle") %></td>
    <td><%= rs.getString("gtitle") %></td>
    <td>
      <%
      out.print("<a href=\"view-message.jsp?msgid=" + rs.getInt("msgid") + "\" rev=contents>" + StringUtil.makeTitle(rs.getString("title")) + "</a>");
      %>
    </td>
    <td><%= rs.getString("reason") %></td>
    <td>
<%
  Timestamp delDate = rs.getTimestamp("deldate");
  if (delDate!=null) {
%>
      <lor:dateinterval date="<%= delDate %>"/>
<%
  }
%>
    </td>
  </tr>
<%  }

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
