<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.user.User,java.util.Date"   buffer="60kb"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="remarks" type="java.util.List<ru.org.linux.user.PreparedRemark>"--%>
<%--@elvariable id="offset" type="java.lang.Integer>"--%>
<%--@elvariable id="sortorder" type="java.lang.String>"--%>
<%--@elvariable id="hasMore" type="java.lang.Boolean>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Просмотр комментариев о пользователях</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <div class=nav>
    <div id="navPath">
      Просмотр комментариев о пользователях
    </div>

  </div>

<h2>Комментарии пользователя ${template.currentUser.nick}</h2>
<c:choose>
  <c:when test="${not empty remarks}">
    <div class=forum>
    <table class="message-table" width="100%">
    <thead>
    <tr>
    <th><a href="/people/${template.currentUser.nick}/remarks/?offset=${offset}&amp;sort=0">Ник</a></th>
    <th><a href="/people/${template.currentUser.nick}/remarks/?offset=${offset}&amp;sort=1">Комментарий</a></th>
    </tr>
    <tbody>

    <c:forEach items="${remarks}" var="remark">
      <tr>
      <td><lor:user link="true" user="${remark.refUser}"/> ${remark.refUser.stars}</td>
      <td><a href="/people/${remark.refUser.nick}/remark/">
        <c:out value="${remark.remark.text}" escapeXml="true"/></a></td>
      </tr>
    </c:forEach>
    </table>

    </div>

    <div class="nav">
    <div style="display: table; width: 100%">
      <c:if test="${offset !=0}">
        <div style="display: table-cell; text-align: left">
          <a href="/people/${template.currentUser.nick}/remarks/?offset=${offset-limit}${sortorder}">← предыдущие</a>
        </div>
      </c:if>
      <c:if test="${hasMore}">
        <div style="display: table-cell; text-align: right">
          <a href="/people/${template.currentUser.nick}/remarks/?offset=${offset+limit}${sortorder}">следующие →</a>
        </div>
      </c:if>
    </div>
    </div>
  </c:when>
  <c:otherwise>
    <p>Заметок нет.</p>
  </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

