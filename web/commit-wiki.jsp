<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet" errorPage="error.jsp"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.AccessViolationException"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.site.UserErrorException" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Публикация правки в вики</title>
<%= tmpl.DocumentHeader() %>

<%
  int id = tmpl.getParameters().getInt("id");
  Connection db = null;

  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }

  try {
    db = tmpl.getConnection();

    PreparedStatement pst2 = db.prepareStatement("SELECT a.*,b.login,c.topic_name FROM jam_topic_version a, jam_wiki_user b, jam_topic c WHERE a.topic_version_id=? AND a.published='f' AND b.wiki_user_id=a.wiki_user_id AND c.topic_id=a.topic_id");
    pst2.setInt(1, id);
    ResultSet rs = pst2.executeQuery();

    if (!rs.next()) {
      throw new UserErrorException("Правка не найдена. Уже удалена или опубликована.");
    }
    int topic_id = rs.getInt("topic_id");

    if (topic_id < 1) {
      throw new UserErrorException("Не могу получить topic_id");
    }

    if (!"POST".equals(request.getMethod())) {
%>
<h1>Публикация правки в вики</h1>
<%= rs.getString("topic_name")+" #"+id %>
<br>
<br>
<form method=POST action="commit-wiki.jsp">
<input type=hidden name=id value="<%= request.getParameter("id") %>">
<input type=submit value="Publish/Опубликовать">
</form>
<%

   } else {
     db.setAutoCommit(false);

     User user = User.getUser(db, (String) session.getValue("nick"));

     user.checkCommit();

     pst2.close();
     pst2 = db.prepareStatement("UPDATE jam_topic_version SET published='t' WHERE topic_version_id=? AND published='f'");
	 pst2.setInt(1, id);
	 pst2.executeUpdate();

     PreparedStatement pst = db.prepareStatement("UPDATE jam_topic SET current_version_id=? WHERE topic_id=?");
     pst.setInt(1, id);
     pst.setInt(2, topic_id);
     pst.executeUpdate();

     out.print("Правка опубликована в вики");
     Logger.getLogger("ru.org.linux").info("commit-wiki: Опубликована правка " + id + " пользователем " + user.getNick());

     db.commit();
   }
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<%= tmpl.DocumentFooter() %>
