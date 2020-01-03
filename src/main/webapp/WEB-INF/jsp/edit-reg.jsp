<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<jsp:include page="head.jsp"/>

<title>Редактирование профиля</title>
<script type="text/javascript">
$script.ready("plugins", function() {
  $(function() {
    $("#editRegForm").validate({
      rules : {
        password2: {
          equalTo: "#password"
        }
      }
    });
  });
});
</script>

<jsp:include page="header.jsp"/>

<h1>Редактирование профиля</h1>

<nav>
  <a href="/people/${template.nick}/edit" class="btn btn-selected">Редактировать профиль</a>
  <a class="btn btn-default" href="/addphoto.jsp">Добавить фотографию</a>
  <a class="btn btn-default" href="/people/${form.nick}/settings">Настройки</a>
</nav>

<form:form modelAttribute="form" method="POST" action="/people/${form.nick}/edit" id="editRegForm">
    <lor:csrf/>
    <form:errors element="label" cssClass="error"/>
    <dl>
        <dt><label for="name">Полное имя</label></dt>
        <dd>
            <form:input path="name" size="40" cssErrorClass="error" maxlength="255"/>
            <form:errors path="name" element="label" cssClass="error" for="name"/>
            <span class="help-block">&nbsp;</span>
        </dd>

        <dt><label for="password">Новый пароль</label></dt>
        <dd>
            <form:password path="password" size="40" cssErrorClass="error" />
            <form:errors path="password" element="label" cssClass="error" for="password"/>
            <span class="help-block">не заполняйте если не хотите менять пароль</span>
        </dd>

        <dt><label for="password2">Подтвердите новый пароль</label></dt>
        <dd>
            <form:password path="password2" size="40" cssErrorClass="error" />
            <form:errors path="password2" element="label" cssClass="error" for="password2"/>
            <span class="help-block">не заполняйте если не хотите менять пароль</span>
        </dd>

        <dt><label for="url">URL</label></dt>
        <dd>
            <form:input path="url" size="60" cssErrorClass="error" maxlength="255"/>
            <form:errors path="url" element="label" cssClass="error" for="url"/>
            <span class="help-block">не забудьте добавить <i>http://</i></span>
        </dd>

        <dt><label for="email">E-mail</label></dt>
        <dd>
            <form:input path="email" type="email" cssClass="email" cssErrorClass="email error" size="60" />
            <form:errors path="email" element="label" cssClass="error" for="email"/>
            <span class="help-block">виден только вам и модераторам</span>
        </dd>

        <dt><label for="town">Город</label></dt>
        <dd>
            <form:input path="town" size="60" cssErrorClass="error" maxlength="100"/>
            <form:errors path="town" element="label" cssClass="error" for="town"/>
            <span class="help-block">просьба писать русскими буквами без сокращений, например: Москва, Нижний Новгород, Троицк (Московская область)</span>
        </dd>

        <dt><label for="info">Дополнительная информация</label></dt>
        <dd>
            <form:textarea path="info" cols="60" rows="10" cssErrorClass="error"/>
            <form:errors path="info" element="label" cssClass="error" for="info"/>
            <span class="help-block"><a href="/help/lorcode.md" target="_blank" title="справка откроется в новом окне">справка по разметке LORCODE</a></span>
        </dd>

        <dt><label for="oldpass">Пароль</label></dt>
        <dd>
            <form:password path="oldpass" size="40" cssErrorClass="error" required="required"/>
            <form:errors path="oldpass" element="label" cssClass="error" for="oldpass"/>
            <span class="help-block">&nbsp;</span>
        </dd>
    </dl>
    <button type="submit" class="btn btn-primary">Применить изменения</button>
</form:form>
<jsp:include page="footer.jsp"/>
