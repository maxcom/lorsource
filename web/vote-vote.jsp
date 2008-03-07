<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8" %>
<%@ page
    import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.*"
      %>
<% Template tmpl = new Template(request, config.getServletContext(), response); %>
<%= tmpl.getHead() %>
<title>Голосование</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
  }

  Connection db = null;
  try {
%>

<H1>Голосование</H1>
<%
    if (request.getParameter("msgid") == null) {
      throw new BadInputException("ничего не выбрано");
    }

    int msgid;
    try {
      msgid = Integer.parseInt(request.getParameter("msgid"));
    } catch (NumberFormatException e) {
      throw new BadInputException("ничего не выбрано");
    }

    db = LorDataSource.getConnection();

    int vote = Poll.getPollIdByTopic(db, msgid);

    if (vote != Poll.getCurrentPollId(db)) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }

    Poll poll = Poll.getCurrentPoll(db);

    out.print("<h2><a href=\"view-message.jsp?msgid=" + poll.getTopicId() + "\">Опрос</a></h2>");
    out.print("<h3>" + poll.getTitle() + "</h3>");

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT id, label FROM votes WHERE vote=" + poll.getId() + " ORDER BY id");

    out.print("<form method=GET action=vote.jsp>");
    out.print("<input type=hidden name=msgid value=" + msgid + '>');
    out.print("<input type=hidden name=voteid value=" + poll.getId() + '>');
    while (rs.next()) {
      out.print("<input type=radio name=vote value=" + rs.getInt("id") + '>' + rs.getString("label") + "<br>");
    }
    rs.close();

    out.print("<input type=submit value=vote>");
    out.print("</form><br>");
    out.print("<a href=\"view-vote.jsp?vote=" + poll.getId() + "\">результаты</a>");

    rs = st.executeQuery("SELECT sum(votes) as s FROM votes WHERE vote=" + poll.getId());
    rs.next();
    out.print(" (" + rs.getInt("s") + " голосов)");
    out.print("<br><a href=\"view-news.jsp?section=5\">итоги прошедших опросов...</a>");
    out.print("<br>[<a href=\"add-poll.jsp\">добавить опрос</a>]");

    //response.setHeader("Location", tmpl.getRedirectUrl() + "view-vote.jsp?vote=" + voteid + "&highlight=" + vote);
    //response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
