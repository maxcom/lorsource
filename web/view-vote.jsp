<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.MissingParameterException,ru.org.linux.site.Poll,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="200kb"%>
<% Template tmpl = new Template(request, config, response); %>
<%=tmpl.head() %>
<%
  Connection db = null;
  try {

    if (request.getParameter("vote") == null)
      throw new MissingParameterException("vote");

    int voteid = Integer.parseInt(request.getParameter("vote"));

    db = tmpl.getConnection("view-vote");

    Poll poll = new Poll(db, voteid);

    response.setHeader("Location", tmpl.getMainUrl() + "jump-message.jsp?msgid=" + poll.getTopicId());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) db.close();
  }
%>
