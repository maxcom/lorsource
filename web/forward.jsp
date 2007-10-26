<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,javax.servlet.http.HttpServletResponse" errorPage="/error.jsp"%>
<%@ page import="ru.org.linux.site.Template"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Редирект</title>
<%= tmpl.DocumentHeader() %>
<%
  int id = Integer.parseInt(request.getParameter("id"));

  Connection db = null;
  try {
    db = tmpl.getConnection();

    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT url FROM banners WHERE id=" + id);
    rs.next();
    String url = rs.getString("url");
    rs.close();
    st.executeUpdate("UPDATE banners SET clicks=clicks+1 WHERE id=" + id);

    st.close();
    response.setHeader("Location", url);
  } finally {
    if (db != null)
      db.close();
  }

  response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
%>
<%= tmpl.DocumentFooter() %>
