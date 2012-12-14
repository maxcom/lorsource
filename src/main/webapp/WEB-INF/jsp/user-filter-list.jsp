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
<%--@elvariable id="ignoreRemarks" type="java.util.Map<Integer, Remark>"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
  response.addHeader("Pragma", "no-cache");
%>
<title>Фильтрация сообщений</title>

<script src="/js/jqueryui/jquery-ui-1.8.18.custom.min.js" type="text/javascript"></script>
<script src="/js/tagsAutocomplete.js" type="text/javascript"></script>
<script type="text/javascript">
  document.tagInputCssString = "#newFavoriteTagName, #newIgnoreTagName";
</script>
<link rel="stylesheet" href="/js/jqueryui/jquery-ui-1.8.18.custom.css">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Фильтрация сообщений</h1>

<c:if test="${empty newFavoriteTagName and empty newIgnoreTagName}">

<fieldset>
<legend>Список игнорирования пользователей</legend>
<form action="<c:url value="/user-filter/ignore-user"/>" method="POST">
  <lor:csrf/>

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
</form>

<c:if test="${fn:length(ignoreList)>0}">
  <ul>
    <c:forEach var="item" items="${ignoreList}">
      <li>
        <form action="<c:url value="/user-filter/ignore-user"/>" method="POST">
          <lor:csrf/>
          <input type="hidden" name="id" value="${item.key}">
          <span style="white-space: nowrap"><img alt="" src="/img/tuxlor.png"><lor:user user="${item.value}" link="true"/> </span>
          <c:if test="${not empty ignoreRemarks[item.key]}">
            <c:out escapeXml="true" value="${ignoreRemarks[item.key].text}"/>
          </c:if>
          <input type="submit" name="del" value="Удалить">
        </form>
      </li>
    </c:forEach>
  </ul>
</c:if>
</fieldset>

</c:if>

<br />

<c:if test="${empty newIgnoreTagName}">

<fieldset>
<legend>Список избранных тегов</legend>
<form action="<c:url value="/user-filter/favorite-tag"/>" method="POST">
  <lor:csrf/>
  <label>Тег: <input type="text" name="tagName" id="newFavoriteTagName" size="20" maxlength="80" value="${fn:escapeXml(newFavoriteTagName)}"></label>
  <input type="submit" name="add" value="Добавить">
  <c:if test="${favoriteTagAddError != null}"><div class="error">
  <c:forEach var="tagAddError" items="${favoriteTagAddError}">
    ${tagAddError}<br />
  </c:forEach>
  </div></c:if>
</form>

<c:if test="${fn:length(favoriteTags)>0}">
  <ul>
    <c:forEach var="tagName" items="${favoriteTags}">
      <li>
        <form action="<c:url value="/user-filter/favorite-tag"/>" method="POST">
          <lor:csrf/>
          <input type="hidden" name="tagName" value="${tagName}">
          <span style="white-space: nowrap">${tagName}</span>
          <input type="submit" name="del" value="Удалить">
        </form>
      </li>
    </c:forEach>
  </ul>
</c:if>
</fieldset>

</c:if>

<br />

<c:if test="${empty newFavoriteTagName}">

<fieldset>
<legend>Список игнорирования тегов</legend>
<c:choose>
<c:when test="${isModerator}">
Модераторам нельзя игнорировать теги
</c:when>
<c:otherwise>
<form action="<c:url value="/user-filter/ignore-tag"/>" method="POST">
  <lor:csrf/>
  <label>Тег: <input type="text" name="tagName" id="newIgnoreTagName" size="20" maxlength="80" value="${fn:escapeXml(newIgnoreTagName)}"></label>
  <input type="submit" name="add" value="Добавить">
  <c:if test="${ignoreTagAddError != null}"><div class="error">
  <c:forEach var="tagAddError" items="${ignoreTagAddError}">
    ${tagAddError}<br />
  </c:forEach>
  </div></c:if>
</form>

<c:if test="${fn:length(ignoreTags)>0}">
  <ul>
    <c:forEach var="tagName" items="${ignoreTags}">
      <li>
        <form action="<c:url value="/user-filter/ignore-tag"/>" method="POST">
          <lor:csrf/>
          <input type="hidden" name="tagName" value="${tagName}">
          <span style="white-space: nowrap">${tagName}</span>
          <input type="submit" name="del" value="Удалить">
        </form>
      </li>
    </c:forEach>
  </ul>
</c:if>
</c:otherwise>
</c:choose>
</fieldset>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
