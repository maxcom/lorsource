<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="topic" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="comment" type="ru.org.linux.comment.PreparedComment"--%>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Восстановление комментария</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Восстановление комментария</h1>

<div class="messages">
  <div class="comment">
    <lor:comment commentsAllowed="false" showMenu="false" comment="${comment}" topic="${topic}"/>
  </div>
</div>

<form method=POST action="undelete_comment">
  <lor:csrf/>

  <input type=hidden name=msgid value="${comment.id}">

  <div class="control-group">
    <button type=submit class="btn btn-primary">Восстановить</button>
  </div>
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
