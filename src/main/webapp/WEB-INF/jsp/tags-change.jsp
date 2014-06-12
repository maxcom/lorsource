<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ page contentType="text/html; charset=utf-8"%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Изменение метки</title>
<link rel="parent" title="Linux.org.ru" href="/">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class="nav">
  <div id="navPath">
    Изменение метки
  </div>
</div>
<c:url var="change_url" value="/tags/change">
  <c:param name="firstLetter" value="${firstLetter}"/>
</c:url>
<form:form modelAttribute="tagRequestChange" method="POST" action="${change_url}" enctype="multipart/form-data" >
  <lor:csrf/>
  <form:errors path="*" element="div" cssClass="error"/>
  <form:hidden path="oldTagName" />
  Старое название: ${tagRequestChange.oldTagName}<br />
  Название: <form:input path="tagName" required="required" style="width: 40em" /><br />
  <input type="submit" value="Изменить" />
  <c:url var="list_url" value="/tags/${firstLetter}" />
  <input type="button" value="Отменить" onClick="window.location='${list_url}';" />
</form:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
