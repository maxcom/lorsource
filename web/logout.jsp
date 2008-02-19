<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet" errorPage="/error.jsp"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.NewsViewer" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.site.ViewerCacher" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>Logout</title>
<%= tmpl.DocumentHeader() %>
<h1>Logout</h1>
<% if (session!=null && session.getValue("login")!=null && ((Boolean) session.getValue("login")).booleanValue()) {
		session.removeValue("login");
		session.removeValue("nick");
		session.removeValue("moderator");
	Cookie cookie=new Cookie("password", "");
	cookie.setMaxAge(60*60*24*31*24);
	cookie.setPath("/");
	response.addCookie(cookie);

	Cookie cookie2=new Cookie("profile", "");
	cookie2.setMaxAge(60*60*24*31*24);
	cookie2.setPath("/");
	response.addCookie(cookie2);

        response.setHeader("Location", tmpl.getMainUrl());
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

        out.print("выход прошел успешно");
   } else {
        out.print("вы не входили в систему");
   }
%>

<%= tmpl.DocumentFooter() %>
