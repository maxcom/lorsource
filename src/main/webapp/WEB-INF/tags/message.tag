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
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
  <!-- ${message.id}  -->

<article class=msg id="topic-${message.id}">
<c:if test="${showMenu}">
  <div class=title>
    <c:if test="${message.resolved}"><img src="/img/solved.png" alt="решено" title="решено"/></c:if>
    <c:if test="${not message.deleted}">
      <sec:authorize access="hasRole('ROLE_MODERATOR')">
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
      </sec:authorize>
    </c:if>
    <c:if test="${message.deleted}">
        <c:if test="${preparedMessage.deleteInfo == null}">
            <strong>Сообщение удалено</strong>
        </c:if>
        <c:if test="${preparedMessage.deleteInfo != null}">
            <strong>Сообщение удалено ${preparedMessage.deleteUser.nick}
                по причине '${preparedMessage.deleteInfo.reason}'</strong>
        </c:if>

        <sec:authorize access="hasRole('ROLE_MODERATOR')">
        <c:if test="${not message.expired}">
            [<a href="/undelete.jsp?msgid=${message.id}">восстановить</a>]
        </c:if>
        </sec:authorize>
    </c:if>
  &nbsp;</div>
</c:if>

<c:set var="showPhotos" value="${currentProperties.showPhotos}"/>
  <c:if test="${showPhotos}">
    <l:userpic author="${preparedMessage.author}" htmlPath="${configuration.HTMLPathPrefix}"/>
    <c:set var="msgBodyStyle" value="message-w-userpic"/>
  </c:if>

  <div class="fav-buttons">
    <a id="favs_button" href="#"><i class="icon-star"></i></a><br><span id="favs_count">${messageMenu.favsCount}</span><br>
    <a id="memories_button" href="#"><i class="icon-eye"></i></a><br><span id="memories_count">${messageMenu.memoriesCount}</span>
  </div>

  <div class="msg_body ${msgBodyStyle}">
  <h1 <c:if test="${enableSchema}">itemprop="headline"</c:if>>
    <a href="${message.link}"><l:title>${message.title}</l:title></a>
  </h1>

  <c:if test="${preparedMessage.image != null}">
    <lor:image enableSchema="true" preparedMessage="${preparedMessage}" showImage="true" enableEdit="${messageMenu.editable}"/>
  </c:if>

  <div <c:if test="${enableSchema}">itemprop="articleBody"</c:if>>
    ${preparedMessage.processedMessage}
  </div>

  <c:if test="${preparedMessage.image != null}">
    <lor:image preparedMessage="${preparedMessage}" showInfo="true"/>
  </c:if>

    <c:if test="${preparedMessage.section.pollPostAllowed}">
      <c:choose>
          <c:when test="${not message.commited}">
              <lor:poll-form poll="${preparedMessage.poll.poll}" enabled="false"/>
          </c:when>
          <c:otherwise>
              <lor:poll poll="${preparedMessage.poll}"/>

              <c:if test="${preparedMessage.poll.poll.current}">
                <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${message.id}">Проголосовать</a></p>
              </c:if>
          </c:otherwise>
      </c:choose>
    </c:if>

    <c:if test="${message.haveLink and not empty message.url}">
      <p <c:if test="${enableSchema}">itemprop="articleBody"</c:if>>
      &gt;&gt;&gt; <a href="${l:escapeHtml(message.url)}">${message.linkText}</a>

      </p>
    </c:if>
<footer>
<c:if test="${not empty preparedMessage.tags}">
  <l:tags list="${preparedMessage.tags}"/>
