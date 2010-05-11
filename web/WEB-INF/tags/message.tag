<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="java.util.List" %>
<%@ tag import="java.sql.Timestamp" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="user" type="java.lang.String"%>
<%@ attribute name="highlight" type="java.lang.Integer" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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

<%
  Template tmpl = Template.getTemplate(request);

  User author = User.getUserCached(db, message.getUid());
  User currentUser = User.getCurrentUser(db, session);

  int msgid = message.getMessageId();
%>

  <!-- ${message.id}  -->
<div class=msg id="topic-${message.id}">
<c:if test="${showMenu}">
  <div class=title>
    <c:if test="${message.resolved}"><img src="/img/solved.png" alt="решено" title="решено"/></c:if>
    <c:if test="${not message.deleted}">
      [<a href="${message.link}">#</a>]
      <c:if test="${template.moderatorSession}">
        <c:if test="${message.section.premoderated and not message.commited}">
          [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
        </c:if>
        <c:if test="${message.votePoll}">
          [<a href="edit-vote.jsp?msgid=${message.id}">Править опрос</a>]
        </c:if>
        
        [<a href="setpostscore.jsp?msgid=${message.id}">Установить параметры</a>]
        [<a href="mt.jsp?msgid=${message.id}">Перенести</a>]

        <c:if test="${message.section.premoderated}">
          [<a href="mtn.jsp?msgid=${message.id}">Группа</a>]
        </c:if>
      </c:if><%

    if (currentUser!=null && message.isEditable(db, currentUser)) {
      out.append("[<a href=\"edit.jsp?msgid=");
      out.print(msgid);
      out.append("\">Править</a>]");
    }
    %></c:if><%
    if (message.isDeleted()) {
      DeleteInfo deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);

      if (deleteInfo==null) {
        out.append("<strong>Сообщение удалено</strong>");
      } else {
        User deleteUser = User.getUserCached(db, deleteInfo.getUserid());

        out.append("<strong>Сообщение удалено ").append(deleteUser.getNick()).append(" по причине '").append(deleteInfo.getReason()).append("'</strong>");
      }
    }

%>
  &nbsp;</div>
</c:if>

<c:set var="showPhotos" value="<%= tmpl.getProf().getBoolean(&quot;photos&quot;)%>"/>
  <c:if test="${showPhotos}">
    <lor:userpic author="<%= author %>"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}">
  <h1>
    ${message.title}
  </h1>

    <%= message.getProcessedMessage(db, true) %>

    <c:if test="${message.votePoll}">
        <%
          Poll poll = Poll.getPollByTopic(db, msgid);
          out.append(poll.renderPoll(db, tmpl.getConfig(), tmpl.getProf(), highlight != null ? highlight : 0));
        %>

      <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${msgid}">Проголосовать</a></p>
    </c:if>
    <%
  if (message.getUrl() != null && message.isHaveLink() && message.getUrl().length()>0) {
    out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(message.getUrl())).append("\">").append(message.getLinktext()).append("</a>.");
  }

  if (message.getUrl() != null && message.getSection().isImagepost()) {
    NewsViewer.showMediumImage(tmpl.getObjectConfig().getHTMLPathPrefix(), out, message.getUrl(), message.getTitle(), message.getLinktext(), true);
  }

  if (message.getSection().isPremoderated()) {
    String tagLinks = Tags.getTagLinks(message.getTags());

    if (tagLinks.length() > 0) {
      out.append("<p class=tags>Метки: <span class=tag>");
      out.append(tagLinks);
      out.append("</span></p>");
    }
  }
%>

<div class=sign>
<%
  out.append(author.getSignature(tmpl.isModeratorSession(), message.getPostdate(), tmpl.isMobile()));
%>
  <c:if test="${template.moderatorSession}">
    <c:if test="${message.userAgent!=null}">
     (<a href="sameip.jsp?msgid=${message.id}" title="${fn:escapeXml(message.userAgent)}">${message.postIP}</a>)
      <br>
      <c:out value="${message.userAgent}" escapeXml="true"/>
    </c:if>
    <c:if test="${message.userAgent==null}">
     (<a href="sameip.jsp?msgid=${message.id}">${message.postIP}</a>)
    </c:if>
  </c:if>
  <%
  if (message.getCommitby() != 0) {
    User commiter = User.getUserCached(db, message.getCommitby());

    if (commiter.getId()!=message.getUid()) {
      Timestamp commitDate = message.getCommitDate();
      DateFormat dateFormat = DateFormats.createDefault();
      out.append("<br>");

      out.append("Проверено: <a href=\"/people/").append(URLEncoder.encode(commiter.getNick())).append("/profile\">").append(commiter.getNick()).append("</a>");

      if (commitDate !=null && !commitDate.equals(message.getPostdate())) {
        out.append(" (").append(dateFormat.format(commitDate)).append(")");
      }
    }
  }

%>
  <c:if test="${template.sessionAuthorized}">
  <%
  List<EditInfoDTO> editInfo = message.loadEditInfo(db);
  if (editInfo!=null) {
    for (EditInfoDTO info : editInfo) {
      User editor = User.getUserCached(db, info.getEditor());
%>
  <br>
  <i>Исправлено: <%= editor.getNick() %> <lor:date date="<%= info.getEditdate() %>"/></i>
  <%
    }
  }
%>
    </c:if>
</div>
    <c:if test="${!message.deleted && showMenu}">
      <div class=reply>
<%
    if (!message.isExpired()) {
      out.append("[<a href=\"comment-message.jsp?msgid=");
      out.print(msgid);
      out.append("\">Ответить на это сообщение</a>] ");
    }

    if (currentUser!=null && message.isEditable(db, currentUser)) {
      out.append("[<a href=\"edit.jsp?msgid=");
      out.print(msgid);
      out.append("\">Править</a>] ");
    }
  
    if (tmpl.isModeratorSession() || author.getNick().equals(user)) {
      out.append("[<a href=\"delete.jsp?msgid=");
      out.print(msgid);
      out.append("\">Удалить</a>]");
    }

    if ((tmpl.isModeratorSession() || author.getNick().equals(user)) &&
            new Group(db, message.getGroupId()).isResolvable()){
      out.append("[<a href=\"resolve.jsp?msgid=");
      out.print(msgid);
      if (message.isResolved()){
        out.append("&amp;resolve=no\">Отметить как не решенную</a>]");
      }else{
        out.append("&amp;resolve=yes\">Отметить как решенную</a>]");
      }
    }
%>
        </div>
      </c:if>
</div>
  <div style="clear: both"></div>
</div>
