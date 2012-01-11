<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
   response.setStatus(403);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>This is ban</title>
<jsp:include page="header.jsp"/>

<div id="warning-body">
    <div id="warning-logo"><img src="/img/good-penguin.jpg" alt="good-penguin" /></div>
    <div id="warning-text">
        <h1>Пользователь <c:out value="${exception.user.nick}" escapeXml="true"/> забанен</h1>
        <p>К сожалению, пользователь <b><c:out value="${exception.user.nick}" escapeXml="true"/></b> не может более посещать LOR.</p>
        <c:if test="${not empty exception.banInfo.date}">
            <p>начиная с <fmt:formatDate value="${exception.banInfo.date}" type="both" dateStyle="full" timeStyle="full"/></p>
        </c:if>
        <c:if test="${not empty exception.banInfo.reason}">
            <p>Причина тому проста: <i><c:out value="${exception.banInfo.reason}" escapeXml="true"/></i></p>
        </c:if>
        <p>Вопросы, пожелания по <a href="mailto:support@sport-loto.ru">адресу</a></p>
    </div>
</div>
<div id="warning-footer"></div>


<jsp:include page="footer.jsp"/>