</c:if>
<div class=sign>
  <lor:sign postdate="${message.postdate}" user="${preparedMessage.author}" shortMode="false"/>
  <sec:authorize access="hasRole('ROLE_MODERATOR')">
    (<a href="sameip.jsp?msgid=${message.id}">${message.postIP}</a>)
  </sec:authorize>

  <span class="sign_more">
  <sec:authorize access="hasRole('ROLE_MODERATOR')">
    <c:if test="${preparedMessage.userAgent!=null}">
      <br>
      <c:out value="${preparedMessage.userAgent}" escapeXml="true"/>
    </c:if>
  </sec:authorize>
  <c:if test="${preparedMessage.section.premoderated and message.commitedby != 0}">
    <c:set var="commiter" value="${preparedMessage.commiter}" />
    <c:if test="${commiter.id != message.uid}">
        <c:set var="commitDate" value="${message.commitDate}" />
        <br>
        Проверено: <a href='<c:url value="/people/${l:urlEncode(commiter.nick, 'UTF-8')}/profile" />'>${commiter.nick}</a>
        <c:if test="${commitDate != null and not commitDate == message.postDate}">
            ( <fmt:formatDate value="${commitDate}"  type="both" pattern="dd.MM.yyyy hh:mm:ss" /> )
        </c:if>
    </c:if>
  </c:if>
  <sec:authorize access="hasRole('ROLE_ANON_USER')">
    <c:if test="${preparedMessage.editCount > 0}">
  <br>
  Последнее исправление: ${preparedMessage.lastEditor.nick} <lor:date date="${preparedMessage.lastHistoryDto.editdate}"/>
    (всего <a href="${message.link}/history">исправлений: ${preparedMessage.editCount}</a>)
    </c:if>
   </sec:authorize>
   </span>
</div>
    <c:if test="${!message.deleted && showMenu}">
      <div class=reply>
          <c:if test="${currentProperties.showSocial}">
          <div class="social-buttons">
            <c:set var="juickUrl" value="*LOR ${message.title} ${configuration.mainUrlNoSlash}${message.link}" />
            <a target="_blank" style="text-decoration: none"
               href="http://juick.com/post?body=${l:urlEncode(juickUrl, 'UTF-8')}">
              <img border="0" src="/img/juick.png" width=16 height=16 alt="Juick" title="Share on Juick">
            </a>
            <c:set var="twiterUrl" value="${configuration.mainUrlNoSlash}${message.link}" />
            <a target="_blank" style="text-decoration: none"
               href="https://twitter.com/intent/tweet?text=${l:urlEncode(message.title, 'UTF-8')}&amp;url=${l:urlEncode(twiterUrl, 'UTF-8')}&amp;hashtags=${l:urlEncode('лор', 'UTF-8')}">
              <img border="0" src="/img/twitter.png" width=16 height=16 alt="Share on Twitter" title="Share on Twitter">
            </a>
            <a target="_blank" style="text-decoration: none"
               href="https://plus.google.com/share?url=${l:urlEncode(twiterUrl, 'UTF-8')}">
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

        <sec:authorize access="hasRole('ROLE_ANON_USER')">
        <c:if test="${not message.expired}">
          <br>${preparedMessage.postscoreInfo}
        </c:if>
        </sec:authorize>
        </div>
      </c:if>
</footer>
</div>
  <div style="clear: both"></div>
</article>

<sec:authorize access="hasRole('ROLE_ANON_USER')">
<script type="text/javascript">
  function memories_add(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { msgid : ${message.id}, add: "add", watch: event.data.watch, csrf: "${fn:escapeXml(csrfToken)}" }
    }).done(function(t) {
       memories_form_setup(t['id'], event.data.watch);
       if (event.data.watch) {
         $('#memories_count').text(t['count']);
       } else {
         $('#favs_count').text(t['count']);
       }
    });
  }

  function memories_remove(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { id : event.data.id, remove: "remove", csrf: "${fn:escapeXml(csrfToken)}" }
    }).done(function(t) {
      memories_form_setup(0, event.data.watch);
      if (t>=0) {
        if (event.data.watch) {
          $('#memories_count').text(t);
        } else {
          $('#favs_count').text(t);
        }
      }
    });
  }

  function memories_form_setup(memId, watch) {
    var el;

    if (watch) {
      el = $('#memories_button');
    } else {
      el = $('#favs_button');
    }

    if (memId==0) {
      el.removeClass('selected');
      el.attr('title', watch?"Отслеживать":"В избранное");

      el.unbind("click", memories_remove);
      el.bind("click", {watch: watch}, memories_add);
    } else {
      el.addClass('selected');
      el.attr('title', watch?"Не отслеживать":"Удалить из избранного");

      el.unbind("click", memories_add);
      el.bind("click", {watch: watch, id: memId}, memories_remove);
    }
  }

  $(document).ready(function() {
    memories_form_setup(${messageMenu.memoriesId}, true);
    memories_form_setup(${messageMenu.favsId}, false);
  });
</script>
</sec:authorize>
