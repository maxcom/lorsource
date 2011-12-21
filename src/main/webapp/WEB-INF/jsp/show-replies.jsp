<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.user.RepliesListItem>"--%>
<%--@elvariable id="firstPage" type="Boolean"--%>
<%--@elvariable id="nick" type="String"--%>
<%--@elvariable id="hasMore" type="String"--%>
<%--@elvariable id="unreadCount" type="Integer"--%>
<%--@elvariable id="enableReset" type="Boolean"--%>
<%--@elvariable id="forceReset" type="Boolean"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<c:set var="title">
  Уведомления пользователя ${nick}
</c:set>
<title>${title}</title>
<link rel="alternate" title="RSS" href="show-replies.jsp?output=rss&amp;nick=${nick}" type="application/rss+xml">
<link rel="alternate" title="Atom" href="show-replies.jsp?output=atom&amp;nick=${nick}" type="application/atom+xml">
<script type="text/javascript">
  <!--
  $(document).ready(function() {
    $('#reset_form').ajaxSubmit(
      function() {
        $('#reset_form').hide();
      }
    );
  });
   -->
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<form:form commandName="notifications" ACTION="/notifications" method="GET">
<table class=nav>
<tr>
    <td align=left valign=middle id="navPath">
    ${title}
    </td>
    <td align=right>
        <form:select path="filter">
            <form:options items="${filter}" itemValue="value" itemLabel="label"/>
        </form:select>
        <input type="submit" value="показать">
    </td>
    <td align=right>
        <ul>
        <li>[<a href="show-replies.jsp?output=rss&amp;nick=${nick}">RSS</a>]</li>
        </ul>
    </td>
</table>
</form:form>

<h1 class="optional">${title}</h1>

<c:if test="${unreadCount > 0 && !forceReset}">
  <div id="counter_block" class="infoblock">
    <c:choose>
      <c:when test="${unreadCount == 1 || (unreadCount>20 && unreadCount%10==1) }">
        У вас ${unreadCount} непрочитанное уведомление
      </c:when>
      <c:when test="${unreadCount == 2 || (unreadCount>20 && unreadCount%10==2)}">
        У вас ${unreadCount} непрочитанных уведомления
      </c:when>
      <c:when test="${unreadCount == 3 || (unreadCount>20 && unreadCount%10==3)}">
        У вас ${unreadCount} непрочитанных уведомления
      </c:when>
      <c:otherwise>
        У вас ${unreadCount} непрочитанных уведомлений
      </c:otherwise>
    </c:choose>

    <c:if test="${enableReset}">
      <form id="reset_form" action="/notifications" method="POST" style="display: inline;">
        <input type="hidden" name="forceReset" value="true">
        <input type="submit" value="Сбросить">
      </form>
    </c:if>
  </div>
</c:if>

<div style="float: left">
<c:if test="${not firstPage}">
<c:choose>
<c:when test="${not isMyNotifications}">
  <a rel=prev rev=next href="show-replies.jsp?nick=${nick}&amp;offset=${offset-topics}${addition_query}">← назад</a>
</c:when>
<c:otherwise>
  <a rel=prev rev=next href="notifications?offset=${offset-topics}${addition_query}">← назад</a>
</c:otherwise>
</c:choose>
</c:if>
</div>

<div style="float: right">
<c:if test="${hasMore}">
<c:choose>
<c:when test="${not isMyNotifications}">
  <a rel=next rev=prev href="show-replies.jsp?nick=${nick}&amp;offset=${offset+topics}${addition_query}">вперед →</a>
</c:when>
<c:otherwise>
  <a rel=next rev=prev href="notifications?offset=${offset+topics}${addition_query}">вперед →</a>
</c:otherwise>
</c:choose>
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
      <c:when test="${topic.type == 'REF'}">
        <img src="/img/tuxlor.png" border="0" title="Упоминание" alt="[U]" width="7" height="16">
      </c:when>
    </c:choose>
  </td>
  <td><a href="${topic.groupUrl}">${topic.groupTitle}</a></td>
  <td>
    <c:if test="${topic.type != 'DEL'}">
      <c:if test="${topic.cid>0}">
        <a href="jump-message.jsp?msgid=${topic.msgid}&amp;cid=${topic.cid}">${topic.subj}</a>
      </c:if>
      <c:if test="${topic.cid==0}">
        <a href="jump-message.jsp?msgid=${topic.msgid}">${topic.subj}</a>
      </c:if>
    </c:if>

    <c:if test="${topic.type == 'DEL'}">
      <a href="view-message.jsp?msgid=${topic.msgid}">${topic.subj}</a>
      <br>
      <c:out value="${topic.eventMessage}" escapeXml="true"/>
    </c:if>

    <c:if test="${topic.unread}">&bull;</c:if>
  </td>
  <td>
    <lor:dateinterval date="${topic.eventDate}"/>

    <c:if test="${topic.cid != 0}">
       (<lor:user user="${topic.commentAuthor}" decorate="true"/>)
    </c:if>
  </td>
</tr>
</c:forEach>

</tbody>
</table>
</div>
<p></p>
<div style="float: left">
<c:if test="${not firstPage}">
<c:choose>
<c:when test="${not isMyNotifications}">
  <a rel=prev rev=next href="show-replies.jsp?nick=${nick}&amp;offset=${offset-topics}${addition_query}">← назад</a>
</c:when>
<c:otherwise>
  <a rel=prev rev=next href="notifications?offset=${offset-topics}${addition_query}">← назад</a>
</c:otherwise>
</c:choose>
</c:if>
</div>

<div style="float: right">
<c:if test="${hasMore}">
<c:choose>
<c:when test="${not isMyNotifications}">
  <a rel=next rev=prev href="show-replies.jsp?nick=${nick}&amp;offset=${offset+topics}${addition_query}">вперед →</a>
</c:when>
<c:otherwise>
  <a rel=next rev=prev href="notifications?offset=${offset+topics}${addition_query}">вперед →</a>
</c:otherwise>
</c:choose>
</c:if>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
