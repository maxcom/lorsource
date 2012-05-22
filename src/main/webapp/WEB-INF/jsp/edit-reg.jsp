<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<jsp:include page="head.jsp"/>

<title>Регистрация пользователя</title>
<script src="/js/jquery.validate.pack.js" type="text/javascript"></script>
<script src="/js/jquery.validate.ru.js" type="text/javascript"></script>
<script type="text/javascript">
  $(document).ready(function() {
    $("#registerForm").validate({
      rules : {
        password2: {
          equalTo: "#password"
        }
      }
    });
    $("#changeForm").validate();
  });
</script>

<jsp:include page="header.jsp"/>
<H1>Изменение регистрации</H1>
Если вы уже регистрировались на нашем сайте и забыли пароль - вам
<a href="../../lostpwd.jsp">сюда</a>.

<form:form modelAttribute="form" method="POST" action="/edit-reg.jsp" id="editRegForm">
    <form:errors element="div" cssClass="error-validation"/>
    <dl>
        <dt><label>Полное имя:</label></dt>
        <dd><form:input path="name" size="40"/><form:errors path="name" cssClass="error-validation"/></dd>

        <dt><label>Новый пароль:</label></dt>
        <dd><form:password path="password" size="40" /><form:errors path="password" cssClass="error-validation"/><span class="hint">не заполняйте если не хотите менять пароль</span></dd>

        <dt><label>Подтвердите новый пароль:</label></dt>
        <dd><form:password path="password2" size="40" /><form:errors path="password2" cssClass="error-validation"/><span class="hint">не заполняйте если не хотите менять пароль</span></dd>

        <dt><label>URL:</label></dt>
        <dd><form:input path="url" size="60"/><form:errors path="url" cssClass="error-validation"/><span class="hint">не забудьте добавить <i>http://</i></span></dd>

        <dt><label>E-mail:</label></dt>
        <dd><form:input path="email" type="email" cssClass="email" size="60"/><form:errors path="email" cssClass="error-validation"/></dd>

        <dt><label>Город:</label></dt>
        <dd><form:input path="town" size="60"/><form:errors path="town" cssClass="error-validation"/><span class="hint">просьба писать русскими буквами без сокращений, например: Москва, Нижний Новгород, Троицк (Московская область)</span></dd>

        <dt><label>Дополнительная информация:</label></dt>
        <dd><form:textarea path="info" cols="60" rows="10"/><form:errors path="info" cssClass="error-validation"/></dd>

        <dt><label>Пароль:</label></dt>
        <dd><form:password path="oldpass" size="40" /><form:errors path="oldpass" cssClass="error-validation"/></dd>

    </dl>
    <input type=submit value="Применить изменения">
</form:form>
<jsp:include page="footer.jsp"/>
