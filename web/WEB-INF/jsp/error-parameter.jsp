<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="org.apache.commons.logging.Log,org.apache.commons.logging.LogFactory" isErrorPage="true" %>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
  Log logger = LogFactory.getLog("ru.org.linux");

  if (exception==null) {
    exception = (Throwable) request.getAttribute("exception");
  }

  response.setStatus(404);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Ошибка: <%= HTMLFormatter.htmlSpecialChars(exception.getClass().getName()) %></title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1><%=exception.getMessage()==null?HTMLFormatter.htmlSpecialChars(exception.getClass().getName()):HTMLFormatter.htmlSpecialChars(exception.getMessage()) %></h1>

Скрипту, генерирующему страничку были переданы некорректные
параметры. Если на эту страничку вас привела одна из
страниц нашего сайта, пожалуйста сообщите нам адреса текущей и ссылающейся страниц.
<%
  logger.debug("error-parameter.jsp", exception);
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
