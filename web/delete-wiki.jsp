<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet" errorPage="error.jsp"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.AccessViolationException"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.site.UserErrorException" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Удаление правки из вики</title>
<%= tmpl.DocumentHeader() %>

<%
  int id = new ServletParameterParser(request).getInt("id");
  Connection db = null;

  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }

  try {
    db = tmpl.getConnection();

	PreparedStatement pst2 = db.prepareStatement("SELECT a.*,b.login,c.topic_name FROM jam_topic_version a, jam_wiki_user b, jam_topic c WHERE a.topic_version_id=? AND a.published='f' AND b.wiki_user_id=a.wiki_user_id AND c.topic_id=a.topic_id");
	pst2.setInt(1, id);
	ResultSet rs = pst2.executeQuery();

    if(!rs.next()) {
      throw new UserErrorException("Правка не найдена. Уже удалена или опубликована.");
    }

    if (!"POST".equals(request.getMethod())) {
%>
<h1>Удаление правки из вики</h1>
<%= rs.getString("topic_name")+" #"+id %>
<br>
<br>
<form method=POST action="delete-wiki.jsp">
<input type=hidden name=id value="<%= request.getParameter("id") %>">
<input type=submit value="Delete/Удалить">
</form>
<%

   } else {
     db.setAutoCommit(false);

     User user = User.getUser(db, (String) session.getValue("nick"));

     user.checkCommit();

     pst2.close();
     pst2 = db.prepareStatement("DELETE FROM jam_recent_change WHERE topic_version_id=?");
	 pst2.setInt(1, id);
	 pst2.executeUpdate();

     PreparedStatement pst = db.prepareStatement("DELETE FROM jam_topic_version WHERE topic_version_id=? AND published='f'");
     pst.setInt(1, id);
     pst.executeUpdate();

     out.print("Правка удалена из вики");
     Logger.getLogger("ru.org.linux").info("delete-wiki: Удалена правка " + id + " пользователем " + user.getNick());

     db.commit();
   }
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<%= tmpl.DocumentFooter() %>
