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

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Конструктор страницы</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Конструктор страницы</h1>
[<a href="<c:url value="/edit-boxes.jsp"/>">В&nbsp;начало</a>] [<a href="<c:url value="/people/${template.nick}/settings"/>">Настройки&nbsp;профиля</a>] [<a href="<c:url value="/"/>">На&nbsp;главную&nbsp;страницу</a>]
<br/>
<s:form method="post" modelAttribute="form" action="/remove-box.jsp">
  <lor:csrf/>
  <s:errors path="*" cssClass="error"/><br/>

  <s:hidden path="position"/>

  <input type=submit value="Удалить">
</s:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>