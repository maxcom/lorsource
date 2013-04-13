<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Настройки профиля</title>
<script type="text/javascript">
$script.ready('plugins', function() {
  $(function() {
    $("#profileForm").validate();
  });
});
</script>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Параметры профиля</h1>
<form method=POST id="profileForm" action="/people/${nick}/settings">
<lor:csrf/>
<table>
<tr><td>Показывать социальные кнопки (Google plus, Twitter, Juick)</td>
<td><input type="checkbox" name="showSocial" <c:if test="${template.prof.showSocial}">checked</c:if> ></td></tr>
<c:if test="${template.prof.showNewFirst}">
  <tr><td>Новые комментарии в начале</td>
  <td><input type="checkbox" name="newfirst" <c:if test="${template.prof.showNewFirst}">checked</c:if> ></td></tr>
</c:if>
<tr><td>Показывать фотографии</td>
<td><input type="checkbox" name="photos" <c:if test="${template.prof.showPhotos}">checked</c:if> ></td></tr>
<tr><td><label for="topics">Число тем форума на странице</label> </td>
<td><input type=number min=1 max=500 size="5" id="topics" name="topics" value="${template.prof.topics}" required></td></tr>
<tr><td><label for="messages">Число комментариев на странице</label></td>
<td><input type=number min=1 max=1000 size="5" id="messages" name="messages" value="${template.prof.messages}" required></td></tr>
<tr><td>Показывать анонимные комментарии</td>
<td><input type="checkbox" name="showanonymous" <c:if test="${template.prof.showAnonymous}">checked</c:if> ></td></tr>
<tr><td>Подсветка строчек в таблицах сообщений (tr:hover) (только для темы black)</td>
<td><input type="checkbox" name="hover" <c:if test="${template.prof.useHover}">checked</c:if> ></td></tr>
<tr><td>Показывать меньше рекламы</td>
<td><input type="checkbox" name="hideAdsense" <c:if test="${template.prof.hideAdsense}">checked</c:if> ></td></tr>
<tr><td>Показывать галерею в ленте на главной</td>
<td><input type="checkbox" name="mainGallery" <c:if test="${template.prof.showGalleryOnMain}">checked</c:if> ></td></tr>
  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Тема</td>
  <td>
    <c:set value="${template.style}" var="style"/>

    <c:forEach var="s" items="${stylesList}">
      <c:if test="${s == style}">
          <label><input type=radio name=style value="${s}" checked>${s}</label>
      </c:if>
      <c:if test="${s != style && s!='white'}">
          <label><input type=radio name=style value="${s}">${s}</label>
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
      <label><input type=radio name=format_mode id="format-quot"  value="quot" <c:if test="${template.formatMode == 'quot' }">checked</c:if> >TeX paragraphs (default)</label>
      <label><input type=radio name=format_mode id="format-ntobr" value="ntobr" <c:if test="${template.formatMode == 'ntobr' }">checked</c:if> >User line break</label>
  </td>
</tr>

</table>

<button type=submit>Установить</button>
</form>

<h2>Другие настройки</h2>
<ul>
<li><a href="/addphoto.jsp">Добавить фотографию</a></li>
<li><a href="/people/${nick}/edit">Изменение регистрации</a></li>
<li><a href="/edit-boxes.jsp">Настройка главной страницы</a>
<li><a href="<c:url value="/user-filter"/>">Настройка фильтрации сообщений</a>
</ul>

<p><b>Внимание!</b> Настройки на некоторых уже посещенных страницах могут
не отображаться. Очистите кеш или используйте кнопку <i>Reload</i> вашего браузера.

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
