<%@ page contentType="text/html; charset=utf-8"  %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
  response.addHeader("Pragma", "no-cache");
%>
<title>Список Игнорирования</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Список Игнорирования</h1>

<form action="ignore-list.jsp" method="POST">

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
<c:if test="${fn:length(ignoreList)>0}">
<select name="nickList" multiple="multiple" size="10" width="20">
  <c:forEach var="item" items="${ignoreList}">
    <option value="${item.value}">${item.value}</option>
  </c:forEach>
 </select>
<br>
  <input type="submit" name="del" value="Удалить">
</c:if>

</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>