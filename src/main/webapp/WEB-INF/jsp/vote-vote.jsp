<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="poll" type="ru.org.linux.poll.Poll"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Голосование</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Голосование</H1>
<h2><a href="view-message.jsp?msgid=${poll.topic}">Опрос</a></h2>
<h3><l:title>${message.title}</l:title></h3>

<lor:poll-form poll="${poll}" enabled="${currentUser != null}"/>

<br>
<a href="view-vote.jsp?vote=${poll.id}">результаты</a>
<br><a href="/polls/">итоги прошедших опросов...</a>
<br>[<a href="add.jsp?group=19387">добавить опрос</a>]

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
