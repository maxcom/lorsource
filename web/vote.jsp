<%@ page contentType="text/html; charset=koi8-r" %>
<%@ page
    import="java.sql.Connection,java.sql.Statement,javax.servlet.http.HttpServletResponse"
    errorPage="/error.jsp" %>
<%@ page import="ru.org.linux.site.*"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>Ваш голос принят</title>
<%= tmpl.DocumentHeader() %>

<%
  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
  }

  Connection db = null;
  try {
%>

<H1>Ваш голос принят</H1>
<%
    if (request.getParameter("vote") == null) {
      throw new BadInputException("ничего не выбрано");
    }

    int vote = Integer.parseInt(request.getParameter("vote"));
    int voteid = Integer.parseInt(request.getParameter("voteid"));
    int msgid = Integer.parseInt(request.getParameter("msgid"));
    
    db = tmpl.getConnection("vote");

    if (voteid != Poll.getCurrentPollId(db)) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }

    Integer last = (Integer) session.getValue("poll.voteid");
    if (last == null || last.intValue() != voteid) {
      Statement st = db.createStatement();

      if (st.executeUpdate("UPDATE votes SET votes=votes+1 WHERE id=" + vote + " AND vote=" + voteid) == 0) {
        throw new BadVoteException(vote, voteid);
      }

      session.putValue("poll.voteid", new Integer(voteid));
      st.close();
    }

    response.setHeader("Location", tmpl.getRedirectUrl() + "view-message.jsp?msgid=" + msgid + "&highlight=" + vote);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<%= tmpl.DocumentFooter() %>
