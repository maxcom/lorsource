<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.*"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Удаление опроса</title>
<%= tmpl.DocumentHeader() %>

<%
  int id = tmpl.getParameters().getInt("id");
  Connection db = null;

  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }

  try {
    db = tmpl.getConnection("delete-vote");

    Poll poll = new Poll(db, id);

    if (poll.isDeleted()) {
      throw new UserErrorException("Голосование уже удалено");
    }

    if (!"POST".equals(request.getMethod())) {
%>
<h1>Удаление опроса</h1>
<%= poll.getTitle() %>
<form method=POST action="delete-vote.jsp">
<input type=hidden name=id value="<%= request.getParameter("id") %>">
<input type=submit value="Delete/Удалить">
</form>
<%

   } else {
     db.setAutoCommit(false);

     User user = new User(db, (String) session.getAttribute("nick"));

     user.checkCommit();

     PreparedStatement pst = db.prepareStatement("UPDATE votenames SET deleted='t' WHERE ID=?");
     pst.setInt(1, id);
     pst.executeUpdate();

     out.print("Голосование удалено");
     tmpl.getLogger().notice("delete-vote", "Удалено голосование " + id + " пользователем " + user.getNick());

     db.commit();
   }
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<%= tmpl.DocumentFooter() %>
