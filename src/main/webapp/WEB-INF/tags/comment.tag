<%@ tag import="ru.org.linux.comment.Comment" %>
<%@ tag import="ru.org.linux.comment.CommentNode" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.user.User" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag import="ru.org.linux.marks.MessageMark" %>
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%
  Template tmpl = Template.getTemplate(request);

  boolean moderatorMode = tmpl.isModeratorSession();

  out.append("\n\n<!-- ");
  out.append(Integer.toString(comment.getComment().getMessageId()));
  out.append(" -->\n");
%>
<article class="msg" id="comment-${comment.comment.messageId}">
  <div class=title>
<c:if test="${showMenu}">
<%
    DateFormat dateFormat = tmpl.dateFormat;

    if (!comment.getComment().isDeleted()) {
      out.append("[<a href='").append(topic.getLink()).append("?cid=").append(Integer.toString(comment.getComment().getMessageId())).append("'>#</a>]");
    }

    if (comment.getComment().isDeleted()) {
      if (comment.getComment().getDeleteInfo() ==null) {
        out.append("<strong>Сообщение удалено</strong>");
      } else {
        out.append("<strong>Сообщение удалено ").append(comment.getComment().getDeleteInfo().getNick()).append(" по причине '").append(StringUtil.escapeHtml(comment.getComment().getDeleteInfo().getReason())).append("'</strong>");
      }
    }
%>
<c:if test="${comment.comment.replyTo!=0}">
    <%
      CommentNode replyNode = comments.getNode(comment.getComment().getReplyTo());
      if (replyNode != null) {
        Comment reply = replyNode.getComment();

        out.append(" Ответ на: <a href=\"");

        int replyPage = comments.getCommentPage(reply, tmpl);

        String urladd = "";
        if (!expired && replyPage==topic.getPageCount(tmpl.getProf().getMessages())-1) {
          urladd = "?lastmod=" + comments.getLastModified();
        }

        out.append(topic.getLinkPage(replyPage)).append(urladd).append("#comment-").append(Integer.toString(comment.getComment().getReplyTo()));

        out.append("\" onclick=\"highlightMessage(").append(Integer.toString(reply.getMessageId())).append(");\">");

        User replyAuthor = comment.getReplyAuthor();

        String title = reply.getTitle();

        if (title.trim().isEmpty()) {
          title = "комментарий";
        }

        out.append(title).append("</a> от ").append(replyAuthor.getNick()).append(' ').append(dateFormat.format(reply.getPostdate()));
      } else {
//        logger.warning("Weak reply #" + comment.getReplyTo() + " on comment=" + comment.getMessageId() + " msgid=" + comment.getTopic());
      }
    %>
</c:if>
  </c:if>

  <c:if test="${not showMenu}">
    [#]
  </c:if>
    &nbsp;</div>

  <c:set var="showPhotos" value="<%= tmpl.getProf().isShowPhotos() %>"/>

  <c:if test="${showPhotos}">
    <lor:userpic author="${comment.author}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}">
    <c:if test="${fn:length(comment.comment.title)>0}">
      <h2>${comment.comment.title}</h2>
    </c:if>

    ${comment.processedMessage}

    <div class=sign>
      <lor:sign postdate="${comment.comment.postdate}" user="${comment.author}" shortMode="false"/>
      <c:if test="${template.moderatorSession}">
        (<a href="sameip.jsp?msgid=${comment.comment.id}">${comment.comment.postIP}</a>)
        <c:if test="${comment.comment.userAgent!=null}">
          <br>
          <span class="sign_more"><c:out value="${comment.comment.userAgent}" escapeXml="true"/></span>
        </c:if>
      </c:if>
    </div>
  <c:if test="${not comment.comment.deleted and showMenu}">
    <ul style="float: right; margin:0" class=nav>
      <c:forEach items="${comment.marks}" var="mark">
        <li>
          <c:set var="current">
            <lor:contains collection="${comment.currentUserMarks}" value="${mark.key}"/>
          </c:set>
          <a <c:if test="${current}">class="current"</c:if> onclick="mark_message(event, ${comment.comment.id}, ${mark.key.id})" href="#">${mark.key.title}</a><br>
          <span style="font-size: x-small">&nbsp;<c:if test="${mark.value>0}">${mark.value}</c:if>&nbsp;</span>
        </li>
      </c:forEach>
    </ul>

    <div class=reply>
      <c:set var="deletable" value="<%= moderatorMode || (!topic.isExpired() && comment.getAuthor().getNick().equals(tmpl.getNick())) %>"/>

      <c:if test="${deletable or commentsAllowed}">
      <ul>
      <c:if test="${commentsAllowed}">
        <li><a href="add_comment.jsp?topic=${topic.id}&amp;replyto=${comment.comment.id}">Ответить на это сообщение</a></li>
      </c:if>

      <c:if test="${deletable}">
          <li><%
      out.append("<a href=\"delete_comment.jsp?msgid=").append(Integer.toString(comment.getComment().getMessageId())).append("\">Удалить</a>");
          %></li>

      </c:if>
      </ul>
        
      </c:if>
     </div>
  </c:if>

  </div><div style="clear: both"></div>
</article>