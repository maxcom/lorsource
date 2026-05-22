<%@ page session="false" %>
<%@ page contentType="text/html; charset=utf-8" import="ru.org.linux.gallery.UploadedImagePreview"  %>
<%@ page import="ru.org.linux.tag.TagName" %>
<%@ page import="ru.org.linux.topic.TopicTagService" %>
<%@ page import="ru.org.linux.gallery.Image" %>
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
<%--@elvariable id="message" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="addportal" type="java.lang.String"--%>
<%--@elvariable id="form" type="ru.org.linux.topic.AddTopicRequest"--%>
<%--@elvariable id="imagepost" type="java.lang.Boolean"--%>
<%--@elvariable id="topicMenu" type="ru.org.linux.topic.TopicMenu"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<title>Добавить сообщение</title>
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
<c:if test="${message != null}">
<h1>Предпросмотр</h1>
<div class=messages>
  <lor:topic messageMenu="${topicMenu}" preparedMessage="${message}" message="${message.message}" showMenu="false"/>
</div>
</c:if>
<h1>Добавить в «${group.title}»</h1>
<c:if test="${not form.noinfo}">
  ${addportal}
</c:if>

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

<form:form modelAttribute="form" id="messageForm" method="POST" action="add.jsp" enctype="${imagepost?'multipart/form-data':'application/x-www-form-urlencoded'}" >
  <lor:csrf/>
  <form:errors path="*" element="div" cssClass="error"/>

  <c:if test="${topicLimitInfo != null and not topicLimitInfo.exempt}">
    <c:choose>
      <c:when test="${topicLimitInfo.reached}">
        <div class="error">Вы можете разместить не более <lor:topic-count count="${topicLimitInfo.limit}"/> за 24 часа. Сейчас вы можете подготовить текст и сохранить его черновик</div>
      </c:when>
      <c:when test="${topicLimitInfo.currentCount > 0}">
        <div class="infoblock">Вы разместили <lor:topic-count count="${topicLimitInfo.currentCount}"/> из ${topicLimitInfo.limit} за 24 часа</div>
      </c:when>
    </c:choose>
  </c:if>

  <form:hidden path="noinfo"/>

  <c:if test="${not template.sessionAuthorized}">
    <div class="control-group">
      <label for="nick">
        Имя
      </label>
      <input id="nick" type=text required value="anonymous" name="nick">
    </div>

    <div class="control-group">
      <label for="password">
        Пароль
      </label>
      <input id="password" type=password name=password>
    </div>
  </c:if>

  <form:hidden path="group"/>
  <div class="control-group">
    <label for="title">Заглавие</label>
    <form:input path="title" required="required" autofocus="autofocus"/>
  </div>

  <c:if test="${imagepost}">
    <form:hidden path="uploadedImage"/>
    <div class="control-group">
      <c:if test="${form.uploadedImage == null}">
        <label>Изображение:
      </c:if>
      <c:if test="${form.uploadedImage != null}">
        <label>Заменить изображение:
      </c:if>
      <input id="image" type="file" name="image" accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif" >
      </label>
    </div>

    <c:if test="${not empty form.additionalUploadedImages}">
      <div class="control-group">
        <c:forEach var="v" items="${form.additionalUploadedImages}" varStatus="i">
          <form:hidden path="additionalUploadedImages[${i.index}]"/>
          <c:if test="${v == null}">
            <label>Дополнительное изображение #${i.index}:
          </c:if>
          <c:if test="${v != null}">
            <label>Заменить изображение #${i.index}:
          </c:if>
          <input type="file" name="additionalImage" accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif">
          </label>
        </c:forEach>
      </div>
    </c:if>
  </c:if>

  <c:if test="${section.pollPostAllowed}">
      <br>

      <c:forEach var="v" items="${form.poll}" varStatus="i">
            <label>Вариант #${i.index}:
                <form:input path="poll[${i.index}]" size="40"/></label>
      </c:forEach>
      <p>
        <label>Мультивыбор: <form:checkbox path="multiSelect" size="40"/></label>
      </p>
  </c:if>

<div class="control-group" data-format-mode="${template.formatMode}">
    <div class="markup-tabs">
      <ul class="markup-tabs__nav">
        <li class="markup-tabs__tab active" data-tab="editor">${template.formatModeTitle}</li>
      </ul>
      <div class="markup-tabs__content">
        <div class="markup-tabs__panel active" data-panel="editor">
          <form:textarea path="msg" id="form_msg"/>
        </div>
      </div>
    </div>

    <div class="help-block">
      <lor:markup-help mode="${template.formatMode}"/>

        Обратите внимание на то, как <a target=_blank href="/forum/linux-org-ru/15431459">правильно копировать вывод терминала</a>.
    </div>
</div>

<c:if test="${group!=null and group.linksAllowed}">
<div class="control-group">
  <label for="linktext">
    Текст ссылки
  </label>
  <form:input path="linktext"/>
</div>

<div class="control-group">
  <label for="url">
    Ссылка (не забудьте <b>http://</b>)</label>
    <form:input placeholder="http://" path="url" type="url"/>
</div>
</c:if>

<div class="control-group">
  <label for="tags">
    <c:if test="${not section.premoderated}">
      <a href="/tags" target="_blank">Метки</a> (разделенные запятой, не более <%= TagName.MaxTagsPerTopic() %>; в заголовке будет показано не более <%= TopicTagService.MaxTagsInTitle() %>)
    </c:if>

    <c:if test="${section.premoderated}">
      <a href="/tags" target="_blank">Метки</a> (разделенные запятой, не более <%= TagName.MaxTagsPerTopic() %>)
    </c:if>
  </label>
  <form:input required="required" autocapitalize="off" data-tags-autocomplete="data-tags-autocomplete" id="tags" path="tags"/>
</div>
  <lor:captcha ipBlockInfo="${ipBlockInfo}"/>

<c:if test="${showAllowAnonymous}">
  <div class="control-group">
    <label><form:checkbox path="allowAnonymous"/> разрешить анонимные комментарии </label>
  </div>
</c:if>

<div class="form-actions">
  <c:choose>
    <c:when test="${topicLimitInfo != null and not topicLimitInfo.exempt and topicLimitInfo.reached}">
      <button type=submit class="btn-primary btn" disabled>Поместить</button>
    </c:when>
    <c:otherwise>
      <button type=submit class="btn-primary btn">Поместить</button>
    </c:otherwise>
  </c:choose>
  <button type=submit name=preview class="btn btn-default">Предпросмотр</button>
<c:if test="${template.sessionAuthorized && !section.pollPostAllowed}">
  <button type=submit name=draft class="btn btn-default">Сохранить в черновики</button>
</c:if>

</div>
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
