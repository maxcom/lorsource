<%--
  ~ Copyright 1998-2013 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление метки</title>
<link rel="parent" title="Linux.org.ru" href="/">
<script src="/js/jqueryui/jquery-ui-1.10.3.custom.min.js" type="text/javascript"></script>
<script type="text/javascript">
  $(function() {
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
<link rel="stylesheet" href="/js/jqueryui/jquery-ui-1.10.3.custom.min.css">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class="nav">
  <div id="navPath">
    Удаление метки
  </div>
</div>
<c:url var="delete_url" value="/tags/delete">
  <c:param name="firstLetter" value="${firstLetter}"/>
</c:url>
 <form:form modelAttribute="tagRequestDelete" method="POST" action="${delete_url}" enctype="multipart/form-data" >
  <form:errors path="*" element="div" cssClass="error"/>
  <form:hidden path="oldTagName" />
  Удаляемое название: ${tagRequestDelete.oldTagName}<br />
  Метка. которой нужно заменить удаляемую (пусто - удалить без замены):<br />
  <form:input id="tagName" path="tagName" style="width: 40em" /><br />
  <input type="submit" value="Удалить" />
  <c:url var="list_url" value="/tags/${firstLetter}" />
  <input type="button" value="Отменить" onClick="window.location='${list_url}';" />
</form:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
