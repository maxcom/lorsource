<%@ page import="ru.org.linux.user.UserService" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Загрузка фотографии</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <div class=nav>
    <div id="navPath">
      Загрузка фотографии
    </div>

    <div class="nav-buttons">
      <ul>
        <li><a href="register.jsp">Редактировать профиль</a></li>
        <li><a href="rules.jsp">Правила форума</a></li>
      </ul>
     </div>
 </div>

<p>
Загрузите вашу фотографию на форум. Изображение должно соответствовать <a href="rules.jsp">правилам</a> сайта.
</p>
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота: от <%= UserService.MIN_IMAGESIZE %>x<%= UserService.MIN_IMAGESIZE %> до <%= UserService.MAX_IMAGESIZE %>x<%= UserService.MAX_IMAGESIZE %> пикселей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более <%= UserService.MAX_USERPIC_FILESIZE / 1024 %> Kb</li>
  </ul>
</p>

<form action="addphoto.jsp" method="POST" enctype="multipart/form-data">
<lor:csrf/>
<c:if test="${error!=null}">
  <div class="error">
    Ошибка! ${error}
  </div>
</c:if>
  <input type="file" name="file"><br>
  <input type="submit" value="Отправить">
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
