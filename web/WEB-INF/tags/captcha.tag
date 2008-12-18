<%@ tag import="java.sql.Connection" %>
<%@ tag import="java.sql.Statement" %>
<%@ tag import="java.util.Random" %>
<%@ tag import="java.util.logging.Logger" %>
<%@ tag import="javax.servlet.http.HttpServletResponse" %>
<%@ tag import="org.apache.commons.lang.StringUtils" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.spring.AddMessageForm" %>
<%@ tag import="ru.org.linux.util.BadURLException" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="ru.org.linux.util.ServletParameterParser" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
  if (!Template.isSessionAuthorized(session)) {
%>
    <p><img src="/jcaptcha.jsp" id="captcha_image" onclick="document.getElementById('captcha_image').src = '/jcaptcha.jsp?'+Math.random();"><input type='text' name='j_captcha_response' value=''>
<%
  }
%>
