<%@ tag import="ru.org.linux.comment.Comment" %>
<%@ tag import="ru.org.linux.comment.CommentNode" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.user.User" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag pageEncoding="UTF-8"%>
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
<%@ attribute name="comments" required="true" type="ru.org.linux.comment.CommentList" %>
<%@ attribute name="expired" required="true" type="java.lang.Boolean"%>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean"%>
<%@ attribute name="commentsAllowed" required="true" type="java.lang.Boolean" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%

  Template tmpl = Template.getTemplate(request);
  boolean moderatorMode = tmpl.isModeratorSession();
%>

<!-- ${comment.comment.messageId}  -->
<article class="msg" id="comment-${comment.comment.messageId}" <c:if test="${enableSchema}">itemprop="comment" itemscope itemtype="http://schema.org/UserComments"</c:if>>
  <div class=title>
<c:choose>
<c:when test="${not showMenu}">[#]</c:when>
<c:otherwise>
<%
  Comment reply = null;
  int replyPage = 0;
  String topicPage = null;
  String replyTitle = null;
  Boolean showLastMod = false;
  User replyAuthor = null;

  if (comment.getComment().getReplyTo() != 0) {
    CommentNode replyNode = comments.getNode(comment.getComment().getReplyTo());
    if (replyNode != null) {
        reply = replyNode.getComment();
        replyPage = comments.getCommentPage(reply, tmpl);
        topicPage = topic.getLinkPage(replyPage);

        replyTitle = reply.getTitle();
        if (replyTitle.trim().isEmpty()) {
          replyTitle = "комментарий";
        }
        showLastMod = (!expired && replyPage==topic.getPageCount(tmpl.getProf().getMessages())-1);
        replyAuthor = comment.getReplyAuthor();
    } else {
//        logger.warning("Weak reply #" + comment.getReplyTo() + " on comment=" + comment.getMessageId() + " msgid=" + comment.getTopic());
    }
  }

  Boolean deletable = moderatorMode ||
    (!topic.isExpired() && comment.getAuthor().getNick().equals(tmpl.getNick()));

  Boolean editable = moderatorMode && tmpl.getConfig().isModeratorAllowedToEditComments();
  if (!editable && comment.getAuthor().getNick().equals(tmpl.getNick())) {
    Integer minutesToEdit = tmpl.getConfig().getCommentExpireMinutesForEdit();

    boolean isbyMinutesEnable;
    if (minutesToEdit != null && !minutesToEdit.equals(0)) {
      long commentTimestamp = comment.getComment().getPostdate().getTime();
      long deltaTimestamp = minutesToEdit * 60 * 1000;
      long nowTimestamp = new java.util.Date().getTime();

      isbyMinutesEnable = commentTimestamp + deltaTimestamp > nowTimestamp;
    } else {
      isbyMinutesEnable = true;
    }

    boolean isbyAnswersEnable = true;
    if (!tmpl.getConfig().isCommentEditingAllowedIfAnswersExists()
      && comment.isHaveAnswers()) {
      isbyAnswersEnable = false;
    }

    Integer scoreToEdit = tmpl.getConfig().getCommentScoreValueForEditing();
    boolean isByScoreEnable = true;
    if (scoreToEdit != null && scoreToEdit > tmpl.getCurrentUser().getScore()) {
      isByScoreEnable = false;
    }

    editable = isbyMinutesEnable & isbyAnswersEnable & isByScoreEnable;
  }
%>
  <c:set var="reply" value="<%= reply %>"/>
  <c:set var="replyPage" value="<%= replyPage %>"/>
  <c:set var="topicPage" value="<%= topicPage %>"/>
  <c:set var="title" value="<%= replyTitle %>"/>
  <c:set var="showLastMod" value="<%= showLastMod %>"/>
  <c:set var="replyAuthor" value="<%= replyAuthor %>"/>
  <c:set var="deletable" value="<%= deletable %>"/>
  <c:set var="editable" value="<%= editable %>"/>

  <c:choose>
    <c:when test="${not comment.comment.deleted}">
      <c:url var="self_link" value="${topic.link}">
        <c:param name="cid" value="${comment.comment.messageId}"/>
      </c:url>
      [<a href="${self_link}">#</a>]
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${comment.comment.deleteInfo == null}">
          <strong>Сообщение удалено</strong>
        </c:when>
        <c:otherwise>
          <strong>Сообщение удалено ${comment.comment.deleteInfo.nick}
          по причине: <c:out value="${comment.comment.deleteInfo.reason}" escapeXml="true"/></strong>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>

  <c:if test="${reply != null}">
    <c:url var="reply_url" value="${topicPage}">
      <c:if test="${showLastMod}">
        <c:param name="lastmod" value="${comments.lastModified}" />
      </c:if>
    </c:url>
    Ответ на:
    <a href="${reply_url}#comment-${comment.comment.replyTo}" onclick="highlightMessage('${reply.messageId}')" ><l:title>${title}</l:title></a>
    от ${replyAuthor.nick} <lor:date date="${reply.postdate}"/>
  </c:if>
</c:otherwise>
</c:choose>

  &nbsp;</div>

  <c:if test="${template.prof.showPhotos}">
    <l:userpic author="${comment.author}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}" <c:if test="${enableSchema}">itemprop="commentText"</c:if>>
    <c:if test="${fn:length(comment.comment.title)>0}">
      <h2><l:title>${comment.comment.title}</l:title></h2>
    </c:if>

    ${comment.processedMessage}

    <div class=sign>
      <lor:sign postdate="${comment.comment.postdate}" user="${comment.author}" shortMode="false"/>

      <c:if test="${template.moderatorSession}">
        (<a href="sameip.jsp?msgid=${comment.comment.id}">${comment.comment.postIP}</a>)
      </c:if>

      <c:if test="${comment.comment.editCount != 0}">
        <span class="sign_more">
        <br>
        Последнее исправление: ${comment.comment.editNick} <lor:date date="${comment.comment.editDate}"/>
        (всего <a href="${topic.link}/${comment.comment.id}/history">исправлений: ${comment.comment.editCount}</a>)
        </span>
      </c:if>

      <c:if test="${template.moderatorSession}">
        <c:if test="${comment.comment.userAgent!=null}">
          <br>
          <span class="sign_more"><c:out value="${comment.comment.userAgent}" escapeXml="true"/></span>
        </c:if>
      </c:if>
    </div>

  <c:if test="${not comment.comment.deleted and showMenu}">
    <div class=reply>

      <c:if test="${deletable or editable or commentsAllowed}">
      <ul>
      <c:if test="${commentsAllowed}">
        <li><a  <c:if test="${enableSchema}">itemprop="replyToUrl"</c:if> href="add_comment.jsp?topic=${topic.id}&amp;replyto=${comment.comment.id}">Ответить на это сообщение</a></li>
      </c:if>

      <c:if test="${editable}">
        <c:url var="edit_url" value="/edit_comment">
          <c:param name="original" value="${comment.comment.messageId}"/>
          <c:param name="topic" value="${topic.id}"/>
        </c:url>
        <li><a href="${edit_url}">Править</a></li>
      </c:if>

      <c:if test="${deletable}">
        <c:url var="delete_url" value="/delete_comment.jsp">
          <c:param name="msgid" value="${comment.comment.messageId}"/>
        </c:url>
        <li><a href="${delete_url}">Удалить</a></li>
      </c:if>
      </ul>
      </c:if>
     </div>
  </c:if>

  </div><div style="clear: both"></div>
</article>
