<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2021 Linux.org.ru
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
<%--@elvariable id="user" type="ru.org.linux.user.User"--%>
<%--@elvariable id="deletedList" type="java.util.List<ru.org.linux.comment.CommentDao.DeletedListItem>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Последние 20 удаленных модераторами комментариев ${user.nick}</h1>

<div class=forum>
  <table width="100%" class="message-table">
    <thead>
    <tr>
      <th>Раздел</th>
      <th>Группа</th>
      <th>Заглавие темы</th>
      <th>Причина удаления</th>
      <th>Бонус</th>
      <th>Дата</th>
    </tr>
    <tbody>
    <c:forEach items="${deletedList}" var="item">

    <tr>
      <td>${item.ptitle}</td>
      <td>${item.gtitle}</td>
      <td>
        <a href="jump-message.jsp?msgid=${item.msgid}&cid=${item.commentId}"><l:title>${item.title}</l:title></a>
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
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
