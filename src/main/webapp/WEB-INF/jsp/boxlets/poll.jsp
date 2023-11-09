<%--
  ~ Copyright 1998-2023 Linux.org.ru
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
<%--@elvariable id="poll" type="ru.org.linux.poll.Poll"--%>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="count" type="java.lang.Integer"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

  <c:url value="/polls/" var="main_url"/>
  <h2><a href="${main_url}">Опрос</a></h2>

  <div class="boxlet_content">
    <p>
      ${message.title}
    </p>
    <lor:poll-form poll="${poll}" enabled="${currentUser != null and poll.userVotePossible}"/>

    <c:url value="/view-vote.jsp" var="vote_url">
      <c:param name="vote" value="${poll.id}"/>
    </c:url>
    <c:if test="${poll.multiSelect}">
        <a href="${vote_url}">результаты</a> (${count}/${countUsers} голосов)
    </c:if>
    <c:if test="${not poll.multiSelect}">
        <a href="${vote_url}">результаты</a> (${count} голосов)
    </c:if>
    <br>
    <a href="${main_url}">итоги прошедших опросов...</a>
    <br>
    [<a href="<c:url value="/add-section.jsp?section=5"/>">добавить опрос</a>]
  </div>