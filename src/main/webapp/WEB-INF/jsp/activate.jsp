<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Активация</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Активация</h1>

<c:if test="${not template.sessionAuthorized}">
  <form method=POST action="/activate.jsp" id="activateForm">
    <lor:csrf/>
    <dl>
      <dt><label>Login:</label></dt>
      <dd><input type="text" name="nick" /></dd>
    </dl>
    <dl>
      <dt><label>Пароль:</label></dt>
      <dd><input type="password" name="passwd" /></dd>
    </dl>
    <dl>
      <dt><label>Код активациии:</label></dt>
      <dd><input type="text" name="activation" /></dd>
    </dl>
    <input type=submit value="Активировать">
    <input type="hidden" name="action" value="new" />
  </form>
</c:if>

<c:if test="${template.sessionAuthorized}">
  <form method=POST action="/activate.jsp" id="activateForm">
    <lor:csrf/>
    <dl>
      <dt><label>Код активациии:</label></dt>
      <dd><input type="text" name="activation" /></dd>
    </dl>

    <input type=submit value="Активировать">
  </form>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
