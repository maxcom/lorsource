<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Просмотр удаленного комментария</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Просмотр удаленного комментария</h1>

<nav>
  <a class="btn btn-default" href="/view-message.jsp?msgid=${topic.id}">Перейти в топик</a></li>
</nav>

<div class=messages>

  <c:forEach var="comment" items="${chain}">
    <h2>Ответ на:</h2>

    <lor:comment
            commentsAllowed="false"
            showMenu="false"
            comment="${comment}"
            topic="${topic}"/>
  </c:forEach>

  <c:if test="${not empty chain}">
    <h2>Удаленный комментарий:</h2>
  </c:if>

  <lor:comment
          commentsAllowed="false"
          showMenu="false"
          comment="${comment}"
          topic="${topic}"/>

</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
