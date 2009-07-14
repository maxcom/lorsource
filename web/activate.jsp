<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<jsp:include page="WEB-INF/jsp/head.jsp"/>

<title>Активация</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<h1>Активация</h1>

<form method=POST action="login.jsp">
  <table>
    <tr>
      <td>Nick:</td>
      <td><input type=text name=nick></td>
    </tr>

    <tr>
      <td>Пароль:</td>
      <td><input type=password name=passwd></td>
    </tr>

    <tr>
      <td>Код активациии:</td>
      <td><input type=text name=activate></td>
    </tr>

  </table>

  <input type=submit value="Активировать">
</form>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
