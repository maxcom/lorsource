<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="message" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.topic.TopicMenu" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
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
  </div>
</c:if>

<c:set var="showPhotos" value="${template.prof.showPhotos}"/>
  <c:if test="${showPhotos}">
    <l:userpic author="${preparedMessage.author}"/>
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

    <c:if test="${not empty preparedMessage.tags}">
      <l:tags list="${preparedMessage.tags}"/>
    </c:if>

  <c:if test="${preparedMessage.image != null}">
    <lor:image enableSchema="true" preparedMessage="${preparedMessage}" showImage="true" enableEdit="${messageMenu.topicEditable}"/>
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
    <%
    out.append("&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(message.getUrl())).append("\">").append(message.getLinktext()).append("</a>");
%>
      </p>
    </c:if>
<footer>
<div class=sign>
  <lor:sign
          postdate="${message.postdate}"
          user="${preparedMessage.author}"
          shortMode="false"
          timeprop="dateCreated"
          author="true"
  />

  <c:if test="${preparedMessage.remark != null}">
    <span class="user-remark"><c:out value="${preparedMessage.remark.text}" escapeXml="true"/> </span>
  </c:if>
 
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
  <c:if test="${preparedMessage.section.premoderated and message.commited}">
    <c:if test="${preparedMessage.commiter != preparedMessage.author}">
      <br>Проверено: <lor:user link="true" user="${preparedMessage.commiter}"/>

      <c:if test="${message.commitDate!=null && message.commitDate != message.postdate}">
        (<lor:date date="${message.commitDate}" itemprop="datePublished"/>)
      </c:if>
    </c:if>
  </c:if>
  <c:if test="${template.sessionAuthorized}">
  <%
  if (preparedMessage.getEditCount()>0) {
  %>
  <br>
  Последнее исправление: <%= preparedMessage.getLastEditor().getNick() %><c:out value=" "/><lor:date date="<%= preparedMessage.getLastHistoryDto().getEditdate() %>"/>
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
        <c:if test="${template.sessionAuthorized and not message.expired}">
          <br>${preparedMessage.postscoreInfo}
        </c:if>
        </div>
      </c:if>
</footer>
</div>
  <div style="clear: both"></div>
</article>

<c:if test="${not template.sessionAuthorized}">
<script type="text/javascript">
    $(function() {
        $("#favs_button").click(function(event) {
            event.preventDefault();
            event.stopPropagation();
            $("#memories_button").popover('hide');
            $("#favs_button").popover('show');
        });
        $("#favs_button").popover({
            content: "Для добавления в избранное надо залогиниться!",
            autoReposition: false,
            trigger: 'manual'
        });

        $("#memories_button").click(function(event) {
             event.preventDefault();
             event.stopPropagation();
             $("#favs_button").popover('hide');
             $("#memories_button").popover('show');
        });
        $("#memories_button").popover({
            content: "Для добавления в отслеживаемое надо залогиниться!",
            autoReposition: false,
            trigger: 'manual'
        });

    });
</script>
</c:if>

<c:if test="${template.sessionAuthorized}">
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
</c:if>
