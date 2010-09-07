<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<%--@elvariable id="poll" type="ru.org.linux.site.Poll"--%>
<%--@elvariable id="msgid" type="java.lang.Integer"--%>
<%--@elvariable id="msg" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="variants" type="java.util.List<ru.org.linux.site.PollVariant>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

        <title>Редактирование опроса</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Редактирование опроса</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>

<form method=POST action="edit-vote.jsp">
<input type=hidden name=id value="${poll.id}">
<input type=hidden name=msgid value="${msgid}">
Вопрос:
<input type=text name=title size=40 value="${msg.title}">
<br>
<c:forEach var="var" items="${variants}">

  Вариант #${var.id}: <input type="text" name="var${var.id}" size="40"
                                      value="${fn:escapeXml(var.label)}"><br>
</c:forEach>
  Еще вариант: <input type="text" name="new1" size="40"><br>
  Еще вариант: <input type="text" name="new2" size="40"><br>
  Еще вариант: <input type="text" name="new3" size="40">
<p>
  <label>Мультивыбор: <input type=checkbox name=multiSelect size=40 value="true" <c:if test="${poll.multiSelect}">checked</c:if>></label>
</p>
<input type=submit name="change" value="Изменить">
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
