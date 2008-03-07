<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.Template"  %>
<% Template tmpl = new Template(request, config.getServletContext(), response);
   response.setStatus(404);
%>
<%= tmpl.getHead() %>
<title>Error 404</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<h1>Error 404</h1>
Запрошенный Вами URL не был найден на этом сервере. <p>
The URL you requested is not found on this server.

  <jsp:include page="WEB-INF/jsp/footer.jsp"/>
