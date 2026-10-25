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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление метки</title>
<link rel="parent" title="Linux.org.ru" href="/">
<script type="text/javascript">
  $script.ready("plugins", function() {
    $script("/js/tagsAutocomplete.js");
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Удаление метки «<c:out value="${tagRequestDelete.oldTagName}"/>»</h1>

<p><strong>Внимание!</strong> Удаление метки нельзя отменить. Изменение не отражается в истории правок топика.</p>

 <form:form modelAttribute="tagRequestDelete" method="POST" action="/tags/delete">
  <lor:csrf/>
  <form:errors path="*" element="div" cssClass="error"/>
  <form:hidden path="oldTagName" />

  <c:if test="${not synonym}">
  <div class="control-group">
   <label for="tagName">Метка, которой нужно заменить удаляемую (пусто - удалить без замены):</label>
   <form:input autofocus="autofocus" autocapitalize="off" data-tags-autocomplete-single="data-tags-autocomplete-single" id="tagName" path="tagName" style="width: 40em" />
  </div>

  <div class="control-group">
   <label>
    <form:checkbox id="createSynonym" path="createSynonym"/>
    создать синоним
   </label>
  </div>
  </c:if>

  <div class="form-actions">
    <button type="submit" class="btn btn-danger">Удалить</button>
    <c:url var="list_url" value="/tags/${firstLetter}"/>
    <a class="btn btn-default" href="${list_url}">Отменить</a>
  </div>
 </form:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
