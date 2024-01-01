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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление метки</title>
<link rel="parent" title="Linux.org.ru" href="/">
<script type="text/javascript">
  $script.ready("jquery", function() {
    $script("/js/jquery-ui-1.13.2.custom/jquery-ui.min.js", "jqueryui");
  });

  $script.ready("jqueryui", function() {
    $( "#tagName" )
            .bind( "keydown", function( event ) {
              if ( event.keyCode === $.ui.keyCode.TAB &&
                      $( this ).data( "autocomplete" ).menu.active ) {
                event.preventDefault();
              }
            })
            .autocomplete({
              source: function( request, response ) {
                $.getJSON( "/tags", {
                  term: request.term
                }, response );
              },
              search: function() {
                // custom minLength
                if ( this.value.length < 2 ) {
                  return false;
                }
              },
              focus: function() {
                // prevent value inserted on focus
                return false;
              },
              select: function( event, ui ) {
                this.value = ui.item.value;
                return false;
              }
            });
  });
 </script>
<link rel="stylesheet" href="/js/jquery-ui-1.13.2.custom/jquery-ui.min.css">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Удаление метки «${tagRequestDelete.oldTagName}»</h1>

<p><strong>Внимание!</strong> Удаление метки нельзя отменить. Изменение не отражается в истории правок топика.</p>

 <form:form modelAttribute="tagRequestDelete" method="POST" action="/tags/delete" enctype="multipart/form-data">
  <lor:csrf/>
  <form:errors path="*" element="div" cssClass="error"/>
  <form:hidden path="oldTagName" />

  <c:if test="${not synonym}">
  <div class="control-group">
   <label for="tagName">Метка, которой нужно заменить удаляемую (пусто - удалить без замены):</label>
   <form:input autofocus="autofocus" autocapitalize="off" id="tagName" path="tagName" style="width: 40em" />
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
    <button type="button" class="btn btn-default" onClick="window.location='${list_url}';">Отменить</button>
  </div>
 </form:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
