<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2021 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Настройки</title>
<script type="text/javascript">
$script.ready('plugins', function() {
  $(function() {
    $("#profileForm").validate();
  });
});
</script>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Настройки</h1>

<nav>
  <a href="/people/${template.nick}/edit" class="btn btn-default">Редактировать профиль</a>
  <a class="btn btn-default" href="/addphoto.jsp">Добавить фотографию</a>
  <a href="/people/${template.nick}/settings" class="btn btn-selected">Настройки</a>
</nav>

<form method=POST id="profileForm" action="/people/${nick}/settings">
<lor:csrf/>
<table>
<tr><td>Показывать фотографии</td>
<td><input type="checkbox" name="photos" <c:if test="${template.prof.showPhotos}">checked</c:if> ></td></tr>
<tr><td>Показывать анонимные комментарии</td>
<td><input type="checkbox" name="showanonymous" <c:if test="${template.prof.showAnonymous}">checked</c:if> ></td></tr>
<tr><td>Показывать меньше рекламы (доступна пользователям начиная с одной зеленой звезды)</td>
<td><input type="checkbox" <c:if test="${template.currentUser.score<100 && !template.prof.hideAdsense}">disabled</c:if> name="hideAdsense" <c:if test="${template.prof.hideAdsense}">checked</c:if> ></td></tr>
<tr><td>Показывать галерею в ленте на главной</td>
<td><input type="checkbox" name="mainGallery" <c:if test="${template.prof.showGalleryOnMain}">checked</c:if> ></td></tr>
  <tr>
    <td>Старый вид трекера и форума</td>
    <td><input type="checkbox" name="oldTracker"
               <c:if test="${template.prof.oldTracker}">checked</c:if> ></td>
  </tr>
  <tr><td colspan=2><hr></td></tr>


<tr>
  <td valign=top>Тема</td>
  <td>
    <c:set value="${template.style}" var="style"/>

    <c:forEach var="s" items="${stylesList}">
      <c:if test="${s == style}">
          <label><input type=radio name=style value="${s}" checked>${s}</label>
      </c:if>
      <c:if test="${s != style}">
          <label><input type=radio name=style value="${s}">${s}</label>
      </c:if>
    </c:forEach>
  </td>
</tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top>Число тем форума на странице</td>
    <td>
      <c:set value="${template.prof.topics}" var="topics"/>

      <c:forEach var="s" items="${topicsValues}">
        <c:if test="${s == topics}">
          <label><input type=radio name=topics value="${s}" checked>${s}</label>
        </c:if>
        <c:if test="${s != topics}">
          <label><input type=radio name=topics value="${s}">${s}</label>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top>Число комментариев на странице</td>
    <td>
      <c:set value="${template.prof.messages}" var="messages"/>

      <c:forEach var="s" items="${messagesValues}">
        <c:if test="${s == messages}">
          <label><input type=radio name=messages value="${s}" checked>${s}</label>
        </c:if>
        <c:if test="${s != messages}">
          <label><input type=radio name=messages value="${s}">${s}</label>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top>Фильтр трекера по умолчанию</td>
    <td>
      <c:set value="${template.prof.trackerMode.value}" var="trackerMode"/>

      <c:forEach var="s" items="${trackerModes}">
        <c:if test="${s.value == trackerMode}">
          <label><input type=radio name=trackerMode value="${s.value}" checked>${s.label}</label>
        </c:if>
        <c:if test="${s.value != trackerMode}">
          <label><input type=radio name=trackerMode value="${s.value}">${s.label}</label>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan="2"><hr></td></tr>
  <tr>
    <td valign="top">При отсутствии аватара показывать</td>
    <td>
      <c:set value="${template.prof.avatarMode}" var="avatar"/>

      <c:forEach var="s" items="${avatarsList}">
        <c:if test="${s == avatar}">
            <label><input type=radio name=avatar value="${s}" checked>${s}</label>
        </c:if>
        <c:if test="${s != avatar}">
            <label><input type=radio name=avatar value="${s}">${s}</label>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Форматирование по умолчанию</td>
  <td>
    <c:forEach var="s" items="${formatModes}">
      <c:if test="${s.key == format_mode}">
        <label><input type=radio name=format_mode value="${s.key}" checked>${s.value}</label>
      </c:if>
      <c:if test="${s.key != format_mode}">
        <label><input type=radio name=format_mode value="${s.key}">${s.value}</label>
      </c:if>
    </c:forEach>
  </td>
</tr>

</table>

<button type=submit class="btn btn-primary">Установить</button>
</form>

<h2>Другие настройки</h2>
<ul>
<li><a href="/addphoto.jsp">Добавить фотографию</a></li>
<li><a href="/people/${nick}/edit">Изменение регистрации</a></li>
<li><a href="/edit-boxes.jsp">Настройка главной страницы</a>
<li><a href="<c:url value="/user-filter"/>">Настройка фильтрации сообщений</a>
<c:if test="${template.currentUser.score >= 100 && !template.moderatorSession && !template.currentUser.administrator}">
  <li><a href="/deregister.jsp">Удаление аккаунта</a>
</c:if>
</ul>

<p><b>Внимание!</b> Настройки на некоторых уже посещенных страницах могут
не отображаться. Очистите кеш или используйте кнопку <i>Reload</i> вашего браузера.
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
