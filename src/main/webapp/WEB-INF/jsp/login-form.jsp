<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2018 Linux.org.ru
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
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Login</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Вход</h1>

<form method=POST action="${configuration.secureUrlWithoutSlash}/login_process">
  <c:if test="${param.error == 'true'}">
    <div class="error">Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.</div>
  </c:if>

  <c:if test="${param.error == 'not_activated'}">
    <div class="error">
      Регистрация не завершена! Инструкция по активации отправлена на указанный при регистрации email.
    </div>
  </c:if>

  <lor:csrf/>
  <label>Имя/email:<br><input autofocus autocapitalize="off" type=text name=nick size=40></label>
  <label>Пароль:<br><input type=password name=passwd size=40></label>
  <button type=submit class="btn btn-primary">Вход</button>
</form>

<div style="font-size: smaller">
(<a href="register.jsp">Регистрация</a> | <a href="lostpwd.jsp">Получить забытый пароль</a>)
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
