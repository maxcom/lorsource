<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.util.Iterator,java.util.List,java.util.Random" errorPage="/error.jsp"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<% Template tmpl = new Template(request, config, response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.head() %>
	<title>Редактирование опроса</title>
<%= tmpl.DocumentHeader() %>

<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  if ("GET".equals(request.getMethod())) {
    if (request.getParameter("msgid") == null) {
      throw new MissingParameterException("msgid");
    }

    Connection db = null;

    try {

      int msgid = tmpl.getParameters().getInt("msgid");

      db = tmpl.getConnection("commit-vote");

      int id = Poll.getPollIdByTopic(db, msgid);

      Poll poll = new Poll(db, id);

%>
<h1>Редактирование опроса</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>

<form method=POST action="edit-vote.jsp">
<input type=hidden name=id value="<%= id %>">
<input type=hidden name=msgid value="<%= msgid %>">
Вопрос:
<input type=text name=title size=40 value="<%= poll.getTitle() %>">
<br>
<%
  List variants = poll.getPollVariants(db, Poll.ORDER_ID);

  for (Iterator i = variants.iterator(); i.hasNext(); ) {
    PollVariant var = (PollVariant) i.next();

      %>
      Вариант #<%= var.getId() %>: <input type="text" name="var<%= var.getId() %>" size="40" value="<%= var.getLabel() %>"><br>
      <%
  }
  %>
  Еще вариант: <input type="text" name="new1" size="40"><br>
  Еще вариант: <input type="text" name="new2" size="40"><br>
  Еще вариант: <input type="text" name="new3" size="40"><br>

  <%

  } finally {
    if (db!=null) db.close();
  }
%>
<input type=submit name="change" value="Изменить">
<!-- input type=submit name="submit" value="Подтвердить" -->
</form>
<%
  } else {
    if (request.getParameter("msgid")==null)
	throw new MissingParameterException("msgid");

    int msgid = tmpl.getParameters().getInt("msgid");
    int id = tmpl.getParameters().getInt("id");
    String title = tmpl.getParameters().getString("title");

    boolean submit = request.getParameter("submit")!=null;

    Connection db = null;

    try {
      db = tmpl.getConnection("commit-vote");
      db.setAutoCommit(false);

      User user = new User(db, (String) session.getAttribute("nick"));
      user.checkCommit();

      Poll poll = new Poll(db, id);

      PreparedStatement pstTitle = db.prepareStatement("UPDATE votenames SET title=? WHERE id=?");
      pstTitle.setInt(2, id);
      pstTitle.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTitle.executeUpdate();

      PreparedStatement pstTopic = db.prepareStatement("UPDATE topics SET title=? WHERE id=?");
      pstTopic.setInt(2, msgid);
      pstTopic.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTopic.executeUpdate();

      List variants = poll.getPollVariants(db, Poll.ORDER_ID);
      for (Iterator i = variants.iterator(); i.hasNext();) {
        PollVariant var = (PollVariant) i.next();

        String label = tmpl.getParameters().getString("var"+var.getId());

        if (label==null || label.trim().length()==0) {
          var.remove(db);
        } else {
          var.updateLabel(db, label);
        }
      }

      for (int i=1; i<=3; i++) {
        String label = tmpl.getParameters().getString("new"+i);

        if (label!=null && label.trim().length()>0) {
          poll.addNewVariant(db, label);
        }
      }

      if (submit) {
        //poll.commit(db, user);

        out.print("Сообщение отредактировано");

        logger.info("Отредактирован опрос" + id + " пользователем " + user.getNick());
      }

      db.commit();

      Random random = new Random();

      response.setHeader("Location", tmpl.getRedirectUrl() + "jump-message.jsp?msgid="+msgid+"&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
