<%@ page session="false" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<%--@elvariable id="form" type="ru.org.linux.user.LostPasswordRequest"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Сбросить забытый пароль</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Сбросить забытый пароль</H1>
<form:form modelAttribute="form" method="POST" action="/lostpwd.jsp" class="form-horizontal">
<lor:csrf/>
  <form:errors element="div" cssClass="error"/>

  <div class="control-group">
    <label class="control-label" for="email-input">Email</label>
    <div class="controls">
      <form:input id="email-input" path="email" type="email" size="40" autofocus="autofocus" required="required"/>
      <form:errors path="email" element="span" cssClass="help-inline"/>
      <span class="help-block">Инструкция по сбросу пароля будет отправлена на этот адрес</span>
    </div>
  </div>

  <div class="control-group">
    <lor:captcha/>
  </div>

  <div class="control-group">
    <div class="controls">
      <button type=submit class="btn btn-primary">Сбросить</button>
    </div>
  </div>
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
