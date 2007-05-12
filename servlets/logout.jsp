<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="javax.servlet.http.Cookie,javax.servlet.http.HttpServletResponse" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.Template"%>
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
