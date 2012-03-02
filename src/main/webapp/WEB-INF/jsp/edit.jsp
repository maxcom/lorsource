<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.SortedSet" %>
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
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="newMsg" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="newPreparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="info" type="java.lang.String"--%>
<%--@elvariable id="editInfo" type="ru.org.linux.topic.EditInfoDto"--%>
<%--@elvariable id="commit" type="java.lang.Boolean"--%>
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.group.Group>"--%>
<%--@elvariable id="topTags" type="java.util.SortedSet<String>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Редактирование сообщения</title>
<script src="/js/jquery.validate.pack.js" type="text/javascript"></script>
<script src="/js/jquery.validate.ru.js" type="text/javascript"></script>
<script type="text/javascript">
  $(document).ready(function() {
    $("#messageForm").validate({
      messages : {
        title : "Введите заголовок"
      }
    });
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<c:if test="${info!=null}">
  <h1>${info}</h1>
  <h2>Текущая версия сообщения</h2>
  <div class=messages>
    <lor:message messageMenu="<%= null %>" preparedMessage="${preparedMessage}" message="${message}" showMenu="false"/>
  </div>
  <h2>Ваше сообщение</h2>
</c:if>
<c:if test="${info==null}">
  <h1>Редактирование</h1>
</c:if>

<div class=messages>
  <lor:message messageMenu="<%= null %>" preparedMessage="${newPreparedMessage}" message="${newMsg}" showMenu="false"/>
</div>

<form:form modelAttribute="form" action="edit.jsp" name="edit" method="post" id="messageForm">
  <form:errors cssClass="error" path="*" element="div"/>

  <input type="hidden" name="msgid" value="${message.id}">
  <c:if test="${editInfo!=null}">
    <input type="hidden" name="lastEdit" value="${editInfo.editdate.time}">
  </c:if>

  <c:if test="${not message.expired}">
  <label>Заголовок:<br> <form:input path="title" cssClass="required" style="width: 40em"/></label><br><br>

  <c:if test="${group.pollPostAllowed and template.moderatorSession}">
      <c:forEach var="v" items="${form.poll}" varStatus="i">
            <label>Вариант #${i.index}:
                <form:input path="poll[${v.key}]" size="40"/></label><br>
      </c:forEach>

      <c:forEach var="v" items="${form.newPoll}" varStatus="i">
            <label>Новый #${i.index}:
                <form:input path="newPoll[${i.index}]" size="40"/></label><br>
      </c:forEach>

      <label>Мультивыбор: <form:checkbox path="multiselect" size="40"/></label>
      <br>
  </c:if>

  <form:textarea path="msg" style="width: 40em" rows="20"/>
  <br><br>
    <c:if test="${message.haveLink}">
      <label>Текст ссылки:<br> <form:input path="linktext" style="width: 40em"/></label><br>
      <label>Ссылка:<br> <form:input path="url" style="width: 40em"/></label><br>
    </c:if>
  </c:if>

  <c:if test="${group.moderated}">
    <label>Теги:<br><form:input path="tags" style="width: 40em"/><p>
      Популярные теги:
     <c:forEach items="${topTags}" var="topTag" varStatus = "status">
${status.first ? '' : ', '}<a onclick="addTag('${topTag}');">${topTag}</a>
     </c:forEach>
  </c:if>

  <c:if test="${group.moderated and template.moderatorSession}">
    <label>Мини-новость: <form:checkbox path="minor"/></label><br>
  </c:if>

  <br>
    
  <input type="submit" value="Отредактировать">
  &nbsp;
  <input type=submit name=preview value="Предпросмотр">
  <c:if test="${commit}">
    <br><br>
    <label>Группа:
    <select name="chgrp">
      <c:forEach var="group" items="${groups}">
        <c:if test="${group.id != message.groupId}">
          <option value="${group.id}">${group.title}</option>
        </c:if>
        <c:if test="${group.id == message.groupId}">
          <option value="${group.id}" selected="selected">${group.title}</option>
        </c:if>
      </c:forEach>
    </select></label><br>
    <label>Бонус автору (<lor:user decorate="true" user="${preparedMessage.author}"/>) (от 0 до 20): <form:input path="bonus" size="5" cssClass="number" type="number"/></label><br>

    <c:forEach items="${editors}" var="editor">
      <label>Бонус корректору (<lor:user decorate="true" user="${editor}"/>) (от 0 до 5): <form:input path="editorBonus[${editor.id}]" size="5" cssClass="number" type="number"/></label><br>
    </c:forEach>

    <input type=submit name=commit value="Подтвердить">
  </c:if>
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>