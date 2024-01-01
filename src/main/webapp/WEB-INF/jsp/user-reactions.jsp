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
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>${title}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${title}</h1>

<div class="reactions-view">
  <p>
    <c:if test="${modeTo == 1}">
      <a class="btn btn-default" href="${url}">${meTitle}</a>
      <a class="btn btn-selected" href="${reactionsUrl}">${reactionsTitle}</a>
    </c:if>
    <c:if test="${modeTo != 1}">
      <a class="btn btn-selected" href="${url}">${meTitle}</a>
      <a class="btn btn-default" href="${reactionsUrl}">${reactionsTitle}</a>
    </c:if>
   </p>
<c:forEach var="item" items="${items}">
  <a class="reactions-view-item" href="${item.link}">
    <div class="reactions-view-reaction">
      <p>
        ${item.item.reaction}
      </p>
    </div>

    <div class="reactions-view-title">
      <p>
        <c:if test="${item.comment}"><i class="icon-comment"></i></c:if>
      ${item.title}
      </p>
    </div>

    <div class="reactions-view-date">
      <p>
        <lor:dateinterval date="${item.item.setDate}" compact="true"/>
      </p>
    </div>

    <div class="reactions-view-preview">
      <div class="text-preview-box">
        <div class="text-preview">
          <lor:user link="false" user="${item.targetUser}"/>: <c:out value="${item.textPreview}" escapeXml="true"/>
        </div>
      </div>
    </div>
  </a>
</c:forEach>
</div>

<table class="nav">
  <tr>
    <c:if test="${not empty prevUrl}">
      <td width="35%" align="left">
        <a href="${prevUrl}">← предыдущие</a>
      </td>
    </c:if>
    <c:if test="${not empty nextUrl}">
      <td align="right" width="35%">
        <a href="${nextUrl}">следующие →</a>
      </td>
    </c:if>
  </tr>
</table>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

