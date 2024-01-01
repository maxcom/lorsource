<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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

<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="remark" type="ru.org.linux.user.Remark"--%>
<%--@elvariable id="currentUser" type="ru.org.linux.user.User"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Комментарий о пользователе</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Комментарий ${currentUser.nick} о пользователе ${nick}</h1>

<form method=POST id="remarkForm" action="/people/${nick}/remark">
<lor:csrf/>

<textarea autofocus id="text" name="text" cols="60" rows="4" maxlength="255"><c:out value="${remark.text}" escapeXml="true"/></textarea>
<br>
<input type=submit value="Установить">
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
