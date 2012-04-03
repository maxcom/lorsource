<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.user.PreparedUserEvent>"--%>
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
<div class=nav>
<tr>
    <div id="navPath">
    ${title}
    </div>
    <div class="nav-buttons">
        <ul>
        <c:forEach var="f" items="${filter}">
        <c:url var="fUrl" value="/notifications">
            <c:param name="filter">${f.value}</c:param>
        </c:url>
        <c:choose>
            <c:when test="${f.value == notifications.filter}">
                <li><a href="${fUrl}" class="current">${f.label}</a></li>
            </c:when>
            <c:otherwise>
                <li><a href="${fUrl}">${f.label}</a></li>
            </c:otherwise>
        </c:choose>
        </c:forEach>
        </ul>
        <ul>
        <li><a href="show-replies.jsp?output=rss&amp;nick=${nick}">RSS</a></li>
        </ul>
    </div>
</div>

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
  <a rel=prev href="show-replies.jsp?nick=${nick}&amp;offset=${offset-topics}${addition_query}">← назад</a>
</c:when>
<c:otherwise>
  <a rel=prev href="notifications?offset=${offset-topics}${addition_query}">← назад</a>
</c:otherwise>
</c:choose>
</c:if>
</div>

<div style="float: right">
<c:if test="${hasMore}">
<c:choose>
<c:when test="${not isMyNotifications}">
  <a rel=next href="show-replies.jsp?nick=${nick}&amp;offset=${offset+topics}${addition_query}">вперед →</a>
</c:when>
<c:otherwise>
  <a rel=next href="notifications?offset=${offset+topics}${addition_query}">вперед →</a>
</c:otherwise>
</c:choose>
</c:if>
</div>

<p style="clear: both;"> </p>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th></th><th>Заголовок</th><th>Комментарий</th></tr>
<tbody>

<c:forEach var="topic" items="${topicsList}">
<tr>
  <td align="center">
    <c:choose>
      <c:when test="${topic.event.type == 'DELETED'}">
        <img src="/img/del.png" border="0" alt="[X]" title="Сообщение удалено" width="15" height="15">
      </c:when>
      <c:when test="${topic.event.type == 'ANSWERS'}">
        <img src="/img/mail_reply.png" border="0" title="Ответ" alt="[R]" width="16" height="16">
      </c:when>
      <c:when test="${topic.event.type == 'REFERENCE'}">
        <img src="/img/tuxlor.png" border="0" title="Упоминание" alt="[U]" width="7" height="16">
      </c:when>
      <c:when test="${topic.event.type == 'TAG'}">
        <img src="/img/tag-icon.png" border="0" alt="[T]" title="Фаворитный тег" width="15" height="15">
      </c:when>
    </c:choose>
  </td>
  <td>
    <c:if test="${topic.event.type != 'DELETED'}">
      <c:if test="${topic.event.cid>0}">
        <a href="jump-message.jsp?msgid=${topic.event.msgid}&amp;cid=${topic.event.cid}">${topic.event.subj}</a>
      </c:if>
      <c:if test="${topic.event.cid==0}">
        <a href="jump-message.jsp?msgid=${topic.event.msgid}">${topic.event.subj}</a>
      </c:if>
      (<a class="secondary" href="${topic.event.groupUrl}">${topic.event.groupTitle}</a>)
    </c:if>

    <c:if test="${topic.event.type == 'DELETED'}">
      <a href="view-message.jsp?msgid=${topic.event.msgid}">${topic.event.subj}</a>
      (<a class="secondary" href="${topic.event.groupUrl}">${topic.event.groupTitle}</a>)
      <br>
      <c:out value="${topic.event.eventMessage}" escapeXml="true"/>
    </c:if>

    <c:if test="${topic.event.unread}">&bull;</c:if>
  </td>
  <td>
    <lor:dateinterval date="${topic.event.eventDate}"/>

    <c:if test="${topic.event.cid != 0}">
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
