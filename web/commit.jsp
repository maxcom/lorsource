<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement,java.util.Random" errorPage="/error.jsp"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<% Template tmpl = new Template(request, config, response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.head() %>
	<title>Подтверждение сообщения</title>
<%= tmpl.DocumentHeader() %>

<%
    if (!tmpl.isSessionAuthorized(session)) {
        throw new IllegalAccessException("Not authorized");
    }

   if ("GET".equals(request.getMethod())) {
   	if (request.getParameter("msgid")==null)
		throw new MissingParameterException("msgid");

        Connection db = null;

        try {

        int msgid = tmpl.getParameters().getInt("msgid");

	db = tmpl.getConnection("commit");

        Message message = new Message(db, msgid);

	int groupid = message.getGroupId();
        Group group = new Group(db, message.getGroupId());

        if (message.isCommited()) {
          throw new AccessViolationException("Сообщение уже подтверждено");
        }

        if (!group.isModerated()) {
          throw new AccessViolationException("группа не является модерируемой");
        }

%>
<h1>Подтверждение сообщения</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>
<div class=messages>

<%= message.printMessage(tmpl, db, false, Template.getNick(session)) %>

</div>

<form method=POST action="commit.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
<input type=hidden name=groupid value="<%= groupid %>">
Заголовок:
<input type=text name=title size=40 value="<%= message.getTitle() %>">
<br>
<%
        Statement st = db.createStatement();
        ResultSet rq = null;
	if (message.getSectionId()==1) { // news
		out.println("Переместить в группу: ");
		rq = st.executeQuery("SELECT id, title FROM groups WHERE section=1 ORDER BY id");
		out.println("<select name=\"chgrp\">");
		out.println("<option value="+groupid+ '>' +message.getGroupTitle()+" (не менять)</option>");
		while (rq.next()) {
			int id = rq.getInt("id");
			if (id != groupid)
				out.println("<option value="+id+ '>' +rq.getString("title")+"</option>");
		}
		out.println("</select><br>");
	}
	if (rq!=null) rq.close();
	st.close();
	} finally {
          if (db!=null) db.close();
        }
%>
<input type=submit value="Submit/Подтвердить">
</form>
<%
  } else {
    int msgid = tmpl.getParameters().getInt("msgid");
    String title = tmpl.getParameters().getString("title");
    Connection db = null;
    try {

      db = tmpl.getConnection("commit");
      db.setAutoCommit(false);
      PreparedStatement pst = db.prepareStatement("UPDATE topics SET moderate='t', commitby=?, commitdate='now', title=? WHERE id=?");
      pst.setInt(3, msgid);
      pst.setString(2, HTMLFormatter.htmlSpecialChars(title));

      PreparedStatement pst2 = db.prepareStatement("UPDATE users SET score=score+3 WHERE id IN (SELECT userid FROM topics WHERE id=?) AND score<300");
      pst2.setInt(1, msgid);

      User user = User.getUser(db, (String) session.getAttribute("nick"));
      pst.setInt(1, user.getId());

      user.checkCommit();

      if (request.getParameter("chgrp") != null) {
        int chgrp = tmpl.getParameters().getInt("chgrp");

        Statement st = db.createStatement();
        ResultSet rs = st.executeQuery("select groupid, section, groups.title FROM topics, groups WHERE topics.id=" + msgid + " and groups.id=topics.groupid");
        if (!rs.next())
          throw new MessageNotFoundException(msgid);

        int oldgrp = rs.getInt("groupid");
        if (oldgrp != chgrp) {
          String oldtitle = rs.getString("title");
          int section = rs.getInt("section");
          if (section != 1)
            throw new AccessViolationException("Can't move topics in non-news section");

          rs.close();
          rs = st.executeQuery("SELECT section, title FROM groups WHERE groups.id=" + chgrp);
          rs.next();
          if (rs.getInt("section") != section)
            throw new AccessViolationException("Can't move topics between sections");
          String newtitle = rs.getString("title");
          st.executeUpdate("UPDATE topics SET groupid=" + chgrp + " WHERE id=" + msgid);
          /* to recalc counters */
          st.executeUpdate("UPDATE groups SET stat4=stat4+1 WHERE id=" + oldgrp + " or id=" + chgrp);
          out.println("<br>Сменена группа с '" + oldtitle + "' на '" + newtitle + "'<br>");
        }
        rs.close();
        st.close();
      }

      pst.executeUpdate();
      pst2.executeUpdate();

      out.print("Сообщение подтверждено");

      logger.info("Подтверждено сообщение " + msgid + " пользователем " + user.getNick());

      pst.close();
      db.commit();

      Random random = new Random();

      response.setHeader("Location", tmpl.getRedirectUrl() + "view-all.jsp?nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    } finally {
      if (db != null) db.close();
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
