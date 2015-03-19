<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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

<title>Пользователь <c:out value="${exception.user.nick}" escapeXml="true"/> забанен.</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div id="warning-body">
    <div id="warning-logo"><img src="/img/good-penguin.jpg" alt="good-penguin" /></div>
    <div id="warning-text">
        <h1>Пользователь <c:out value="${exception.user.nick}" escapeXml="true"/> забанен.</h1>
        <p>К сожалению, пользователь <b><c:out value="${exception.user.nick}" escapeXml="true"/></b> не может более посещать LOR, </p>
        <c:choose>
            <c:when test="${not empty exception.banInfo.date}">
                <p>начиная с <fmt:formatDate value="${exception.banInfo.date}" type="both" pattern="dd.MM.yyyy hh:mm:ss"/>.</p>
            </c:when>
            <c:otherwise>
                <p>причём забанен он был настолько давно, что никто уже и не помнит, когда.</p>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${not empty exception.banInfo.reason}">
                <p>Причина тому проста: <i><c:out value="${exception.banInfo.reason}" escapeXml="true"/></i>.</p>
            </c:when>
            <c:otherwise>
                <p>Мы не знаем, за что его забанили. Видимо, он был большой редиской.</p>
            </c:otherwise>
        </c:choose>
        <p>Мы сожалеем, правда.</p>
        <p>Вопросы, пожелания по <a href="mailto:support@sport-loto.ru">адресу</a>.</p>
    </div>
</div>
<div id="warning-footer"></div>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
