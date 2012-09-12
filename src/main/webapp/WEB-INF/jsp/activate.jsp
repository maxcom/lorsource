<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Активация</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Активация</h1>

<sec:authorize access="not hasRole('ROLE_ANON_USER')">
  <form method="POST" action="login.jsp" id="activateForm">
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
  </form>
</sec:authorize>

<sec:authorize access="hasRole('ROLE_ANON_USER')">
  <form method=POST action="activate.jsp" id="activateForm">
    <lor:csrf/>
    <dl>
      <dt><label>Код активациии:</label></dt>
      <dd><input type="text" name="activation" /></dd>
    </dl>

    <input type=submit value="Активировать">
  </form>
</sec:authorize>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
