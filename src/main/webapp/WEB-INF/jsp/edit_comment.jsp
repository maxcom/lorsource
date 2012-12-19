<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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

<title>Изменить комментарий</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=nav>
<div id="navPath">
Изменить комментарий
</div>
  <div class="nav-buttons">
    <ul>
        <c:url var="topic_url" value="/view-message.jsp">
          <c:param name="msgid" value="${edit.topic.id}"/>
        </c:url>
        <li><a href="${topic_url}">Читать комментарии</a></li>
    </ul>
  </div>
</div>

<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
  <a href="rules.jsp">правилами</a> сайта.</font>

<p>

<c:if test="${comment!=null}">
  <p><b>Ваше сообщение</b></p>
  <div class=messages>
    <lor:comment commentsAllowed="false" showMenu="false" comment="${comment}" expired="${false}" topic="${null}"/>
  </div>
</c:if>

<form:form modelAttribute="edit">
    <form:errors path="*" cssClass="error" element="div" />
</form:form>

<c:if test="${error!=null}">
  <div class="error">
      ${error.message}
  </div>
</c:if>

<c:url var="form_action_url" value="/edit_comment" />

<lor:commentForm
        topic="${edit.topic}"
        title="${edit.title}"
        replyto="0"
        original="${edit.original.id}"
        msg="${edit.msg}"
        mode="${edit.mode}"
        form_action_url="${form_action_url}"
        cancel="true"
        postscoreInfo="${postscoreInfo}" />

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
