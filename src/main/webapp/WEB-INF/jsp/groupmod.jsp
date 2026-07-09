<%@ page session="false" %>
<%--
  ~ Copyright 1998-2026 Linux.org.ru
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

<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="groupInfo" type="ru.org.linux.group.PreparedGroupInfo"--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="head.jsp"/>

<title>Правка группы</title>
<script type="text/javascript">
  $script('/js/add-form.js?MAVEN_BUILD_TIMESTAMP', function() {
    setupFormWithSpinner({
      formSelector: '#groupModForm',
      textareaSelector: '#form_longinfo'
    });
  });
</script>
<jsp:include page="header.jsp"/>

<h1>
  Правка группы <c:out value="${group.title}"/>
  <c:if test="${preview}"> - Предпросмотр</c:if>
</h1>

<lor:groupinfo group="${groupInfo}"/>

<c:if test="${not empty error}">
  <div class="error"><c:out value="${error}"/></div>
</c:if>

<form id="groupModForm" action="groupmod.jsp" method="POST">
  <lor:csrf/>
  <input type="hidden" name="group" value="${group.id}">
  <label>Заголовок: <input type="text" name="title" size="70" value="${fn:escapeXml(group.title)}"<c:if test="${not currentUser.administrator}"> readonly</c:if>></label><br>
  <label>Строка описания: <input type="text" name="info" size="70" value="${fn:escapeXml(group.info)}"></label><br>
  <label>Имя для URL: <input type="text" name="urlName" size="70" value="${fn:escapeXml(group.urlName)}"<c:if test="${not currentUser.administrator}"> readonly</c:if>></label><br>
  <label>Можно помечать темы как решенные: <input type="checkbox" name="resolvable" <c:if test="${group.resolvable}">checked="checked"</c:if>></label><br>
  <label>Подробное описание:</label><br>
  <div class="control-group" data-format-mode="markdown">
    <div class="markup-tabs">
      <ul class="markup-tabs__nav">
        <li class="markup-tabs__tab active" data-tab="editor">Markdown</li>
      </ul>
      <div class="markup-tabs__content">
        <div class="markup-tabs__panel active" data-panel="editor">
          <textarea rows="20" cols="70" name="longinfo" id="form_longinfo"><c:out value="${group.longInfo}" escapeXml="true"/></textarea>
        </div>
      </div>
    </div>
    <div class="help-block"><lor:markup-help mode="markdown"/></div>
  </div>
  <div class="form-actions">
    <input type="submit" value="Изменить">
    <button type="submit" name="preview" class="btn btn-default">Предпросмотр</button>
  </div>
</form>

<jsp:include page="footer.jsp"/>