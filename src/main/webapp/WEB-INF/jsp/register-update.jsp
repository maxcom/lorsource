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
<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
%>
  <table class=nav><tr>
    <td id="navPath" align=left valign=middle>
      Изменение регистрации
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="../../addphoto.jsp">Добавить фотографию</a>]
      [<a style="text-decoration: none" href="../../rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>
<h1 class="optional">Изменение регистрации</h1>

<form:form modelAttribute="form" method="POST" action="register.jsp" id="changeForm">

    <form:errors path="*" element="div" cssClass="error"/>
    
<input type=hidden name=mode value="change">
<label> Полное имя: <form:input path="name" size="40"/></label><br>
<label>Пароль:
<input class="required" type=password name="oldpass" size="20"></label><br>
<label>Новый пароль:
<input type=password name="password" size="20"> (не заполняйте если не хотите менять)</label><br>
<label>Повторите новый пароль:
<input type=password name="password2" size="20"></label><br>

<label>URL (не забудьте добавить <b>http://</b>): <form:input path="url" size="50"/></label><br>

<label>Email: <form:input path="email" size="50" cssClass="required email"/></label><br>

<label>
Город (просьба писать русскими буквами без сокращений, например: <b>Москва</b>,
<b>Нижний Новгород</b>, <b>Троицк (Московская область)</b>):
<form:input path="town" size="50" maxlength="100"/></label> <br>

<label>Дополнительная информация:<br>
  <form:textarea path="info" cols="50" rows="5"/>
</label>
<br>
<input type=submit value="Обновить">
</form:form>
<jsp:include page="footer.jsp"/>
