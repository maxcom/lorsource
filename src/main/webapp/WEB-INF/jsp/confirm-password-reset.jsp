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
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="user" type="ru.org.linux.user.User"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Сброс пароля пользователя ${user.nick}</title>

<jsp:include page="header.jsp"/>

<h1>Сброс пароля пользователя <a href="${whoisLink}">${user.nick}</a></h1>

<p><strong>Внимание!</strong> Сброс пароля предназначен для защиты учетной записи при
подозрении на «угон». Не используйте эту кнопку для ограничения доступа пользователя
на сайт (используйте для этого функционал блокировки). Мы не храним старые пароли,
по этому отменить действие сброса пароля не возможно.</p>

<form action="/usermod.jsp" method="POST">
  <lor:csrf/>
  <input type="hidden" name="id" value="${user.id}">
  <input type='hidden' name='action' value='reset-password'>
  <div class="form-actions">
    <button type="submit" class="btn btn-danger">Сбросить пароль</button>
  </div>
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
