<%@ page contentType="text/html; charset=utf-8" import="ru.org.linux.gallery.Screenshot"  %>
<%@ page import="ru.org.linux.topic.TopicTagService"%>
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

<title>Добавить сообщение</title>
<script type="text/javascript">
  $script.ready('lorjs', function() { initTopTagSelection(); });

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

  $script("/js/jqueryui/jquery-ui-1.10.3.custom.min.js", "jqueryui");
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
<h1>Добавить в раздел ${group.title}</h1>
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
      от <%= Screenshot.MIN_SCREENSHOT_SIZE %>x<%= Screenshot.MIN_SCREENSHOT_SIZE %>
      до <%= Screenshot.MAX_SCREENSHOT_SIZE %>x<%= Screenshot.MAX_SCREENSHOT_SIZE %> пикселей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более <%= (Screenshot.MAX_SCREENSHOT_FILESIZE / 1024) - 50 %> Kb</li>
    <li>Изображения, содержащие EXIF-информацию, не всегда могут быть загружены. Если ваше изображение соответствует требованиям выше, но не принимается к загрузке, удалите из него EXIF-информацию.</li>
  </ul>
</p>
</c:if>


<form:form modelAttribute="form" id="messageForm" method="POST" action="add.jsp" enctype="${imagepost?'multipart/form-data':'application/x-www-form-urlencoded'}" >
  <form:errors path="*" element="div" cssClass="error"/>

  <form:hidden path="noinfo"/>

  ${postscoreInfo}<br>

  <c:if test="${not template.sessionAuthorized}">
    <label>
        Имя:<br> <input type=text required value="anonymous" name="nick" style="width: 40em">
    </label>
    <label>
        Пароль:<br> <input type=password name=password style="width: 40em">
    </label>
  </c:if>

  <form:hidden path="group"/>
  <p>

  <label>Заглавие:<br>
    <form:input path="title" required="required" style="width: 40em" autofocus="autofocus"/><br>
   </label>

  <c:if test="${imagepost}">
    <label>Изображение: <input type="file" name="image"></label>
  </c:if>

  <c:if test="${section.pollPostAllowed}">
      Внимание! Вопрос должен быть задан в поле «заглавие». В поле «сообщение» можно написать
      дополнительное описание опроса, которое будет видно только на странице опроса (и не будет
      видно в форме голосования на главной странице)<br>

      <c:forEach var="v" items="${form.poll}" varStatus="i">
            <label>Вариант #${i.index}:
                <form:input path="poll[${i.index}]" size="40"/></label><br>
      </c:forEach>
      <p>
        <label>Мультивыбор: <form:checkbox path="multiSelect" size="40"/></label>
      </p>
  </c:if>

<c:if test="${template.prof.formatMode == 'ntobr'}">
<label>Разметка:*<br>
<form:select path="mode" items="${modes}"/></label><br>
</c:if>

<label for="form_msg">Сообщение:</label>
<form:textarea path="msg" style="width: 40em" rows="20" id="form_msg"/><br>
<font size="2"><b>Внимание:</b> <a href="/wiki/en/Lorcode" target="_blank">прочитайте описание разметки LORCODE</a></font><br>

<c:if test="${group!=null and group.linksAllowed}">
<label>
Текст ссылки:<br> <form:input path="linktext" style="width: 40em"/>
</label>
<label>
Ссылка (не забудьте <b>http://</b>):<br> <form:input path="url" type="url" style="width: 40em"/>
</label>
</c:if>

<label>
<c:if test="${not section.premoderated}">
  Метки (разделенные запятой, не более <%= TopicTagService.MAX_TAGS_PER_TOPIC %>; в заголовке будет показано не более <%= TopicTagService.MAX_TAGS_IN_TITLE %>):<br>
</c:if>

<c:if test="${section.premoderated}">
  Метки (разделенные запятой, не более <%= TopicTagService.MAX_TAGS_PER_TOPIC %>):<br>
</c:if>

    <form:input data-tags-autocomplete="data-tags-autocomplete" id="tags" path="tags" style="width: 40em"/>
    </label>
    Популярные теги:
     <c:forEach items="${topTags}" var="topTag" varStatus = "status">
${status.first ? '' : ', '}<a data-toptag>${topTag}</a>
     </c:forEach>

  <lor:captcha ipBlockInfo="${ipBlockInfo}"/>
<div class="form-actions">
  <button type=submit>Поместить</button>
  <button type=submit name=preview>Предпросмотр</button>
<c:if test="${template.sessionAuthorized && !section.pollPostAllowed && section.premoderated}">
  <button type=submit name=draft>Сохранить в черновики</button>
</c:if>

</div>
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
