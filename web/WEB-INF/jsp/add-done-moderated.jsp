<%@ page import="java.util.Random" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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

<p><b>Пожалуйста, не нажимайте кнопку "ReLoad" вашего броузера на этой страничке и не возвращайтесь на нее по средством кнопки Back</b>