<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="blocks" type="java.util.List<ru.org.linux.admin.PreparedEmailDomainBlock>"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="limit" type="java.lang.Integer"--%>
<%--@elvariable id="hasMore" type="java.lang.Boolean"--%>
<%--@elvariable id="count" type="java.lang.Long"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Блокировка почтовых доменов</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Блокировка почтовых доменов</h1>

<fieldset>
  <legend>Добавить домен в блок</legend>
  <form method="post" action="/admin/email-domains/add">
    <lor:csrf/>
    <label>Домен:
      <input type="text" name="domain" size="40" maxlength="255" required
             placeholder="example.com" pattern="[a-z0-9]([a-z0-9.-]*[a-z0-9])?">
    </label>
    <button type="submit" class="btn btn-default">Заблокировать на 3 года</button>
  </form>
  <p class="info">Ручная блокировка добавляется на 3 года. Если домен уже заблокирован, срок продлевается.
  Ручная блокировка перекрывает автоматическую.</p>
</fieldset>

<c:choose>
  <c:when test="${not empty blocks}">
    <div class="forum">
      <table class="message-table" width="100%">
        <thead>
        <tr>
          <th>Домен</th>
          <th>Заблокирован до</th>
          <th>Кем заблокирован</th>
          <th>Когда</th>
          <th>&nbsp;</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${blocks}" var="block">
          <tr>
            <td><c:out value="${block.domain}" escapeXml="true"/></td>
            <td><lor:date date="${block.blockUntil}"/></td>
            <td>
              <c:if test="${block.moderator != null}">
                <lor:user user="${block.moderator}"/>
              </c:if>
              <c:if test="${block.moderator == null}">
                &mdash;
              </c:if>
            </td>
            <td><lor:date date="${block.blockedAt}"/></td>
            <td>
              <form method="post" action="/admin/email-domains/delete" style="display:inline">
                <lor:csrf/>
                <input type="hidden" name="domain" value="${block.domain}">
                <button type="submit" class="btn btn-danger btn-xs">удалить</button>
              </form>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>

    <div class="nav">
      <div style="display: table; width: 100%">
        <c:if test="${offset != 0}">
          <div style="display: table-cell; text-align: left">
            <a href="/admin/email-domains?offset=${offset - limit}">&larr; предыдущие</a>
          </div>
        </c:if>
        <c:if test="${hasMore}">
          <div style="display: table-cell; text-align: right">
            <a href="/admin/email-domains?offset=${offset + limit}">следующие &rarr;</a>
          </div>
        </c:if>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <p>Доменов, заблокированных вручную, нет.</p>
  </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>