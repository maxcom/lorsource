<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.LorDataSource"%>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.spring.ShowRepliesController.MyTopicsListItem>"--%>
<%--@elvariable id="firstPage" type="Boolean"--%>
<%--@elvariable id="nick" type="String"--%>
<%--@elvariable id="hasMore" type="String"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<% 
	Connection db = null;

	try {
      db = LorDataSource.getConnection();
%>
<c:set var="title">
  Уведомления пользователя ${nick}
</c:set>
<title>${title}</title>
<link rel="alternate" title="RSS" href="show-replies.jsp?output=rss&amp;nick=${nick}" type="application/rss+xml">
<link rel="alternate" title="Atom" href="show-replies.jsp?output=atom&amp;nick=${nick}" type="application/atom+xml">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<table class=nav>
<tr>
<td align=left valign=middle id="navPath">
  ${title}
</td>
  <td align=right>
    <ul>
      <li>[<a href="show-replies.jsp?output=rss&amp;nick=${nick}">RSS</a>]</li>
    </ul>
  </td>
</table>

<h1 class="optional">${title}</h1>

<%
  int offset = (Integer) request.getAttribute("offset");
  int topics = (Integer) request.getAttribute("topics");
%>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-replies.jsp?nick=${nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>
</div>

<div style="float: right">
<c:if test="${hasMore}">
  <a rel=next rev=prev href="show-replies.jsp?nick=${nick}&amp;offset=<%= offset + topics %>">вперед →</a>
</c:if>
</div>

<p style="clear: both;"> </p>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th></th><th>Группа</th><th>Заголовок</th><th>Комментарий</th></tr>
<tbody>

<c:forEach var="topic" items="${topicsList}">
<tr>
  <td align="center">
    <c:choose>
      <c:when test="${topic.type == 'DEL'}">
        <img src="/img/del.png" border="0" alt="[X]" title="Сообщение удалено" width="15" height="15">
      </c:when>
      <c:when test="${topic.type == 'REPLY'}">
        <img src="/img/mail_reply.png" border="0" title="Ответ" alt="[R]" width="16" height="16">
      </c:when>
    </c:choose>
  </td>
  <td><a href="${topic.groupUrl}">${topic.groupTitle}</a></td>
  <td>
    <c:if test="${topic.type != 'DEL'}">
      <a href="jump-message.jsp?msgid=${topic.msgid}&amp;cid=${topic.cid}">${topic.subj}</a>
    </c:if>

    <c:if test="${topic.type == 'DEL'}">
      <a href="view-message.jsp?msgid=${topic.msgid}">${topic.subj}</a>
      <br>
      <c:out value="${topic.eventMessage}" escapeXml="true"/>
    </c:if>
  </td>
  <td><lor:dateinterval date="${topic.commentDate}"/> (<lor:user db="<%= db %>" id="${topic.commentAuthor}" decorate="true"/>)</td>
</tr>
</c:forEach>

</tbody>
</table>
</div>
<p></p>
<div style="float: left">
<c:if test="${not firstPage}">
  <a rel=prev rev=next href="show-replies.jsp?nick=${nick}&amp;offset=<%= offset - topics %>">← назад</a>
</c:if>
</div>

<div style="float: right">
<c:if test="${hasMore}">
  <a rel=next rev=prev href="show-replies.jsp?nick=${nick}&amp;offset=<%= offset + topics %>">вперед →</a>
</c:if>
</div>


<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
