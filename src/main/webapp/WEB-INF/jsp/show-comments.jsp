<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
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
<%--@elvariable id="firstPage" type="Boolean"--%>
<%--@elvariable id="user" type="ru.org.linux.user.User"--%>
<%--@elvariable id="topics" type="Integer"--%>
<%--@elvariable id="offset" type="Integer"--%>
<%--@elvariable id="list" type="java.util.List<ru.org.linux.spring.ShowCommentsController.CommentsListItem>"--%>
<%--@elvariable id="deletedList" type="java.util.List<ru.org.linux.comment.CommentDao.DeletedListItem>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  int offset = (Integer) request.getAttribute("offset");
  int topics = (Integer) request.getAttribute("topics");
%>
<title>Комментарии пользователя ${user.nick}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Комментарии пользователя ${user.nick}</h1>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th colspan=5>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>  
</div>
<div style="float: right">
<c:if test="${fn:length(list)==topics and (offset+topics<1000)}">
  <a rel=next rev=prev href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset + topics %>">вперед →</a>
</c:if>
</div>
</th></tr>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>

<c:forEach items="${list}" var="comment">
<tr>
  <td>${comment.sectionTitle}</td>
  <td>${comment.groupTitle}</td>
  <td><a href="jump-message.jsp?msgid=${comment.topicId}&amp;cid=${comment.commentId}" rev=contents><l:title>${comment.title}</l:title></a></td>
  <td><lor:dateinterval date="${comment.postdate}"/></td>
</c:forEach>

</tbody>
<tfoot>
  <tr><td colspan=5><p>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>  
</div>
<div style="float: right">
  <c:if test="${fn:length(list)==topics and (offset+topics<1000)}">
    <a rel=next rev=prev href="show-comments.jsp?nick=${user.nick}&amp;offset=<%= offset + topics %>">вперед →</a>
  </c:if>
</div>
  </td></tr>
</tfoot>
</table>
</div>

<c:if test="${deletedList != null}">
  <h2>Последние 20 удаленных модераторами комментариев</h2>

  <div class=forum>
    <table width="100%" class="message-table">
      <thead>
      <tr>
        <th>Раздел</th>
        <th>Группа</th>
        <th>Комментарий</th>
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
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
