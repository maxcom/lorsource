<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet"  %>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.Template" %>
<%  Template tmpl = Template.getTemplate(request);  
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Список меток</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<h1>Список меток</h1>
<%

if (!tmpl.isModeratorSession()) {
  throw new IllegalAccessException("Not authorized");
}

Connection db = null;
try {

  db = LorDataSource.getConnection();

  PreparedStatement pst = db.prepareStatement("SELECT * FROM tags_values ORDER BY value");

  ResultSet rs = pst.executeQuery();

  while (rs.next()) {
    out.print("<a href=\"view-news.jsp?section=1&tag="+rs.getString("value")+"\">"+rs.getString("value")+"</a>("+rs.getInt("counter")+") ");      
  }

  pst.close();

} finally {
  if (db != null) {
    db.close();
  }
}
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
