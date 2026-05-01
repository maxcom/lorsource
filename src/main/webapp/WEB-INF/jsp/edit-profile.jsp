<%@ page session="false" contentType="text/html; charset=utf-8"%>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="currentUser" type="ru.org.linux.user.User"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Настройки</title>
<script type="text/javascript">
$script.ready('plugins', function() {
  $(function() {
    $("#profileForm").validate();
  });
});

$script.ready('jquery', function() {
  $(function() {
    $("#profileForm").on("submit", function() {
      localStorage.removeItem('lor-theme');
    });
  });
});
</script>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Настройки</h1>

<nav>
  <a href="/people/${currentUser.nick}/edit" class="btn btn-default">Редактировать профиль</a>
  <c:if test="${canLoadUserpic}">
    <a class="btn btn-default" href="/addphoto.jsp">Добавить фотографию</a>
  </c:if>
  <a href="/people/${currentUser.nick}/settings" class="btn btn-selected">Настройки</a>
</nav>

<form method=POST id="profileForm" action="/people/${nick}/settings">
<lor:csrf/>
<table>
<tr><td><label for="photos">Показывать фотографии</label></td>
<td><input type="checkbox" id="photos" name="photos" <c:if test="${template.prof.showPhotos}">checked</c:if> ></td></tr>
<tr><td><label for="hideAdsense">Показывать меньше рекламы (доступна пользователям начиная с одной зеленой звезды)</label></td>
<td><input type="checkbox" id="hideAdsense" <c:if test="${currentUser.score<100 && !template.prof.hideAdsense}">disabled</c:if> name="hideAdsense" <c:if test="${template.prof.hideAdsense}">checked</c:if> ></td></tr>
<tr><td><label for="mainGallery">Показывать галерею, опросы и статьи в ленте на главной</label></td>
<td><input type="checkbox" id="mainGallery" name="mainGallery" <c:if test="${template.prof.showGalleryOnMain}">checked</c:if> ></td></tr>
  <tr>
    <td><label for="oldTracker">Старый вид трекера и форума</label></td>
    <td><input type="checkbox" id="oldTracker" name="oldTracker"
               <c:if test="${template.prof.oldTracker}">checked</c:if> ></td>
  </tr>
  <tr>
    <td><label for="reactionNotification">Уведомлять о реакциях</label></td>
    <td><input type="checkbox" id="reactionNotification" name="reactionNotification"
               <c:if test="${template.prof.reactionNotification}">checked</c:if> ></td>
  </tr>
  <tr><td colspan=2><hr></td></tr>


<tr>
  <td valign=top><span id="style-label">Тема</span></td>
  <td>
    <c:set value="${template.style}" var="style"/>

    <div role="radiogroup" aria-labelledby="style-label">
      <c:forEach var="s" items="${stylesList}" varStatus="status">
        <label><input type=radio id="style-${status.index}" name=style value="${s}" <c:if test="${s == style}">checked</c:if>>${s}</label>
      </c:forEach>
    </div>
  </td>
</tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top><span id="topics-label">Число тем форума на странице</span></td>
    <td>
      <c:set value="${template.prof.topics}" var="topics"/>

      <div role="radiogroup" aria-labelledby="topics-label">
        <c:forEach var="s" items="${topicsValues}" varStatus="status">
          <label><input type=radio id="topics-${status.index}" name=topics value="${s}" <c:if test="${s == topics}">checked</c:if>>${s}</label>
        </c:forEach>
      </div>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top><span id="messages-label">Число комментариев на странице</span></td>
    <td>
      <c:set value="${template.prof.messages}" var="messages"/>

      <div role="radiogroup" aria-labelledby="messages-label">
        <c:forEach var="s" items="${messagesValues}" varStatus="status">
          <label><input type=radio id="messages-${status.index}" name=messages value="${s}" <c:if test="${s == messages}">checked</c:if>>${s}</label>
        </c:forEach>
      </div>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
  <td valign=top><span id="trackerMode-label">Фильтр трекера по умолчанию</span></td>
  <td>
    <c:set value="${template.prof.trackerMode.value}" var="trackerMode"/>

    <div role="radiogroup" aria-labelledby="trackerMode-label">
      <c:forEach var="s" items="${trackerModes}" varStatus="status">
        <label><input type=radio id="trackerMode-${status.index}" name=trackerMode value="${s.value}" <c:if test="${s.value == trackerMode}">checked</c:if>>${s.label}</label>
      </c:forEach>
    </div>
  </td>
</tr>

  <tr><td colspan="2"><hr></td></tr>
  <tr>
  <td valign="top"><span id="avatar-label">При отсутствии аватара показывать</span></td>
  <td>
    <c:set value="${template.prof.avatarMode}" var="avatar"/>

    <div role="radiogroup" aria-labelledby="avatar-label">
      <c:forEach var="s" items="${avatarsList}" varStatus="status">
        <label><input type=radio id="avatar-${status.index}" name=avatar value="${s}" <c:if test="${s == avatar}">checked</c:if>>${s}</label>
      </c:forEach>
    </div>
  </td>
</tr>

  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top><span id="format-mode-label">Разметка текста</span></td>
  <td>
    <div role="radiogroup" aria-labelledby="format-mode-label">
      <c:forEach var="s" items="${formatModes}" varStatus="status">
        <label><input type=radio id="format_mode-${status.index}" name=format_mode value="${s.key}" <c:if test="${s.key == format_mode}">checked</c:if>>${s.value}</label>
      </c:forEach>
    </div>
  </td>
</tr>

</table>

<button type=submit class="btn btn-primary">Установить</button>
</form>

<h2>Другие настройки</h2>
<ul>
<c:if test="${canLoadUserpic}">
  <li><a href="/addphoto.jsp">Добавить фотографию</a></li>
</c:if>
<li><a href="/people/${nick}/edit">Изменение регистрации</a></li>
<li><a href="<c:url value="/user-filter"/>">Настройка фильтрации сообщений</a>
<c:if test="${currentUser.score >= 100 && !template.moderatorSession && !currentUser.administrator && !currentUser.frozen}">
  <li><a href="/deregister.jsp">Удаление аккаунта</a>
</c:if>
</ul>

<p><b>Внимание!</b> Настройки на некоторых уже посещенных страницах могут
не отображаться. Очистите кеш или используйте кнопку <i>Reload</i> вашего браузера.
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
