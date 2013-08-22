<%@ page import="java.util.Random" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  ~ Copyright 1998-2013 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Добавление сообщения прошло успешно</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<%
  Random random = new Random();
%>

<c:if test="${moderated}">
Вы поместили сообщение в защищенный раздел. Подождите, пока ваше сообщение проверят.
</c:if>

<p>Сообщение помещено успешно

<c:if test="${moderated}">
<p>Пожалуйста, проверьте свое сообщение и работоспособность ссылок в нем в <a href="view-all.jsp?nocache=<%= random.nextInt()%>">буфере неподтвержденных сообщений</a>
</c:if>

<p><a href="${url}">Перейти к сообщению</a>

<p><b>Пожалуйста, не нажимайте кнопку "ReLoad" вашего броузера на этой страничке и не возвращайтесь на нее посредством кнопки Back</b>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
