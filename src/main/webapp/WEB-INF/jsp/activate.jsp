<%@ page contentType="text/html; charset=utf-8" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Активация</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Активация</h1>

<c:if test="${not template.sessionAuthorized}">
  <form method=POST action="/activate.jsp" id="activateForm" class="form-horizontal">
    <lor:csrf/>

    <c:if test="${not empty error}">
      <div class="error">${error}</div>
    </c:if>

    <div class="control-group">
      <label class="control-label" for="field_nick">Login/Email</label>
      <div class="controls">
        <input type="text" name="nick" required autofocus id="field_nick" value="${fn:escapeXml(nick)}">
        <span class="help-block">
          Регистр имеет значение! Вместо имени пользователя
          можно ввести email, указанный при регистрации.
        </span>
      </div>
    </div>

    <div class="control-group">
      <label for="field_password" class="control-label">Пароль</label>
      <div class="controls">
        <input type="password" name="passwd" required id="field_password">
      </div>
    </div>

    <div class="control-group">
      <label for="field_code" class="control-label">Код активации</label>
      <div class="controls">
        <input type="text" name="activation" required id="field_code" value="${fn:escapeXml(activation)}">
      </div>
    </div>

    <div class="control-group">
      <div class="controls">
        <button type=submit class="btn btn-primary">Активировать</button>
      </div>
    </div>
    <input type="hidden" name="action" value="new" />
  </form>
</c:if>

<c:if test="${template.sessionAuthorized}">
  <form method=POST action="/activate.jsp" id="activateForm">
    <lor:csrf/>
    <dl>
      <dt><label for="field_code">Код активации:</label></dt>
      <dd><input type="text" name="activation" required autofocus id="field_code" value="${fn:escapeXml(activation)}"></dd>
    </dl>

    <button type=submit>Активировать</button>
  </form>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
