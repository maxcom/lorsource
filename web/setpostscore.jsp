<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.util.logging.Logger,ru.org.linux.site.Message,ru.org.linux.site.Template" errorPage="/error.jsp"%>
<%@ page import="ru.org.linux.site.User" %>
<% Template tmpl = new Template(request, config, response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.head() %>
	<title>Смена параметров сообщения</title>
<%= tmpl.DocumentHeader() %>

<%
if (!tmpl.isModeratorSession()) {
  throw new IllegalAccessException("Not authorized");
}
%>

<%
  if (request.getMethod().equals("GET")) {
    Connection db = null;

    try {
      int msgid = tmpl.getParameters().getInt("msgid");

      db = tmpl.getConnection();

      Message msg = new Message(db, msgid);

      int postscore = msg.getPostScore();
      boolean sticky = msg.isSticky();
      boolean notop = msg.isNotop();

%>
<h1>Смена режима параметров сообщения</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="setpostscore.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
<br>
Текущий уровень записи: <%= (postscore<0?"только для модераторов":Integer.toString(postscore)) %>
<select name="postscore">
  <option value="0">0 - без ограничений</option>
  <option value="50">50 - для зарегистрированных</option>
  <option value="100">100 - одна "звезда"</option>
  <option value="200">200 - две "звезды"</option>
  <option value="300">300 - три "звезды"</option>
  <option value="400">400 - четыре "звезды"</option>
  <option value="500">500 - пять "звезд"</option>
  <option value="-1">только для модераторов</option>
</select><br>
Прикрепить сообщение <input type=checkbox name="sticky" <%= sticky?"checked":"" %>><br>
Удалить из top10 <input type=checkbox name="notop" <%= notop?"checked":"" %>><br>
<%
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<input type=submit value="Изменить">
</form>
<%
  } else {
    int msgid = tmpl.getParameters().getInt("msgid");
    int postscore = tmpl.getParameters().getInt("postscore");
    boolean sticky = request.getParameter("sticky") != null;
    boolean notop = request.getParameter("notop") != null;

    if (postscore < -1) postscore = 0;
    if (postscore > 500) postscore = 500;

    Connection db = null;
    try {
      db = tmpl.getConnection();
      db.setAutoCommit(false);

      Message msg = new Message(db, msgid);

      PreparedStatement pst = db.prepareStatement("UPDATE topics SET postscore=?, sticky=?, notop=? WHERE id=?");
      pst.setInt(1, postscore);
      pst.setBoolean(2, sticky);
      pst.setBoolean(3, notop);
      pst.setInt(4, msgid);

      User user = User.getUser(db, Template.getNick(session));
      user.checkCommit();

      pst.executeUpdate();

      if (msg.getPostScore() != postscore) {
        out.print("Установлен новый уровень записи " + (postscore < 0 ? "только для модераторов" : Integer.toString(postscore)) + "<br>");
        logger.info("Установлен новый уровень записи " + postscore + " для " + msgid + " пользователем " + user.getNick());
      }

      if (msg.isSticky() != sticky) {
        out.print("Новое значение sticky: " + sticky + "<br>");
        logger.info("Новое значение sticky: " + sticky);
      }

      if (msg.isNotop() != notop) {
        out.print("Новое значение notop: " + notop + "<br>");
        logger.info("Новое значение notop: " + notop);
      }

      pst.close();
      db.commit();

    } finally {
      if (db != null) db.close();
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
