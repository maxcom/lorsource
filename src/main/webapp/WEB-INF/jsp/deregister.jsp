<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<jsp:include page="head.jsp"/>

<title>Удаление пользователя</title>

<jsp:include page="header.jsp"/>
<H1>Удаление пользователя</H1>
<p>
Аккаунт становится недоступен для входа, все сообщения переходят к специальному пользователю.
</p>

<form:form modelAttribute="form" method="POST" action="deregister.jsp" id="registerForm">
    <lor:csrf/>
    <form:errors element="div" cssClass="error"/>

  <div class="control-group">
    <label for="password">Пароль</label>
    <form:password path="password" size="40" required="required" cssErrorClass="error"/>
    <form:errors path="password" element="span" cssClass="error help-inline" for="password"/>
  </div>

  <div class="control-group">
    <lor:captcha/>
  </div>

  <div class="control-group">
    <label>Заблокировать мой аккаунт
      <form:checkbox path="acceptBlock" value="true" required="required" cssErrorClass="error"/>
    </label>
    <label>Передать все сообщения специальному пользователю
      <form:checkbox path="acceptMoveToDeleted" value="true" required="required" cssErrorClass="error"/>
    </label>
    <label>Согласен с невозможностью восстановления
      <form:checkbox path="acceptOneway" value="true" required="required" cssErrorClass="error"/>
    </label>
  </div>

  <div class="form-actions">
    <button type=submit class="btn btn-primary">Удалить аккаунт</button>
  </div>
</form:form>
<jsp:include page="footer.jsp"/>
