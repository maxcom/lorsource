<%@ page contentType="text/html; charset=utf-8"  %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="ignoreList" type="java.util.Map<Integer, String>"--%>
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
</form>

<c:if test="${fn:length(ignoreList)>0}">
  <ul>
    <c:forEach var="item" items="${ignoreList}">
      <li>
        <form action="ignore-list.jsp" method="POST">
          <input type="hidden" name="id" value="${item.key}">
          <span style="white-space: nowrap"><img alt="" src="/img/tuxlor.png"><a style="text-decoration: none" href='/people/${item.value}/profile'>${item.value}</a></span>
          <input type="submit" name="del" value="Удалить">
        </form>
      </li>
    </c:forEach>
  </ul>
<br>

</c:if>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>