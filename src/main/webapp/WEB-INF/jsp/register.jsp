<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<%--@elvariable id="error" type="java.lang.String"--%>
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
<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");

     session.setAttribute("register-visited", Boolean.TRUE);

%>
<H1>Регистрация</H1>
Если вы уже регистрировались на нашем сайте и забыли пароль - вам
<a href="../../lostpwd.jsp">сюда</a>.

<c:if test="${error!=null}">
  <div class="error">Ошибка: ${error}</div>
</c:if>

<form method=POST action="register.jsp" id="registerForm">
<b>Login:</b>
<input class="required" type=text name=nick size=40 value="<c:out value="${param.nick}" escapeXml="true"/>"><br>
Полное имя:
<input type=text name=name size=40 value="<c:out value="${param.name}" escapeXml="true"/>"><br>
<b>Пароль:</b>
<input class="required" id="password" type=password name=password size=20 maxlength="40"><br>
<b>Повторите Пароль:</b>
<input class="required" id="password2" type=password name=password2 size=20 maxlength="40"><br>
URL (не забудьте добавить <b>http://</b>): <br>
<input type=text name=url size="50" value="<c:out value="${param.url}" escapeXml="true"/>"><br>
<b>E-mail</b> (ваш email не будет публиковаться на сайте):<br>
<input class="required email" type=text name=email size="50" value="<c:out value="${param.email}" escapeXml="true"/>"><br>
Город (просьба писать русскими буквами без сокращений, например: <b>Москва</b>,
<b>Нижний Новгород</b>, <b>Троицк (Московская область)</b>):
<input type=text name=town maxlength="100" size=50 value="<c:out value="${param.town}" escapeXml="true"/>"><br>
Дополнительная информация:<br>
<textarea name=info cols=50 rows=5><c:out value="${param.info}" escapeXml="true"/></textarea><br>
<p>
  <lor:captcha/>

<br>
<input type=submit value="Зарегистрироваться">
</form>
<jsp:include page="footer.jsp"/>
