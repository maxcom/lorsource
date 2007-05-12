<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.MessageNotFoundException" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Смена режима записи комментариев</title>
<%= tmpl.DocumentHeader() %>

<%
   if (request.getParameter("nick")==null) {
   	if (request.getParameter("msgid")==null)
		throw new MissingParameterException("msgid");

        Connection db = null;

        try {

        int msgid = tmpl.getParameters().getInt("msgid");

	db = tmpl.getConnection("setpostscore");

	Statement st = db.createStatement();
	ResultSet rq = st.executeQuery("SELECT groupid, section, groups.title, postscore FROM topics, groups WHERE topics.id="+msgid+" AND topics.groupid=groups.id");
	if (!rq.next())
		throw new MessageNotFoundException(msgid);
        int postscore = rq.getInt("postscore");

%>
<h1>Смена режима записи комментариев</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="setpostscore.jsp">
Имя:
<input type=text name=nick size=40 value="<%= tmpl.getCookie("NickCookie","anonymous") %>"><br>
Пароль:
<input type=password name=password size=40><br>
<input type=hidden name=msgid value="<%= msgid %>">
<br>
Текущий уровень записи: <%= postscore %>
<select name="postscore">
  <option value="0">0 - без ограничений</option>
  <option value="50">50 - для зарегистрированных</option>
  <option value="100">100 - одна "звезда"</option>
  <option value="200">200 - две "звезды"</option>
  <option value="300">300 - три "звезды"</option>
  <option value="400">400 - четыре "звезды"</option>
  <option value="500">500 - пять "звезд"</option>
</select><br>
<%
	rq.close();
	st.close();
	} finally {
          if (db!=null) db.close();
        }
%>
<input type=submit value="Изменить">
</form>
<%
   } else {
        int msgid = tmpl.getParameters().getInt("msgid");
        int postscore = tmpl.getParameters().getInt("postscore");

        if (postscore<0) postscore=0;
        if (postscore>500) postscore=500;

        Connection db = null;
        try {

	db = tmpl.getConnection("setpostscore");
	db.setAutoCommit(false);
	PreparedStatement pst=db.prepareStatement("UPDATE topics SET postscore=? WHERE id=?");
	pst.setInt(1, postscore);
	pst.setInt(2, msgid);

	User user=new User(db, request.getParameter("nick"));

	user.checkPassword(request.getParameter("password"));
	user.checkCommit();

	pst.executeUpdate();

        out.print("Установлен новый уровень записи "+postscore);

	tmpl.getLogger().notice("commit", "Установлен новый уровень записи "+postscore+" для "+msgid+" пользователем "+user.getNick());

	pst.close();
	db.commit();

        } finally {
          if (db!=null) db.close();
        }
   }
%>
<%=	tmpl.DocumentFooter() %>
