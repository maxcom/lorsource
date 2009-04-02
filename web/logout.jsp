<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet"  %>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Logout</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<h1>Logout</h1>
<% if (session!=null && session.getValue("login")!=null && (Boolean) session.getValue("login")) {
		session.removeValue("login");
		session.removeValue("nick");
		session.removeValue("moderator");
        session.removeAttribute("ACEGI_SECURITY_CONTEXT"); // if any
	Cookie cookie=new Cookie("password", "");
	cookie.setMaxAge(60*60*24*31*24);
	cookie.setPath("/");
	response.addCookie(cookie);

	Cookie cookie2=new Cookie("profile", "");
	cookie2.setMaxAge(60*60*24*31*24);
	cookie2.setPath("/");
	response.addCookie(cookie2);

	Cookie cookie3=new Cookie("ACEGI_SECURITY_HASHED_REMEMBER_ME_COOKIE", "");
	cookie3.setMaxAge(60*60*24*31*24);
	cookie3.setPath("/wiki");
	response.addCookie(cookie3);

        response.setHeader("Location", tmpl.getMainUrl());
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

        out.print("выход прошел успешно");
   } else {
        out.print("вы не входили в систему");
   }
%>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
