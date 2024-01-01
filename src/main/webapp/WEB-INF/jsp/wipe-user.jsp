<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%--@elvariable id="commentCount" type="java.lang.Integer"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Блокировка с удалением сообщений ${user.nick}</title>

<jsp:include page="header.jsp"/>

<h1>Блокировка с удалением сообщений ${user.nick}</h1>
<p>
  Будет удалено ${commentCount} комментариев вместе с ответами, а так же все темы пользователя.
</p>

<p><strong>Внимание!</strong> простого способа востановить комментарии после удаления нет.</p>

<form method='post' action='usermod.jsp'>
  <lor:csrf/>
  <input type='hidden' name='id' value='${user.id}'>
  <c:if test="${not user.blocked}">
    <div class="control-group">
      <label for="reason">Причина</label>
      <input type="text" id="reason" autofocus name="reason" size="60">
    </div>
    <div class="form-actions">
      <button type=submit name=action value="block-n-delete-comments" class="btn btn-danger">
        Блокировать с удалением сообщений
      </button>
    </div>
  </c:if>
</form>

<jsp:include page="footer.jsp"/>