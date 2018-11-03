<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
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
<%--@elvariable id="comment" type="ru.org.linux.comment.PreparedComment"--%>
<%--@elvariable id="deadline" type="java.util.Date"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Изменить комментарий</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Изменить комментарий</h1>

<p>
<a href="${edit.topic.link}?cid=${edit.original.id}">Комментарий</a> написан <lor:date date="${comment.postdate}"/>.
<c:if test="${deadline!=null}">
  Редактирование возможно до <lor:date date="${deadline}"/>.
</c:if>
</p>

<div class=messages>
  <l:comment commentsAllowed="false" showMenu="false" comment="${comment}" topic="${edit.topic}" enableSchema="false"/>
</div>

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
