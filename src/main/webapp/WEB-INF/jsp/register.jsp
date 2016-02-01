<%@ page import="ru.org.linux.user.User" %>
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

<title>Регистрация пользователя</title>
<script type="text/javascript">
  $script.ready("plugins", function() {
    $(function() {
      $("#registerForm").validate({
        errorElement : "span",
        errorClass : "error help-inline",
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
<H1>Регистрация</H1>
<p>
Если вы уже регистрировались на нашем сайте и забыли пароль - вам
<a href="/lostpwd.jsp">сюда</a>.
</p>

<form:form modelAttribute="form" method="POST" action="register.jsp" id="registerForm">
    <lor:csrf/>
    <form:errors element="div" cssClass="error"/>

  <div class="control-group">
    <label for="nick">Login</label>
    <form:input path="nick" required="required" size="40" cssErrorClass="error"
                title="Только латинские буквы, цифры и знаки _-, в первом символе только буквы"
                pattern="[a-zA-Z][a-zA-Z0-9_-]*"
                autofocus="autofocus" maxlength="<%= Integer.toString(User.MAX_NICK_LENGTH) %>"/>
    <form:errors path="nick" element="span" cssClass="error help-inline" for="nick"/>
    <div class="help-block">
      мы сохраняем регистр, в котором введен логин
    </div>
  </div>

  <div class="control-group">
    <label for="email">E-mail</label>
    <form:input path="email" type="email" required="required" cssClass="email" size="40" cssErrorClass="error"/>
    <form:errors path="email" element="span" cssClass="error help-inline" for="email"/>
  </div>

  <div class="control-group">
    <label for="password">Пароль</label>
    <form:password path="password" size="40" required="required" cssErrorClass="error" minlength="5"/>
    <form:errors path="password" element="span" cssClass="error help-inline" for="password"/>
  </div>

  <div class="control-group">
    <label for="password2">Подтвердите пароль</label>
    <form:password path="password2" size="40" required="required" cssErrorClass="error"/>
    <form:errors path="password2" element="span" cssClass="error help-inline" for="password"/>
  </div>

  <div class="control-group">
    <lor:captcha/>
  </div>

  <div class="control-group">
    <label for="rules">С
      <a href="/help/rules.md" target="_blank" title="правила откроются в новом окне">правилами</a> ознакомился:
      <form:checkbox path="rules" id="rules" value="okay" required="required" cssErrorClass="error"/>
      <form:errors path="rules" element="span" cssClass="error help-inline" for="rules"/></label>
  </div>

  <div class="form-actions">
    <button type=submit class="btn btn-primary">Зарегистрироваться</button>
  </div>
</form:form>
<jsp:include page="footer.jsp"/>
