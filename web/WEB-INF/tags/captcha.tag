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

<%
  if (!Template.isSessionAuthorized(session)) {
%>
    <p><img src="/jcaptcha.jsp" id="captcha_image" onclick="document.getElementById('captcha_image').src = '/jcaptcha.jsp?'+Math.random();"><input type='text' name='j_captcha_response' value=''>
<%
  }
%>
