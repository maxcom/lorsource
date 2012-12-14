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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

        <title>Настройки профиля</title>
<script type="text/javascript">
$script.ready('plugins', function() {
  $(function() {
    $("#profileForm").validate({
    rules: {
      topics: {
        required: true,
        range: [ 1, 500 ]
      },
      messages: {
        required: true,
        range: [ 1, 1000 ]
      },
      tags: {
        required: true,
        range: [ 1, 100 ]
      }
    }
    });
  });
});
</script>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=nav>
    <div id="navPath">
      Настройки профиля
    </div>

    <div class="nav-buttons">
      <ul>
        <li><a href="/addphoto.jsp">Добавить фотографию</a></li>
        <li><a href="/people/${nick}/edit">Изменение регистрации</a></li>
      </ul>
     </div>
 </div>

<h2>Параметры профиля</h2>
<form method=POST id="profileForm" action="/people/${nick}/settings">
<lor:csrf/>
<table>
<tr><td colspan=2><hr></td></tr>
<tr><td>Показывать социальные кнопки (Google plus, Twitter, Juick)</td>
<td><input type="checkbox" name="showSocial" <c:if test="${template.prof.showSocial}">checked</c:if> ></td></tr>
<tr><td>Новые комментарии в начале</td>
<td><input type="checkbox" name="newfirst" <c:if test="${template.prof.showNewFirst}">checked</c:if> ></td></tr>
<tr><td>Показывать фотографии</td>
<td><input type="checkbox" name="photos" <c:if test="${template.prof.showPhotos}">checked</c:if> ></td></tr>
<tr><td><label for="topics">Число тем форума на странице</label> </td>
<td><input type=text size="5" id="topics" name="topics" value="${template.prof.topics}" ></td></tr>
<tr><td><label for="messages">Число комментариев на странице</label></td>
<td><input type=text size="5" id="messages" name="messages" value="${template.prof.messages}" ></td></tr>
<tr><td><label for="tags">Число меток в облаке</label></td>
<td><input type=text size="5" id="tags" name="tags" value="${template.prof.tags}" ></td></tr>
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
        <input type=radio name=style value="${s}" id="style-${s}" checked><label for="style-${s}">${s}</label><br>
      </c:if>
      <c:if test="${s != style && s!='white'}">
        <input type=radio name=style id="style-${s}" value="${s}"><label for="style-${s}">${s}</label><br>
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
          <input type=radio name=avatar value="${s}" checked id="avatar-${s}"><label for="avatar-${s}">${s}</label><br>
        </c:if>
        <c:if test="${s != avatar}">
          <input type=radio name=avatar value="${s}" id="avatar-${s}"><label for="avatar-${s}">${s}</label><br>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Форматирование по умолчанию</td>
  <td>
    <input type=radio name=format_mode id="format-quot"  value="quot" <c:if test="${template.formatMode == 'quot' }">checked</c:if> ><label for="format-quot">TeX paragraphs (default)</label><br>
    <input type=radio name=format_mode id="format-ntobr" value="ntobr" <c:if test="${template.formatMode == 'ntobr' }">checked</c:if> ><label for="format-ntobr">User line break</label><br>
<%--
    <input type=radio name=format_mode id="format-lorcode" value=lorcode  <%= "lorcode".equals(formatMode)?"checked":"" %>><label for="format-lorcode">LORCODE</label><br>
--%>
  </td>
</tr>

</table>

<input type=submit value="Установить">
</form>

<h2>Настройка главной страницы</h2>
После того, как вы создали свой собственный профиль, вы можете
настроить под себя содержимое стартовой страницы.
<ul>
<li><a href="/edit-boxes.jsp">настройка стартовой страницы</a>
</ul>

<h2>Настройка фильтрации сообщений</h2>
<ul>
<li><a href="<c:url value="/user-filter"/>">настройка фильтрации сообщений</a>
</ul>

<p><b>Внимание!</b> Настройки на некоторых уже посещенных страницах могут
не отображаться. Очистите кеш или используйте кнопку <i>Reload</i> вашего браузера.

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
