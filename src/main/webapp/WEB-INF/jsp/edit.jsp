<%@ page session="false" %>
<%@ page import="ru.org.linux.tag.TagName" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<%@ page import="ru.org.linux.gallery.Image" %>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="newMsg" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="newPreparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="info" type="java.lang.String"--%>
<%--@elvariable id="commit" type="java.lang.Boolean"--%>
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.group.Group>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="topicMenu" type="ru.org.linux.topic.TopicMenu"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Редактирование сообщения</title>
<script type="text/javascript">
  $script('/js/add-form.js?MAVEN_BUILD_TIMESTAMP', function() {
    setupFormWithSpinner({
      formSelector: '#messageForm',
      textareaSelector: '#form_msg',
      validateOptions: {
        messages: { title: "Введите заголовок" }
      }
    });
  });
  $script.ready("plugins", function() {
    $script("/js/tagsAutocomplete.js");
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<c:if test="${info!=null}">
  <h1>${info}</h1>
</c:if>
<c:if test="${info==null}">
  <h1>Редактирование</h1>
</c:if>

<c:if test="${newPreparedMessage==null && commit}">
<div class=messages>
  <lor:topic messageMenu="${topicMenu}" preparedMessage="${preparedMessage}" message="${message}" showMenu="false" showImageDelete="true"/>
</div>
</c:if>

<c:if test="${newPreparedMessage!=null}">
<h2>Предпросмотр</h2>
<div class=messages>
  <lor:topic messageMenu="${topicMenu}" preparedMessage="${newPreparedMessage}" message="${newMsg}" showMenu="false"/>
</div>
</c:if>

<form:form modelAttribute="form" action="edit.jsp" name="edit" method="post" id="messageForm"
           enctype="${imagepost?'multipart/form-data':'application/x-www-form-urlencoded'}">
  <lor:csrf/>
  <form:errors cssClass="error" path="*" element="div"/>

  <input type="hidden" name="msgid" value="${message.id}">
  <c:if test="${lastEdit!=null}">
    <input type="hidden" name="lastEdit" value="${lastEdit}">
  </c:if>

  <c:if test="${topicMenu.topicEditable}">

  <div class="control-group">
    <label for="title">Заглавие</label>
    <form:input path="title" required="required"/>
  </div>

  <c:if test="${group.pollPostAllowed}">
    <c:forEach var="v" items="${form.poll}" varStatus="i">
      <label>Вариант #${i.index}: <form:input path="poll[${v.key}]" size="40"/></label><br>
    </c:forEach>

    <c:forEach var="v" items="${form.newPoll}" varStatus="i">
      <label>Новый #${i.index}: <form:input path="newPoll[${i.index}]" size="40"/></label><br>
    </c:forEach>

    <label>Мультивыбор: <form:checkbox path="multiselect" size="40"/></label>
    <br>
  </c:if>

  <div class="control-group" data-format-mode="${modeFormId}">
    <div class="markup-tabs">
      <ul class="markup-tabs__nav">
        <li class="markup-tabs__tab active" data-tab="editor">${mode}</li>
      </ul>
      <div class="markup-tabs__content">
        <div class="markup-tabs__panel active" data-panel="editor">
          <form:textarea path="msg" id="form_msg"/>
        </div>
      </div>
    </div>
    <div class="help-block"><lor:markup-help mode="${modeFormId}"/></div>
  </div>

    <c:if test="${preparedMessage.group.linksAllowed}">
      <div class="control-group">
        <label for="linktext">Текст ссылки</label>
        <form:input path="linktext"/>
      </div>

      <div class="control-group">
        <label for="url">Ссылка (не забудьте <b>https://</b>)</label>
        <form:input placeholder="https://" path="url" type="url"/>
      </div>
    </c:if>
  </c:if>

  <c:if test="${topicMenu.tagsEditable}">
    <div class="control-group">
      <label for="tags">
        Метки (разделенные запятой, не более <%= TagName.MaxTagsPerTopic() %>)
      </label>
      <form:input required="required" autocapitalize="off" data-tags-autocomplete="data-tags-autocomplete" id="tags" path="tags"/>
    </div>
  </c:if>

  <c:if test="${imagepost}">
    <form:hidden path="uploadedImage"/>

    <div class="control-group">
      <c:if test="${preparedMessage.image!=null}">
        <label for="image">Заменить изображение:</label>
      </c:if>
      <c:if test="${preparedMessage.image==null}">
        <label for="image">Добавить изображение:</label>
      </c:if>
      <input id="image" type="file" name="image">
    </div>

    <c:if test="${not empty form.additionalUploadedImages}">
      <div class="control-group">
        <c:forEach var="v" items="${form.additionalUploadedImages}" varStatus="i">
          <form:hidden path="additionalUploadedImages[${i.index}]"/>
          <c:if test="${v == null}">
            <label>Дополнительное изображение:
          </c:if>
          <c:if test="${v != null}">
            <label>Заменить изображение:
          </c:if>
          <input type="file" name="additionalImage" accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif">
          </label>
        </c:forEach>
      </div>
    </c:if>
  </c:if>

  <c:if test="${topicMenu.miniEditable}">
    <label>Мини: <form:checkbox path="minor"/></label>
  </c:if>

  <c:if test="${not topicMenu.miniEditable}">
    <form:hidden path="minor"/>
  </c:if>

  <div class="form-actions">
    <c:if test="${commit}">
      <button type="submit" class="btn btn-default">Отредактировать</button>
    </c:if>
    <c:if test="${not commit}">
      <button type="submit" class="btn btn-primary">Отредактировать</button>
    </c:if>
  &nbsp;
  <button type=submit name=preview class="btn btn-default">Предпросмотр</button>
  <c:if test="${message.draft}">
    &nbsp;
    <c:choose>
      <c:when test="${not topicPublishingAllowed}">
        <a class="btn btn-primary disabled" title="${fn:escapeXml(topicPublishingReason)}">Опубликовать</a>
      </c:when>
      <c:otherwise>
        <c:if test="${commit}">
          <button type=submit name=publish class="btn btn-default">Опубликовать</button>
        </c:if>
        <c:if test="${not commit}">
          <button type=submit name=publish class="btn btn-primary">Опубликовать</button>
        </c:if>
      </c:otherwise>
    </c:choose>
  </c:if>
  </div>

  <c:if test="${commit}">
    <label>Группа:
    <select name="chgrp">
      <c:forEach var="group" items="${groups}">
        <c:if test="${group.id != message.groupId}">
          <option value="${group.id}"><c:out value="${group.title}"/></option>
        </c:if>
        <c:if test="${group.id == message.groupId}">
          <option value="${group.id}" selected="selected"><c:out value="${group.title}"/></option>
        </c:if>
      </c:forEach>
    </select></label>
    <label>Бонус автору (<lor:user user="${preparedMessage.author}"/>): <form:input path="bonus" size="5" cssClass="number" type="number" min="0" max="20"/> (от 0 до 20; текущий score=${preparedMessage.author.score})</label>

    <c:forEach items="${editors}" var="editor">
      <label>Бонус корректору (<lor:user user="${editor}"/>): <form:input path="editorBonus[${editor.nick}]" size="5" cssClass="number" type="number" min="0" max="5"/> (от 0 до 5; текущий score=${editor.score})</label>
    </c:forEach>

    <div class="form-actions">
      <c:if test="${message.draft}">
        <button type=submit name=commit class="btn btn-primary">Опубликовать и Подтвердить</button>
      </c:if>
      <c:if test="${not message.draft}">
        <button type=submit name=commit class="btn btn-primary">Подтвердить</button>
      </c:if>
    </div>
  </c:if>
</form:form>

<c:if test="${imagepost}">
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота:
      от <%= Image.MinDimension() %>x<%= Image.MinDimension() %>
      до <%= Image.MaxDimension() %>x<%= Image.MaxDimension() %> пикселей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более <%= (Image.MaxFileSize() / 1024) - 50 %> Kb</li>
    <li>Изображения, содержащие EXIF-информацию, не всегда могут быть загружены. Если ваше изображение соответствует требованиям выше, но не принимается к загрузке, удалите из него EXIF-информацию.</li>
  </ul>
</p>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
