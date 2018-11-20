<%@ page contentType="text/html; charset=utf-8" import="ru.org.linux.gallery.UploadedImagePreview"  %>
<%@ page import="ru.org.linux.tag.TagName" %>
<%@ page import="ru.org.linux.topic.TopicTagService" %>
<%@ page import="ru.org.linux.gallery.Image" %>
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
<%--@elvariable id="message" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="modes" type="java.util.Map"--%>
<%--@elvariable id="addportal" type="java.lang.String"--%>
<%--@elvariable id="form" type="ru.org.linux.topic.AddTopicRequest"--%>
<%--@elvariable id="postscoreInfo" type="java.lang.String"--%>
<%--@elvariable id="imagepost" type="java.lang.Boolean"--%>
<%--@elvariable id="topicMenu" type="ru.org.linux.topic.TopicMenu"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<title>Добавить сообщение</title>
<script type="text/javascript">
  $script.ready("plugins", function() {
    $(function() {
      $("#messageForm").validate({
        messages : {
          title : "Введите заголовок"
        }
      });

      window.onbeforeunload = function() {
          if ($("#form_msg").val()!='') {
            return "Вы что-то напечатали в форме. Все введенные данные будут потеряны при закрытии страницы.";
          }
        };

      $("#messageForm").bind("submit", function() {
          window.onbeforeunload = null;
      });
    });
  });

  $script.ready("jquery", function() {
    $script("/js/jqueryui/jquery-ui-1.10.3.custom.min.js", "jqueryui");
  });

  $script.ready("jqueryui", function() {
    $script("/js/tagsAutocomplete.js");
  });
</script>
<link rel="stylesheet" href="/js/jqueryui/jquery-ui-1.10.3.custom.min.css">
  <jsp:include page="/WEB-INF/jsp/header.jsp"/>
  <c:if test="${not form.noinfo}">
      ${addportal}
  </c:if>
<c:if test="${message != null}">
<h1>Предпросмотр</h1>
<div class=messages>
  <lor:message messageMenu="${topicMenu}" preparedMessage="${message}" message="${message.message}" showMenu="false"/>
</div>
</c:if>
<h1>Добавить в раздел «${group.title}»</h1>
<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>
<%--<% } %>--%>

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

  <form:hidden path="noinfo"/>

  <p>
    ${postscoreInfo}
  </p>

  <c:if test="${not template.sessionAuthorized}">
    <div class="control-group">
      <label for="nick">
        Имя
      </label>
      <input id="nick" type=text required value="anonymous" name="nick" style="width: 40em">
    </div>

    <div class="control-group">
      <label for="password">
        Пароль
      </label>
      <input id="password" type=password name=password style="width: 40em">
    </div>
  </c:if>

  <form:hidden path="group"/>
  <div class="control-group">
    <label for="title">Заглавие</label>
    <form:input path="title" required="required" style="width: 40em" autofocus="autofocus"/>
  </div>

  <c:if test="${imagepost}">
    <div class="control-group">
      <label for="image">Изображение:</label>
      <input id="image" type="file" name="image">
    </div>
  </c:if>

  <c:if test="${section.pollPostAllowed}">
      Внимание! Вопрос должен быть задан в поле «заглавие». В поле «сообщение» можно написать
      дополнительное описание опроса, которое будет видно только на странице опроса (и не будет
      видно в форме голосования на главной странице)<br>

      <c:forEach var="v" items="${form.poll}" varStatus="i">
            <label>Вариант #${i.index}:
                <form:input path="poll[${i.index}]" size="40"/></label>
      </c:forEach>
      <p>
        <label>Мультивыбор: <form:checkbox path="multiSelect" size="40"/></label>
      </p>
  </c:if>

<c:if test="${template.prof.formatMode == 'ntobr'}">
  <label>Разметка:*<br>
  <form:select path="mode" items="${modes}"/></label><br>
</c:if>

<div class="control-group">
  <label for="form_msg">Сообщение</label>
  <form:textarea path="msg" style="width: 40em" rows="20" id="form_msg"/>
  <div class="help-block"><b>Внимание:</b> <a href="/help/lorcode.md" target="_blank">прочитайте описание разметки LORCODE</a></div>
</div>

<c:if test="${group!=null and group.linksAllowed}">
<div class="control-group">
  <label for="linktext">
    Текст ссылки
  </label>
  <form:input path="linktext" style="width: 40em"/>
</div>

<div class="control-group">
  <label for="url">
    Ссылка (не забудьте <b>http://</b>)</label>
    <form:input placeholder="http://" path="url" type="url" style="width: 40em"/>
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
  <form:input required="required" autocapitalize="off" data-tags-autocomplete="data-tags-autocomplete" id="tags" path="tags" style="width: 40em"/>
</div>
  <lor:captcha ipBlockInfo="${ipBlockInfo}"/>
<div class="form-actions">
  <button type=submit class="btn-primary btn">Поместить</button>
  <button type=submit name=preview class="btn btn-default">Предпросмотр</button>
<c:if test="${template.sessionAuthorized && !section.pollPostAllowed}">
  <button type=submit name=draft class="btn btn-default">Сохранить в черновики</button>
</c:if>

</div>
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
