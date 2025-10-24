<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ page import="java.util.Random" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Добавление сообщения прошло успешно</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<%
  Random random = new Random();
%>

<p>
  Вы поместили сообщение в защищенный раздел. Подождите, пока ваше сообщение проверят.
</p>

<c:if test="${currentUser != null}">
  <p>Пожалуйста, проверьте свое сообщение и работоспособность ссылок в нем в <a href="view-all.jsp?nocache=<%= random.nextInt()%>">буфере неподтверждённых сообщений</a>

  <p><a href="${url}">Перейти к сообщению</a>
</c:if>

<p><b>Пожалуйста, не нажимайте кнопку "Reload" вашего браузера на этой странице и не возвращайтесь на нее посредством кнопки Back.</b>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
