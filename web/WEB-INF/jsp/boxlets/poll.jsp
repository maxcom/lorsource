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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
  <c:url value="/polls/" var="main_url"/>
  <h2><a href="${main_url}">Опрос</a></h2>

  <div class="boxlet_content">
    <h3>${poll.title}</h3>

    <form action="/vote.jsp" method="POST">
      <input type="hidden" name="voteid" value="${poll.id}"/>
      <c:forEach var="item" items="${votes}">
        <input type="radio" name="vote" id="poll-${item.id}" value="${item.id}"><label for="poll-${item.id}"><c:out escapeXml="true" value="${item.label}"/></label> <br/>
      </c:forEach>
      <input type="submit" value="vote"/>
    </form>
    <br/>
    <c:url value="/view-vote.jsp" var="vote_url">
      <c:param name="vote" value="${poll.id}"/>
    </c:url>
    <a href="${vote_url}">результаты</a> (${count} голосов)
    <br/>
    <a href="${main_url}">итоги прошедших опросов...</a>
    <br/>
    [<a href="<c:url value="/add-poll.jsp"/>">добавить опрос</a>]
  </div>