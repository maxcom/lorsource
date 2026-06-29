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
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--@elvariable id="user" type="ru.org.linux.user.User"--%>
<%--@elvariable id="deletedList" type="java.util.List<ru.org.linux.comment.CommentDao.DeletedListItem>"--%>
<%--@elvariable id="filters" type="java.util.List<ru.org.linux.comment.DeletedCommentsFilterEnum>"--%>
<%--@elvariable id="filter" type="java.lang.String"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${title}</h1>

<nav>
  <c:forEach items="${filters}" var="f">
    <c:url var="fUrl" value="/people/${user.nick}/deleted-comments">
      <c:if test="${f.value != 'all'}">
        <c:param name="filter">${f.value}</c:param>
      </c:if>
    </c:url>
    <c:if test="${filter != f.value}">
      <a class="btn btn-default" href="${fUrl}">${f.label}</a>
    </c:if>
    <c:if test="${filter == f.value}">
      <a href="${fUrl}" class="btn btn-selected">${f.label}</a>
    </c:if>
  </c:forEach>
</nav>

<div class=forum>
  <table width="100%" class="message-table">
    <thead>
    <tr>
      <th>Группа</th>
      <th>Заглавие темы</th>
      <th>Причина удаления</th>
      <th>Штраф</th>
      <th>Дата</th>
    </tr>
    <tbody>
    <c:forEach items="${deletedList}" var="item">

    <tr>
      <td>${item.gtitle}</td>
      <td>
        <c:if test="${item.topicDeleted}">
          <s>
        </c:if>

        <c:if test="${item.deleted}">
          <a href="/view-deleted?id=${item.commentId}#comment-${item.commentId}"><l:title>${item.title}</l:title></a>
        </c:if>

        <c:if test="${not item.deleted}">
          <a href="jump-message.jsp?msgid=${item.msgid}&cid=${item.commentId}"><l:title>${item.title}</l:title></a>
        </c:if>

        <c:if test="${item.topicDeleted}">
          </s>
        </c:if>
      </td>
      <td><c:out value="${item.reason}" escapeXml="true"/></td>
      <td>${item.bonus}</td>
      <td>
        <c:if test="${item.delDate != null}">
          <lor:dateinterval date="${item.delDate}"/>
        </c:if>
      </td>
    </tr>
    </c:forEach>

  </table>
</div>

<div class="nav">
  <div style="display: table; width: 100%">
    <div style="display: table-cell; text-align: left">
      <c:if test="${not empty prevLink}">
        <a href="${prevLink}" rel="prev">← предыдущие</a>
      </c:if>
    </div>
    <div style="display: table-cell; text-align: right">
      <c:if test="${not empty nextLink}">
        <a href="${nextLink}" rel="next">следующие →</a>
      </c:if>
    </div>
  </div>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
