<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,ru.org.linux.site.MissingParameterException,ru.org.linux.site.Template" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.User"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Удаление сообщения из TOP10</title>
<%= tmpl.DocumentHeader() %>

<%
   if (request.getParameter("nick")==null) {
   	if (request.getParameter("msgid")==null)
		throw new MissingParameterException("msgid");

        int msgid = tmpl.getParameters().getInt("msgid");
%>
<h1>Удаление сообщения из TOP10</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="notop.jsp">
Имя:
<input type=text name=nick size=40 value="<%= tmpl.getCookie("NickCookie","anonymous") %>"><br>
Пароль:
<input type=password name=password size=40><br>
<input type=hidden name=msgid value="<%= msgid %>"><br>
<input type=submit value="Submit/Подтвердить">
</form>
<%
   } else {
      Connection db = null;
      try {
        int msgid = tmpl.getParameters().getInt("msgid");

	db = tmpl.getConnection("notop");
	db.setAutoCommit(false);
	PreparedStatement pst=db.prepareStatement("UPDATE topics SET notop='t' WHERE id=?");
	pst.setInt(1, msgid);

	User user=new User(db, request.getParameter("nick"));
	user.checkPassword(request.getParameter("password"));
	user.checkCommit();

	pst.executeUpdate();

        out.print("Сообщение удалено из top10");

	tmpl.getLogger().notice("commit", "Удалено из TOP10 сообщение "+msgid+" пользователем "+user.getNick());
	pst.close();

	db.commit();

      } finally {
        if (db!=null) db.close();
      }
   }
%>
<%=	tmpl.DocumentFooter() %>
