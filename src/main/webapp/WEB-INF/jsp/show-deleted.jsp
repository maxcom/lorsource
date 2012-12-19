<%@ page contentType="text/html; charset=utf-8"%>
<%@ page buffer="60kb" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<spring:url value="/people/{nick}/profile" var="profileLink">
  <spring:param name="nick" value="${user.nick}" />
</spring:url>

<h1>${title} <lor:user user="${user}" link="true"/></h1>
  <c:if test="${listMessages.count > 1}"><div class="nav"><l:page paginationPrepared="${listMessages}" baseUrl="${baseUrl}"/></div></c:if>
  <div class="forum">
    <table width="100%" class="message-table">
      <thead>
      <tr>
        <th>Раздел</th>
        <th>Группа</th>
        <th>Заглавие темы</th>
        <th>Причина удаления</th>
        <th>Модератор</th>
        <th>Бонус</th>
        <th>Дата</th>
      </tr>
      </thead>
      <tbody>
      <c:forEach items="${listMessages.items}" var="item">
      <tr>
        <td>${l:escape(item.sectionTitle)}</td>
        <td>${l:escape(item.groupTitle)}</td>
        <td>
          <a href="view-message.jsp?msgid=${item.id}" rev="contents"><l:title>${item.title}</l:title></a>
        </td>
        <td>${l:escape(item.reason)}</td>
        <td>${l:escape(item.moderator.nick)}</td>
        <td>${l:escape(item.bonus)}</td>
        <td><lor:dateinterval date="${item.date}"/></td>
      </tr>
      </c:forEach>
      </tbody>

    </table>
  </div>
  <c:if test="${listMessages.count > 1}"><div class="nav"><l:page paginationPrepared="${listMessages}" baseUrl="${baseUrl}"/></div></c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

