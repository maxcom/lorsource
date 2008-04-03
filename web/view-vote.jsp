<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.LorDataSource,ru.org.linux.site.MissingParameterException,ru.org.linux.site.Poll"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Template" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {

    if (request.getParameter("vote") == null) {
      throw new MissingParameterException("vote");
    }

    int voteid = Integer.parseInt(request.getParameter("vote"));

    db = LorDataSource.getConnection();

    Poll poll = new Poll(db, voteid);

    response.setHeader("Location", tmpl.getMainUrl() + "jump-message.jsp?msgid=" + poll.getTopicId());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
