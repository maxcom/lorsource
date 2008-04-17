<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet"  %>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.*" %>
<%  Template tmpl = Template.getTemplate(request);  
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Восстановление сообщения</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%

if (!tmpl.isModeratorSession()) {
  throw new IllegalAccessException("Not authorized");
}

Connection db = null;
try {

  db = LorDataSource.getConnection();

  if (request.getParameter("msgid")==null) {
    throw new MissingParameterException("msgid");
  }
  int msgid = Integer.parseInt(request.getParameter("msgid"));

  Message message = new Message(db, msgid);

  if (message.isExpired()) {
    throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения");
  }

  if (!message.isDeleted()) {
    throw new AccessViolationException("Сообщение уже восстановлено");
  }

  if (message.getSectionId()!=1) {
    throw new AccessViolationException("Можно восстанавливать только новости"); 
  }


  if (request.getParameter("undel")==null) {
%>
<h1>Восстановление сообщения</h1>
Вы можете восстановить удалённое сообщение.
<form method=POST action="undelete.jsp">
<input type=hidden name=msgid value="<%= request.getParameter("msgid") %>">
<div class=messages>
<%= message.printMessage(tmpl, db, false, null) %>
</div>
<input type=submit name=undel value="Undelete/Восстановить">
</form>
<%
  } else {
    db.setAutoCommit(false);

    PreparedStatement lock = db.prepareStatement("SELECT deleted FROM topics WHERE id=? FOR UPDATE");
    PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='f' WHERE id=?");
    PreparedStatement st2 = db.prepareStatement("DELETE FROM del_info WHERE msgid=?");
    lock.setInt(1, msgid);
    st1.setInt(1, msgid);
    st2.setInt(1, msgid);

    User user;
    String nick;

    if (session == null || session.getAttribute("login") == null || !(Boolean) session.getAttribute("login")) {
      throw new BadInputException("Вы уже вышли из системы");
    } else {
      user = User.getUser(db, (String) session.getAttribute("nick"));
      nick = (String) session.getAttribute("nick");
    }

    user.checkAnonymous();

    ResultSet lockResult = lock.executeQuery(); // lock another undelete.jsp on this row

    if (lockResult.next() && !lockResult.getBoolean("deleted")) {
      throw new UserErrorException("Сообщение уже восстановлено");
    }

    st1.executeUpdate();
    st2.executeUpdate();

    out.print("Сообщение восстановлено");
    logger.info("Восстановлено сообщение " + msgid + " пользователем " + nick);

    st1.close();
    st2.close();

    db.commit();
  }
} finally {
  if (db != null) {
    db.close();
  }
}
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
