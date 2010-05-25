<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
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
<%--@elvariable id="message" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="poll" type="ru.org.linux.site.Poll"--%>
<%--@elvariable id="votes" type="java.util.List<ru.org.linux.spring.dao.VoteDTO>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Голосование</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Голосование</H1>
<h2><a href="view-message.jsp?msgid=${poll.topicId}">Опрос</a></h2>
<h3><c:out value="${message.title}" escapeXml="true"/></h3>
<form method=POST action=vote.jsp>
  <input type=hidden name=voteid value=${poll.id}>
  <input type=hidden name=msgid value=${message.id}>
  <c:forEach var="item" items="${votes}">
    <input type="radio" name="vote" id="poll-${item.id}" value="${item.id}"><label for="poll-${item.id}"><c:out escapeXml="true" value="${item.label}"/></label> <br/>
  </c:forEach>

<input type=submit value=vote>  
</form><br>
<a href="view-vote.jsp?vote=${poll.id}">результаты</a>
<br><a href="view-news.jsp?section=5">итоги прошедших опросов...</a>
<br>[<a href="add-poll.jsp">добавить опрос</a>]

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
