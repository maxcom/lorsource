<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
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
<%@ attribute name="comment" required="true" type="ru.org.linux.comment.PreparedComment" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean"%>
<%@ attribute name="commentsAllowed" required="true" type="java.lang.Boolean" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:if test="${not template.sessionAuthorized}">

<article class="msg" id="comment-${comment.id}" <c:if test="${enableSchema}">itemprop="comment" itemscope itemtype="http://schema.org/UserComments"</c:if>>
  <div class=title>
    <c:if test="${comment.deleted}">
      <c:choose>
        <c:when test="${comment.deleteInfo == null}">
          <strong>Сообщение удалено</strong>
        </c:when>
        <c:otherwise>
          <strong>Сообщение удалено ${comment.deleteInfo.nick}
            по причине: <c:out value="${comment.deleteInfo.reason}" escapeXml="true"/></strong>
        </c:otherwise>
      </c:choose>
    </c:if>

    <c:if test="${comment.reply != null}">
      <c:url var="reply_url" value="${topic.link}">
        <c:param name="cid" value="${comment.reply.id}"/>
      </c:url>
      Ответ на:
      <a href="${reply_url}"
         <c:if test="${comment.reply.samePage}">data-samepage</c:if>
       ><l:title>${comment.reply.title}</l:title></a>
      от ${comment.reply.author}<c:out value=" "/><lor:date date="${comment.reply.postdate}"/>
    </c:if>
  </div>

  <c:if test="${comment.userpic != null}">
    <l:userpic userpic="${comment.userpic}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}" <c:if test="${enableSchema}">itemprop="commentText"</c:if>>
    <c:if test="${fn:length(comment.title)>0}">
      <h2><l:title>${comment.title}</l:title></h2>
    </c:if>

    ${comment.processedMessage}

    <div class=sign>
      <l:sign
              postdate="${comment.postdate}"
              user="${comment.author}"
              shortMode="false"
              timeprop="commentTime"
              author="false"
      />

      <c:if test="${comment.remark != null}">
        <c:out value=" "/>
        <span class="user-remark"><c:out value="${comment.remark}" escapeXml="true"/></span>
      </c:if>

      <c:if test="${comment.postIP!=null}">
        (<a href="sameip.jsp?msgid=${comment.id}">${comment.postIP}</a>)
      </c:if>

      <c:if test="${comment.editSummary != null}">
        <span class="sign_more">
        <br>
        Последнее исправление: ${comment.editSummary.editNick}<c:out value=" "/><lor:date date="${comment.editSummary.editDate}"/>
        (всего <a href="${topic.link}/${comment.id}/history">исправлений: ${comment.editSummary.editCount}</a>)
        </span>
      </c:if>

      <c:if test="${comment.userAgent!=null}">
        <br>
        <span class="sign_more"><c:out value="${comment.userAgent}" escapeXml="true"/></span>
      </c:if>
    </div>

  <c:if test="${not comment.deleted and showMenu}">
    <div class="reply">

    <ul>
      <c:if test="${commentsAllowed}">
        <li><a  <c:if test="${enableSchema}">itemprop="replyToUrl"</c:if> href="add_comment.jsp?topic=${topic.id}&amp;replyto=${comment.id}">Ответить на это сообщение</a></li>
      </c:if>

      <c:if test="${comment.editable}">
        <c:url var="edit_url" value="/edit_comment">
          <c:param name="original" value="${comment.id}"/>
          <c:param name="topic" value="${topic.id}"/>
        </c:url>
        <li><a href="${edit_url}">Править</a></li>
      </c:if>

      <c:if test="${comment.deletable}">
        <c:url var="delete_url" value="/delete_comment.jsp">
          <c:param name="msgid" value="${comment.id}"/>
        </c:url>
        <li><a href="${delete_url}">Удалить</a></li>
      </c:if>

      <c:url var="self_link" value="${topic.link}">
        <c:param name="cid" value="${comment.id}"/>
      </c:url>
      <li><a href="${self_link}">Ссылка</a></li>
    </ul>
     </div>
  </c:if>

  </div><div style="clear: both"></div>
</article>
</c:if>

<c:if test="${template.sessionAuthorized}">
<l:comment
        comment="${comment}"
        enableSchema="${enableSchema}"
        topic="${topic}"
        showMenu="${showMenu}"
        commentsAllowed="${commentsAllowed}"
/>
</c:if>