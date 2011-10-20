<%@ page import="ru.org.linux.site.Userpic" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Загрузка фотографии</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>


  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      Загрузка фотографии
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="register.jsp">Изменение регистрации</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>

<h1 class="optional">Загрузка фотографии</h1>

<p>
Загрузите вашу фотографию в форум. Изображение должно соответствовать <a href="rules.jsp">правилам</a> сайта.
</p>
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота: от <%= Userpic.MIN_IMAGESIZE %>x<%= Userpic.MIN_IMAGESIZE %> до <%= Userpic.MAX_IMAGESIZE %>x<%= Userpic.MAX_IMAGESIZE %> пискелей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более <%= Userpic.MAX_USERPIC_FILESIZE / 1024 %> Kb</li>
  </ul>
</p>

<form action="addphoto.jsp" method="POST" enctype="multipart/form-data">
<c:if test="${error!=null}">
  <div class="error">
    Ошибка! ${error}
  </div>
</c:if>
  <input type="file" name="file"><br>
  <input type="submit" value="Отправить">
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
