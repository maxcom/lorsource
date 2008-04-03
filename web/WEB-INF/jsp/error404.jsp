<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%
   response.setStatus(404);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Error 404</title>
<jsp:include page="header.jsp"/>

<h1>Error 404</h1>
Запрошенный Вами URL не был найден на этом сервере. <p>
The URL you requested is not found on this server.

  <jsp:include page="footer.jsp"/>
