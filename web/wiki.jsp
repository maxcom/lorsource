<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement, ru.org.linux.site.AccessViolationException" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.BadInputException"%>
<%@ page import="ru.org.linux.site.Template"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Изменения в вики</title>
<%= tmpl.DocumentHeader() %>
<%
  boolean showDeleted =request.getParameter("deleted")!=null;
  if (showDeleted && !"POST".equals(request.getMethod())) {
    response.setHeader("Location", "/wiki.jsp");
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    showDeleted = false;
  }

  if (!tmpl.isSessionAuthorized(session)) {
    throw new BadInputException("Вы уже вышли из системы");
  }

  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }

  Connection db = null;
  try {
%>
<div class=messages>
<div class=nav>
<div class=color1>
  <table width="100%" cellspacing=1 cellpadding=1 border=0>
    <tr class=body>
      <td align=left valign=middle>
        <strong>Изменения в вики</strong>
      </td>

      <td align=right valign=middle>
        [<a style="text-decoration: none" href="/wiki/">Перейти в вики</a>]
      </td>
    </tr>
  </table>
</div>
</div>
</div>


<H1>Изменения в вики</H1>
<ul>
<%
  db = tmpl.getConnection();

  Statement st = db.createStatement();

  ResultSet rs;

  if (tmpl.isModeratorSession()) {
    rs = st.executeQuery("SELECT a.*,b.login,c.topic_name FROM jam_topic_version a, jam_wiki_user b, jam_topic c WHERE a.published='f' AND b.wiki_user_id=a.wiki_user_id AND c.topic_id=a.topic_id ORDER BY a.topic_id DESC");
  } else {
    throw new AccessViolationException("Not moderator");
  }

  while (rs.next()) {
    int topic_version_id = rs.getInt("topic_version_id");
    int topic_id = rs.getInt("topic_id");
    String login = rs.getString("login");
    int wiki_user_id = rs.getInt("wiki_user_id");
    String wiki_user_ip_address = rs.getString("wiki_user_ip_address");
    int previous_topic_version_id = rs.getInt("previous_topic_version_id");
    String topic_name = rs.getString("topic_name");
%> <li>
  <a href="/wiki/ru/Special:History?topicVersionId=<%= topic_version_id%>&topic=<%= URLEncoder.encode(topic_name) %>"><%
     out.print(topic_name+ " #"+topic_version_id);
	 String edit_comment = rs.getString("edit_comment");
     if(edit_comment!=null && edit_comment.length()>0) {
		out.print(" ("+edit_comment+")</a><br/> &nbsp; &nbsp; <a name='tvid"+topic_version_id+"'>");
     }
   %></a>
  <%
    out.print(" [<a href=\"/wiki/ru/Special:Diff?type=arbitrary&topic="+URLEncoder.encode(topic_name)+"&diff%3A"+topic_version_id+"=on&diff%3A"+previous_topic_version_id+"=on\">Разница с последней публикацией</a>]");
    if (tmpl.isModeratorSession()) {
      out.print(" [<a href=\"commit-wiki.jsp?id="+topic_version_id+"\">Опубликовать</a>]");
      out.print(" [<a href=\"delete-wiki.jsp?id="+topic_version_id+"\">Удалить</a>]");
    }
  %>
   <br>
&nbsp; <i>Автор: <%= login %> (<a href="whois.jsp?nick=<%= login %>">*</a>) <%= rs.getString("edit_date").substring(0,19) %>
(IP: <%= rs.getString("wiki_user_ip_address") %>)</i>
  <% } %>
</ul>

<p>
<%
    rs.close();
    st.close();
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
