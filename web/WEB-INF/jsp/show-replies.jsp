<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.LorDataSource"%>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<% 
	String nick=(String) request.getAttribute("nick");

	Connection db = null;

	try {
      db = LorDataSource.getConnection();
%>
<c:set var="title">
  Последние
    <c:if test="${firstPage}">
      ${topics}
    </c:if>
    <c:if test="${not firstPage}">
      ${offset+count} - ${offset}
    </c:if>
  ответов на комментарии пользователя ${nick}
</c:set>
<title>${title}</title>
<link rel="alternate" title="RSS" href="show-replies.jsp?output=rss&amp;nick=${nick}" type="application/rss+xml"/>
<link rel="alternate" title="Atom" href="show-replies.jsp?output=atom&amp;nick=${nick}" type="application/atom+xml"/>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>${title}</h1>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><td colspan=5>
<%
  boolean firstPage = (Boolean) request.getAttribute("firstPage");
  int offset = (Integer) request.getAttribute("offset");
  int topics = (Integer) request.getAttribute("topics");
%>
<div style="float: left"><%
  if (firstPage || (offset - topics)<0) {
	out.print("");
  } else {
	out.print("<a rel=prev rev=next href=\"show-replies.jsp?nick=" + nick + "&amp;offset=" + (offset - topics) + "\">← назад</a>");
  }
%>
</div>
    <div style="float: right">
<%
  	out.print("<a rel=next rev=prev href=\"show-replies.jsp?nick=" + nick + "&amp;offset=" + (offset + topics) + "\">вперед →</a>");
%>
      </div>

</td></tr>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Автор</th><th>Дата</th></tr>
<tbody>

<c:forEach var="topic" items="${topicsList}">
<tr>
  <td>${topic.sectionTitle}</td>
  <td>${topic.groupTitle}</td>
  <td><a href="jump-message.jsp?msgid=${topic.msgid}&amp;cid=${topic.cid}">${topic.subj}</a> </td>
  <td><lor:user db="<%= db %>" id="${topic.commentAuthor}" decorate="true"/></td>
  <td><lor:dateinterval date="${topic.commentDate}"/></td>
</tr>
</c:forEach>

</tbody>
<tfoot>
  <tr><td colspan=5><p>
<div style="float: left"><%
  if (firstPage || (offset - topics)<0) {
	out.print("");
  } else {
	out.print("<a rel=prev rev=next href=\"show-replies.jsp?nick=" + nick + "&amp;offset=" + (offset - topics) + "\">← назад</a>");
  }
%>
</div>
    <div style="float: right">
<%
  	out.print("<a rel=next rev=prev href=\"show-replies.jsp?nick=" + nick + "&amp;offset=" + (offset + topics) + "\">вперед →</a>");
%>
      </div>
  </td></tr>
</tfoot>
</table>
</div>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
