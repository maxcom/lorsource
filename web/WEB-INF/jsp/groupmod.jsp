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

<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>
<%--@elvariable id="groupInfo" type="ru.org.linux.site.PreparedGroupInfo"--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="head.jsp"/>

<title>Правка группы</title>
<jsp:include page="header.jsp"/>

<h1>
  Правка группы ${group.title}
  <c:if test="${preview}"> - Предпросмотр</c:if>
</h1>

<lor:groupinfo group="${groupInfo}"/>

<form action="groupmod.jsp" method="POST">
  <input type="hidden" name="group" value="${group.id}">
  Строка описания: <input type="text" name="info" size="70" value="${fn:escapeXml(group.info)}"><br>
  <label>Имя для URL: <input type="text" name="urlName" size="70" value="${fn:escapeXml(group.urlName)}"></label><br>
  Можно помечать темы как решенные: <input type="checkbox" name="resolvable" <c:if test="${group.resolvable}">checked="checked"</c:if>/><br />
  Подробное описание:<br>
  <textarea rows="20" cols="70" name="longinfo"><c:out value="${group.longInfo}" escapeXml="true"/></textarea><br>
  <input type="submit" value="Изменить">
  <input type="submit" name="preview" value="Предпросмотр">
</form>

<jsp:include page="footer.jsp"/>

