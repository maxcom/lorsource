<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2019 Linux.org.ru
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
<%--@elvariable id="error" type="java.lang.String"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Сбросить забытый пароль</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Сбросить забытый пароль</H1>
<form method=POST action="/lostpwd.jsp" class="form-horizontal">
<lor:csrf/>

  <c:if test="${not empty error}">
    <div class="error">
      <strong>Ошибка!</strong> <c:out escapeXml="true" value="${error}"/>
    </div>
  </c:if>

  <div class="control-group">
    <label class="control-label" for="email-input">Email</label>
    <div class="controls">
      <input id="email-input" type=email name=email size=40 autofocus="autofocus" required="required">
      <span class="help-block">Инструкция по сбросу пароля будет отправлена на этот адрес</span>
    </div>
  </div>

  <div class="control-group">
    <div class="controls">
      <button type=submit class="btn btn-primary">Сбросить</button>
    </div>
  </div>
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
