<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8" %>
<%@ page
    import="java.sql.Connection,java.sql.Statement,javax.servlet.http.HttpServletResponse"
      %>
<%@ page import="ru.org.linux.site.*"%>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Ваш голос принят</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

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

    db = LorDataSource.getConnection();

    if (voteid != Poll.getCurrentPollId(db)) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }

    Integer last = (Integer) session.getValue("poll.voteid");
    if (last == null || last != voteid) {
      Statement st = db.createStatement();

      if (st.executeUpdate("UPDATE votes SET votes=votes+1 WHERE id=" + vote + " AND vote=" + voteid) == 0) {
        throw new BadVoteException(vote, voteid);
      }

      session.putValue("poll.voteid", voteid);
      st.close();
    }

    response.setHeader("Location", tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid + "&highlight=" + vote);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
