<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<title>Ошибка: ${headTitle}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>${errorMessage}</h1>

<c:choose>
  <c:when test="${'SCRIPT_ERROR' == exceptionType}">
    Скрипту, генерирующему страничку были переданы некорректные
    параметры. Если на эту страничку вас привела одна из
    страниц нашего сайта, пожалуйста сообщите нам адреса текущей и ссылающейся страниц.
  </c:when>
  <c:when test="${'OTHER' == exceptionType}">
    К сожалению, произошла исключительная ситуация при генерации страницы.
    <br />
    <br />
    <b>${infoMessage}</b>
  </c:when>
</c:choose>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
