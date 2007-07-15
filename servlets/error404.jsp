<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder" errorPage="/error.jsp"%>
<%@ page import="ru.org.linux.site.Template" %>
<% Template tmpl = new Template(request, config, response);
   response.setStatus(404);
%>
<%= tmpl.head() %>
<title>Error 404</title>
<base href="<%= URLEncoder.encode(tmpl.getRedirectUrl()) %>">
<%= tmpl.DocumentHeader() %>

<h1>Error 404</h1>
Запрошенный Вами URL не был найден на этом сервере. <p>
The URL you requested is not found on this server.

<%= tmpl.DocumentFooter() %>
