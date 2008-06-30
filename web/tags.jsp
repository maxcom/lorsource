<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.PreparedStatement"  %>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.NewsViewer" %>
<%@ page import="ru.org.linux.site.Section" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%  Template tmpl = Template.getTemplate(request);  
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Список меток</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<h1>Список меток</h1>
<%

Connection db = null;
try {

  db = LorDataSource.getConnection();

  PreparedStatement pst = db.prepareStatement("SELECT * FROM tags_values WHERE counter!=0 ORDER BY value");

  ResultSet rs = pst.executeQuery();
%>
<ul>
<%
  while (rs.next()) {
    int counter = rs.getInt("counter");

    %>
    <li><a href="view-news.jsp?section=1&tag=<%= rs.getString("value") %>"><%= rs.getString("value")%></a>(<%= counter %>)</li>
  <%
  }
%>
  </ul>
<%
  pst.close();

} finally {
  if (db != null) {
    db.close();
  }
}
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
