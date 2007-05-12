<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.util.Iterator,java.util.List,java.util.Random" errorPage="error.jsp"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Подтверждение опроса</title>
<%= tmpl.DocumentHeader() %>

<%
    if (!tmpl.isSessionAuthorized(session)) {
        throw new IllegalAccessException("Not authorized");
    }

   if ("GET".equals(request.getMethod())) {
   	if (request.getParameter("id")==null)
		throw new MissingParameterException("id");

        Connection db = null;

        try {

        int id = tmpl.getParameters().getInt("id");

	db = tmpl.getConnection("commit-vote");

        Poll poll = new Poll(db, id);

        if (poll.isCommited()) {
          throw new AccessViolationException("Уже подтверждено");
        }

%>
<h1>Подтверждение опроса</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>

<form method=POST action="commit-vote.jsp">
<input type=hidden name=id value="<%= id %>">
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
<input type=submit name="submit" value="Подтвердить">
</form>
<%
  } else {
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
        poll.commit(db, user);

        out.print("Сообщение подтверждено");

        tmpl.getLogger().notice("commit", "Подтвержден опрос" + id + " пользователем " + user.getNick());
      }

      db.commit();

      Random random = new Random();

      response.setHeader("Location", tmpl.getRedirectUrl() + "votes.jsp?nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
