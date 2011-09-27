<%@ tag import="ru.org.linux.site.NewsViewer" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.site.User" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag import="java.sql.Timestamp" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.site.PreparedMessage" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.site.MessageMenu" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="highlight" type="java.util.Set" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%
  Template tmpl = Template.getTemplate(request);

  User author = preparedMessage.getAuthor();
  User currentUser = tmpl.getCurrentUser();

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
        <c:if test="${preparedMessage.section.premoderated and not message.commited}">
          [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
        </c:if>
        <c:if test="${message.votePoll}">
          [<a href="edit-vote.jsp?msgid=${message.id}">Править опрос</a>]
        </c:if>
        
        [<a href="setpostscore.jsp?msgid=${message.id}">Параметры</a>]
        [<a href="mt.jsp?msgid=${message.id}">Перенести</a>]

        <c:if test="${preparedMessage.section.premoderated}">
          [<a href="mtn.jsp?msgid=${message.id}">Группа</a>]
        </c:if>

        <c:if test="${message.commited and not message.expired}">
          [<a href="uncommit.jsp?msgid=${message.id}">Отменить подтверждение</a>]
        </c:if>
      </c:if>
    </c:if>
    <c:if test="${message.deleted}">
        <c:if test="${preparedMessage.deleteInfo == null}">
            <strong>Сообщение удалено</strong>
        </c:if>
        <c:if test="${preparedMessage.deleteInfo != null}">
            <strong>Сообщение удалено ${preparedMessage.deleteUser.nick}
                по причине '${preparedMessage.deleteInfo.reason}'</strong>
        </c:if>

        <c:if test="${template.moderatorSession and not message.expired}">
            [<a href="/undelete.jsp?msgid=${message.id}">восстановить</a>]
        </c:if>
    </c:if>
  &nbsp;</div>
</c:if>

<c:set var="showPhotos" value="<%= tmpl.getProf().isShowPhotos() %>"/>
  <c:if test="${showPhotos}">
    <lor:userpic author="${preparedMessage.author}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}">
  <h1>
    ${message.title}
  </h1>

    ${preparedMessage.processedMessage}

    <c:if test="${message.votePoll}">
      <lor:poll poll="${preparedMessage.poll}" highlight="${highlight}"/>

      <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${message.id}">Проголосовать</a></p>
    </c:if>
    <%
  if (message.getUrl() != null && message.isHaveLink() && message.getUrl().length()>0) {
    out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(message.getUrl())).append("\">").append(message.getLinktext()).append("</a>");
  }

  if (message.getUrl() != null && preparedMessage.getSection().isImagepost()) {
    out.append(NewsViewer.showMediumImage(tmpl.getObjectConfig().getHTMLPathPrefix(), message, true));
  }
%>
<c:if test="${preparedMessage.section.premoderated and not empty preparedMessage.tags}">
  <lor:tags list="${preparedMessage.tags}"/>
</c:if>
<div class=sign>
  <lor:sign postdate="${message.postdate}" user="${preparedMessage.author}" shortMode="false"/>
  <c:if test="${template.moderatorSession}">
    (<a href="sameip.jsp?msgid=${message.id}">${message.postIP}</a>)
  </c:if>

  <span class="sign_more">
  <c:if test="${template.moderatorSession}">
    <c:if test="${preparedMessage.userAgent!=null}">
      <br>
      <c:out value="${preparedMessage.userAgent}" escapeXml="true"/>
    </c:if>
  </c:if>
  <%
  if (preparedMessage.getSection().isPremoderated() && message.getCommitby() != 0) {
    User commiter = preparedMessage.getCommiter();

    if (commiter.getId()!=message.getUid()) {
      Timestamp commitDate = message.getCommitDate();
      DateFormat dateFormat = tmpl.dateFormat;
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
  if (preparedMessage.getEditCount()>0) {
  %>
  <br>
  Последнее исправление: <%= preparedMessage.getLastEditor().getNick() %> <lor:date date="<%= preparedMessage.getLastEditInfo().getEditdate() %>"/>
    (всего <a href="${message.link}/history">исправлений: ${preparedMessage.editCount}</a>)
  <%
  }
%>
    </c:if>
   </span>
</div>
    <c:if test="${!message.deleted && showMenu}">
      <div class=reply>
        <c:if test="${not message.expired}">
          <% if (message.isCommentsAllowed(currentUser)) { %>
          [<a href="comment-message.jsp?topic=${message.id}">Ответить на это сообщение</a>]
          <% } %>
        </c:if>
        <c:if test="${messageMenu.editable}">
            [<a href="edit.jsp?msgid=${message.id}">Править</a>]
        </c:if>

<%
    if (tmpl.isModeratorSession() || author.equals(tmpl.getCurrentUser())) {
      out.append("[<a href=\"delete.jsp?msgid=");
      out.print(msgid);
      out.append("\">Удалить</a>] ");
    }
%>
        <c:if test="${messageMenu.resolvable}">
            <c:if test="${message.resolved}">
                [<a href="resolve.jsp?msgid=${message.id}&amp;resolve=no">Отметить как нерешенную</a>]
            </c:if>
            <c:if test="${not message.resolved}">
                [<a href="resolve.jsp?msgid=${message.id}&amp;resolve=yes">Отметить как решенную</a>]
            </c:if>
        </c:if>
<%
    if (tmpl.isSessionAuthorized()) {
      int memId = messageMenu.getMemoriesId();

      if (memId!=0) {
%>
        <form id="memories_form" action="/memories.jsp" method="POST" style="display: inline">
          <input type="hidden" name="id" value="<%= memId %>">
          <input type="hidden" name="remove" value="remove">
          [<a onclick="$('#memories_form').submit(); return false;" href="#">Удалить из избранного</a>]
        </form>
<%
      } else {
%>
        <form id="memories_form" action="/memories.jsp" method="POST" style="display: inline">
          <input type="hidden" name="msgid" value="${message.id}">
          <input type="hidden" name="add" value="add">
          [<a onclick="$('#memories_form').submit(); return false;" href="#">Добавить в избранное</a>]
        </form>
        <%
      }
    }
%>
        <c:if test="${template.sessionAuthorized}">
          <br>${message.postScoreInfo}
        </c:if>
        </div>
      </c:if>
</div>
  <div style="clear: both"></div>
</div>
