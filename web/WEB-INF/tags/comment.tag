<%@ tag import="java.text.DateFormat" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag pageEncoding="UTF-8"%>
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

<%@ attribute name="comment" required="true" type="ru.org.linux.site.Comment" %>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="comments" required="true" type="ru.org.linux.site.CommentList" %>
<%@ attribute name="expired" required="true" type="java.lang.Boolean"%>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean"%>
<%@ attribute name="topic" required="true" type="ru.org.linux.site.Message" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%
  Template tmpl = Template.getTemplate(request);

  boolean moderatorMode = tmpl.isModeratorSession();

  out.append("\n\n<!-- ");
  out.append(Integer.toString(comment.getMessageId()));
  out.append(" -->\n");

  User author = User.getUserCached(db, comment.getUserid());
%>
<div class="msg" id="comment-${comment.messageId}">
<c:if test="${showMenu}">
  <div class=title>
<%
    DateFormat dateFormat = DateFormats.createDefault();

    if (!comment.isDeleted()) {
      out.append("[<a href=\"/jump-message.jsp?msgid=").append(Integer.toString(comment.getTopic())).append("&amp;cid=").append(Integer.toString(comment.getMessageId())).append("\">#</a>]");
    }

    if (comment.isDeleted()) {
      if (comment.getDeleteInfo() ==null) {
        out.append("<strong>Сообщение удалено</strong>");
      } else {
        out.append("<strong>Сообщение удалено ").append(comment.getDeleteInfo().getNick()).append(" по причине '").append(HTMLFormatter.htmlSpecialChars(comment.getDeleteInfo().getReason())).append("'</strong>");
      }
    }
%>
<c:if test="${comment.replyTo!=0}">
    <%
      CommentNode replyNode = comments.getNode(comment.getReplyTo());
      if (replyNode != null) {
        Comment reply = replyNode.getComment();

        out.append(" Ответ на: <a href=\"");

        String urladd = "";
        if (!expired) {
          urladd = "?lastmod=" + comments.getLastModified();
        }

        int replyPage = comments.getCommentPage(reply, tmpl);
        out.append(topic.getLinkPage(replyPage)).append(urladd).append("#comment-").append(Integer.toString(comment.getReplyTo()));

        out.append("\" onclick=\"highlightMessage(").append(Integer.toString(reply.getMessageId())).append(");\">");

        User replyAuthor = User.getUserCached(db, reply.getUserid());

        String title = reply.getTitle();

        if (title.trim().length() == 0) {
          title = "комментарий";
        }

        out.append(title).append("</a> от ").append(replyAuthor.getNick()).append(' ').append(dateFormat.format(reply.getPostdate()));
      } else {
//        logger.warning("Weak reply #" + comment.getReplyTo() + " on comment=" + comment.getMessageId() + " msgid=" + comment.getTopic());
      }
    %>
</c:if>    &nbsp;</div>
  </c:if>
  <c:set var="showPhotos" value="<%= tmpl.getProf().getBoolean(&quot;photos&quot;)%>"/>

  <c:if test="${showPhotos}">
    <lor:userpic author="<%= author %>"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}">
    <c:if test="${fn:length(comment.title)>0}">
      <h2>${comment.title}</h2>
    </c:if>
  <%
  out.append(comment.getProcessedMessage(db));
  %>
    <div class=sign>
    <%
  out.append(author.getSignature(moderatorMode, comment.getPostdate(), tmpl.isMobile()));
%>
      <c:if test="${template.moderatorSession}">
        <c:if test="${comment.userAgent!=null}">
          (<a href="sameip.jsp?msgid=${comment.id}" title="${fn:escapeXml(comment.userAgent)}">${comment.postIP}</a>)
          <br>
          <c:if test="${not template.mobile}">
            <c:out value="${comment.userAgent}" escapeXml="true"/>
          </c:if>
        </c:if>
        <c:if test="${comment.userAgent==null}">
          (<a href="sameip.jsp?msgid=${comment.id}">${comment.postIP}</a>)
        </c:if>
      </c:if>
    </div>
    <%
  if (!comment.isDeleted() && showMenu) {
    out.append("<div class=reply>");
    if (!expired) {
      out.append("[<a href=\"add_comment.jsp?topic=").append(Integer.toString(comment.getTopic())).append("&amp;replyto=").append(Integer.toString(comment.getMessageId())).append("\">Ответить на это сообщение</a>] ");
    }

    if ((moderatorMode || author.getNick().equals(Template.getNick(session)))) {
      out.append("[<a href=\"delete_comment.jsp?msgid=").append(Integer.toString(comment.getMessageId())).append("\">Удалить</a>]");
    }

    out.append("</div>");
  }
%>
  </div><div style="clear: both"></div>
</div>