<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.user.PreparedUserEvent>"--%>
<%--@elvariable id="firstPage" type="Boolean"--%>
<%--@elvariable id="nick" type="String"--%>
<%--@elvariable id="hasMore" type="String"--%>
<%--@elvariable id="unreadCount" type="Integer"--%>
<%--@elvariable id="enableReset" type="Boolean"--%>
<%--@elvariable id="isMyNotifications" type="java.lang.Boolean"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<c:set var="title">
  <c:choose>
    <c:when test="${isMyNotifications}">
      Уведомления
    </c:when>
    <c:otherwise>
      Уведомления пользователя ${nick}
    </c:otherwise>
  </c:choose>
</c:set>
<title>${title}</title>
<link rel="alternate" title="RSS" href="show-replies.jsp?output=rss&amp;nick=${nick}" type="application/rss+xml">
<link rel="alternate" title="Atom" href="show-replies.jsp?output=atom&amp;nick=${nick}" type="application/atom+xml">
<script type="text/javascript">
  $script.ready('plugins', function() {
    $(document).ready(function() {
      $('#reset_form').ajaxSubmit({
        success: function() { $('#reset_form').hide(); },
        url: "/notifications-reset"
      });
    });
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${title}</h1>

<nav>
  <c:forEach var="f" items="${filterValues}">
    <c:url var="fUrl" value="/notifications">
      <c:param name="filter">${f.name}</c:param>
    </c:url>
    <c:choose>
      <c:when test="${f.name == filter}">
        <a href="${fUrl}" class="btn btn-selected">${f.label}</a>
      </c:when>
      <c:otherwise>
        <a href="${fUrl}" class="btn btn-default">${f.label}</a>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</nav>

<c:if test="${unreadCount > 0}">
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
        <lor:csrf/>
        <button type="submit">Сбросить</button>
      </form>
    </c:if>
  </div>
</c:if>

<div class=forum>
<table width="100%" class="message-table">
<tbody>

<c:forEach var="topic" items="${topicsList}">
<tr>
  <td align="center">
    <c:choose>
      <c:when test="${topic.event.type == 'DELETED'}">
        <img src="/img/del.png" alt="[X]" title="Сообщение удалено" width="15" height="15">
      </c:when>
      <c:when test="${topic.event.type == 'ANSWERS'}">
        <img src="/img/mail_reply.png" title="Ответ" alt="[R]" width="16" height="16">
      </c:when>
      <c:when test="${topic.event.type == 'REFERENCE'}">
        <img src="/img/tuxlor.png" title="Упоминание" alt="[U]" width="7" height="16">
      </c:when>
      <c:when test="${topic.event.type == 'TAG'}">
        <i class="icon-tag" title="Избранный тег"></i>
      </c:when>
    </c:choose>
  </td>
  <td>
    <c:if test="${topic.event.type != 'DELETED'}">
      <c:if test="${topic.event.cid>0}">
        <a href="jump-message.jsp?msgid=${topic.event.msgid}&amp;cid=${topic.event.cid}"><l:title>${topic.event.subj}</l:title></a>
      </c:if>
      <c:if test="${topic.event.cid==0}">
        <a href="jump-message.jsp?msgid=${topic.event.msgid}"><l:title>${topic.event.subj}</l:title></a>
      </c:if>
      (${topic.group.title})
    </c:if>

    <c:if test="${topic.event.type == 'DELETED'}">
      <a href="view-message.jsp?msgid=${topic.event.msgid}"><l:title>${topic.event.subj}</l:title></a>
      (${topic.group.title})
      <br>
      <c:out value="${topic.event.eventMessage}" escapeXml="true"/> (${topic.bonus})
    </c:if>

    <c:if test="${topic.event.unread}">&bull;</c:if>
  </td>
  <td>
    <lor:dateinterval date="${topic.event.eventDate}"/>

    <c:if test="${topic.event.cid != 0}">
       (<lor:user user="${topic.commentAuthor}"/>)
    </c:if>
  </td>
</tr>
</c:forEach>

</tbody>
</table>
</div>

<div class="container" style="margin-bottom: 1em">
  <div style="float: left">
    <c:if test="${not firstPage}">
      <c:choose>
        <c:when test="${not isMyNotifications}">
          <a rel=prev rev=next href="show-replies.jsp?nick=${nick}&amp;offset=${offset-topics}${addition_query}">←
            назад</a>
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
          <a rel=next rev=prev href="show-replies.jsp?nick=${nick}&amp;offset=${offset+topics}${addition_query}">вперед
            →</a>
        </c:when>
        <c:otherwise>
          <a rel=next rev=prev href="notifications?offset=${offset+topics}${addition_query}">вперед →</a>
        </c:otherwise>
      </c:choose>
    </c:if>
  </div>
</div>

<p>
  <i class="icon-rss"></i>
  <a href="show-replies.jsp?output=rss&amp;nick=${nick}">
    RSS подписка на новые уведомления
  </a>
</p>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
