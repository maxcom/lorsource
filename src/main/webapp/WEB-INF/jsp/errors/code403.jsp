<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="ru.org.linux.auth.AuthUtil" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" isErrorPage="true" %>
<%--
  ~ Copyright 1998-2018 Linux.org.ru
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
  Logger logger = LoggerFactory.getLogger("ru.org.linux");

  response.setStatus(403);

  if (exception==null) {
    exception = (Throwable) request.getAttribute("exception");
  }

  String message = exception==null ? "":(exception.getMessage()==null?"":exception.getMessage());

  logger.info("Forbidden. {}: {} ({})", request.getServletPath(), message, AuthUtil.getNick());
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Error 403</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div id="warning-body">
    <div id="warning-logo"><img src="/img/good-penguin.png" alt="good-penguin" /></div>
    <div id="warning-text">
        <h1>403</h1>
        <p>Доступ запрещен.</p>
        <p>Access denied.</p>
    </div>
</div>
<div id="warning-footer"></div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
