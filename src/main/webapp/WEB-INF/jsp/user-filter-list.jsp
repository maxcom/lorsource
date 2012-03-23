<%@ page contentType="text/html; charset=utf-8"  %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="ignoreList" type="java.util.Map<Integer, User>"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
  response.addHeader("Pragma", "no-cache");
%>
<title>Фильтрация сообщений</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Фильтрация сообщений</h1>
<fieldset>
<legend>Список игнорирования пользователей</legend>
<form action="<c:url value="/user-filter/ignore-user"/>" method="POST">

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
</form>

<c:if test="${fn:length(ignoreList)>0}">
  <ul>
    <c:forEach var="item" items="${ignoreList}">
      <li>
        <form action="<c:url value="/user-filter/ignore-user"/>" method="POST">
          <input type="hidden" name="id" value="${item.key}">
          <span style="white-space: nowrap"><img alt="" src="/img/tuxlor.png"><lor:user user="${item.value}" decorate="true" link="true"/> </span>
          <input type="submit" name="del" value="Удалить">
        </form>
      </li>
    </c:forEach>
  </ul>
<br>

</c:if>
</fieldset>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>