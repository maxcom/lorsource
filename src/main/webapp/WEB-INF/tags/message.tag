<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.user.User" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag import="java.sql.Timestamp" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="message" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.topic.TopicMenu" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="favoriteTags" required="false" type="java.util.List" %>
<%@ attribute name="ignoreTags" required="false" type="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%
  Template tmpl = Template.getTemplate(request);
%>
  <!-- ${message.id}  -->
<article class=msg id="topic-${message.id}">
<c:if test="${showMenu}">
  <div class=title>
    <c:if test="${message.resolved}"><img src="/img/solved.png" alt="решено" title="решено"/></c:if>
    <c:if test="${not message.deleted}">
      [<a href="${message.link}">#</a>]
      <c:if test="${template.moderatorSession}">
        <c:if test="${preparedMessage.section.premoderated and not message.commited}">
          [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
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

<c:set var="showPhotos" value="${template.prof.showPhotos}"/>
  <c:if test="${showPhotos}">
    <lor:userpic author="${preparedMessage.author}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="msg_body ${msgBodyStyle}">
  <h1>
    ${message.title}
  </h1>

    ${preparedMessage.processedMessage}

    <c:if test="${preparedMessage.section.votePoll}">
      <c:choose>
          <c:when test="${not message.commited}">
              <lor:poll-form poll="${preparedMessage.poll.poll}" enabled="false"/>
          </c:when>
          <c:otherwise>
              <lor:poll poll="${preparedMessage.poll}"/>

            <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${message.id}">Проголосовать</a></p>
          </c:otherwise>
      </c:choose>
    </c:if>

    <%
  if (message.getUrl() != null && message.isHaveLink() && !message.getUrl().isEmpty()) {
    out.append("<p>&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(message.getUrl())).append("\">").append(message.getLinktext()).append("</a>");
  }
%>
    <c:if test="${preparedMessage.section.imagepost}">
      <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showImage="true"/>
      <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showInfo="true"/>
    </c:if>
<footer>
<c:if test="${not empty preparedMessage.tags}">
  <lor:tags list="${preparedMessage.tags}" favoriteTags="${favoriteTags}" ignoreTags="${ignoreTags}"/>
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
          <c:if test="${template.prof.showSocial}">
          <div class="social-buttons">
            <a target="_blank" style="text-decoration: none"
               href="http://juick.com/post?body=<%= URLEncoder.encode("*LOR " + message.getTitle()+ ' '+tmpl.getMainUrlNoSlash()+message.getLink()) %>">
              <img border="0" src="/img/juick.png" width=16 height=16 alt="Juick" title="Share on Juick">
            </a>

            <a target="_blank" style="text-decoration: none"
               href="https://twitter.com/intent/tweet?text=<%= URLEncoder.encode(message.getTitle()) %>&amp;url=<%= URLEncoder.encode(tmpl.getMainUrlNoSlash()+message.getLink()) %>&amp;hashtags=<%= URLEncoder.encode("лор") %>">
              <img border="0" src="/img/twitter.png" width=16 height=16 alt="Share on Twitter" title="Share on Twitter">
            </a>

            <a target="_blank" style="text-decoration: none"
               href="https://plus.google.com/share?url=<%= URLEncoder.encode(tmpl.getMainUrlNoSlash()+message.getLink()) %>">
              <img border="0" src="/img/google-plus-icon.png" width=16 height=16 alt="Share on Google Plus" title="Share on Google Plus">
            </a>
          </div>
          </c:if>
          <ul id="topicMenu">
          <c:if test="${not message.expired}">
            <c:if test="${messageMenu.commentsAllowed}">
              <li><a href="comment-message.jsp?topic=${message.id}">Ответить на это сообщение</a></li>
            </c:if>
        </c:if>
        <c:if test="${messageMenu.editable}">
            <li><a href="edit.jsp?msgid=${message.id}">Править</a></li>
        </c:if>
        <c:if test="${messageMenu.deletable}">
          <li><a href="delete.jsp?msgid=${message.id}">Удалить</a></li>
        </c:if>
        <c:if test="${messageMenu.resolvable}">
            <c:if test="${message.resolved}">
                <li><a href="resolve.jsp?msgid=${message.id}&amp;resolve=no">Отметить как нерешенную</a></li>
            </c:if>
            <c:if test="${not message.resolved}">
                <li><a href="resolve.jsp?msgid=${message.id}&amp;resolve=yes">Отметить как решенную</a></li>
            </c:if>
        </c:if>
          </ul>
        <c:if test="${template.sessionAuthorized}">
          <br>${preparedMessage.postscoreInfo}
        </c:if>
        </div>
      </c:if>
</footer>
</div>
  <div style="clear: both"></div>
</article>

<c:if test="${template.sessionAuthorized}">
<script type="text/javascript">
  function memories_add(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { msgid : ${message.id}, add: "add" }
    }).done(function(t) {
              memories_form_setup(t);
            });
  }

  function memories_remove(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { id : memId, remove: "remove" }
    }).done(function(t) {
              memories_form_setup(0);
            });
  }

  function memories_form_setup(memId) {
    if (memId==0) {
      $("#memories_button").text("Отслеживать");

      $('#memories_button').unbind("click", memories_remove);
      $('#memories_button').bind("click", memories_add);
    } else {
      $("#memories_button").text("Не отслеживать");

      $('#memories_button').unbind("click", memories_add);
      $('#memories_button').bind("click", memories_remove);
    }
  }

  $(document).ready(function() {
    memId = ${messageMenu.memoriesId};

    $("#topicMenu").append("<li><a id=\"memories_button\" href=\"#\"></a></li>");

    memories_form_setup(memId);
  });
</script>
</c:if>
