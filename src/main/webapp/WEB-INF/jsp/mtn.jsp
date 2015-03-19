<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="groups" type="java.util.List<Group>"--%>
<%--@elvariable id="author" type="ru.org.linux.user.User"--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Перенос</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
перенос <strong>${message.id}</strong> в группу:
<form method="post" action="/mt.jsp">
<lor:csrf/>
<input type=hidden name="msgid" value="${message.id}">
<select name="moveto">
  <c:forEach var="group" items="${groups}">
    <c:if test="${group.id == message.groupId}">
      <option value="${group.id}" selected="selected">${group.title}</option>
    </c:if>
    <c:if test="${group.id != message.groupId}">
      <option value="${group.id}">${group.title}</option>
    </c:if>
  </c:forEach>
</select>
<input type='submit' name='move' value='move'>
</form>

сообщение написано
<lor:user user="${author}"/>, score=${author.score}

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
