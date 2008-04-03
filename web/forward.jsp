<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,javax.servlet.http.HttpServletResponse"  %>
<%@ page import="ru.org.linux.site.LorDataSource"%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

        <title>Редирект</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  int id = Integer.parseInt(request.getParameter("id"));

  Connection db = null;
  try {
    db = LorDataSource.getConnection();

    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT url FROM banners WHERE id=" + id);
    rs.next();
    String url = rs.getString("url");
    rs.close();
    st.executeUpdate("UPDATE banners SET clicks=clicks+1 WHERE id=" + id);

    st.close();
    response.setHeader("Location", url);
  } finally {
    if (db != null) {
      db.close();
    }
  }

  response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
