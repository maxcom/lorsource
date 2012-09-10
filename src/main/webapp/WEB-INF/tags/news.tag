<%@ tag import="ru.org.linux.group.Group" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.topic.Topic" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="ru.org.linux.user.ProfileProperties" %>
<%@ tag import="ru.org.linux.spring.Configuration" %>
<%@ tag import="java.io.IOException" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.topic.TopicMenu" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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

<c:set var="topic" value="${preparedMessage.message}"/>

<c:set var="commentsLinks">
  <c:if test="${topic.commentCount > 0}">
  [<a href="${topic.link}">${topic.commentCount}&nbsp;<l:commentsWithSuffix stat="${topic.commentCount}" /></a><l:topicPaginator topic="${topic}" topicsPerPage="${currentProfile.properties.messages}" />]
  </c:if>
</c:set>

<c:if test="${not topic.minor}">
<article class=news id="topic-${topic.id}">
<h2>
  <a href="${fn:escapeXml(topic.link)}"><l:title>${topic.title}</l:title></a>
</h2>
<c:if test="${multiPortal}">
  <div class="group">
    ${preparedMessage.section.title} - ${preparedMessage.group.title}
    <c:if test="${not topic.commited and preparedMessage.section.premoderated}">
      (не подтверждено)
    </c:if>
  </div>
</c:if>
<c:set var="group" value="${preparedMessage.group}"/>

<c:if test="${group.image != null}">
<div class="entry-userpic">
  <a href="${group.url}">
  <l:groupImage group="${group}" htmlPath="${configuration.HTMLPathPrefix}" style="${currentStyle}" />
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  <c:if test="${preparedMessage.image != null}">
    <lor:image preparedImage="${preparedMessage.image}" topic="${topic}" showImage="true"/>
  </c:if>
  
  ${preparedMessage.processedMessage}
  <c:if test="${not empty topic.url}">
    <p>&gt;&gt;&gt; <a href="${l:escapeHtml(topic.url)}"/>${topic.linktext}</a>
  </c:if>
<c:if test="${preparedMessage.image != null}">
  <lor:image preparedImage="${preparedMessage.image}" topic="${topic}" showInfo="true"/>
</c:if>
<c:if test="${preparedMessage.section.pollPostAllowed}">
        <c:choose>
            <c:when test="${not topic.commited || preparedMessage.poll.poll.current}">
                <lor:poll-form poll="${preparedMessage.poll.poll}" enabled="${preparedMessage.poll.poll.current}"/>
            </c:when>
            <c:otherwise>
                <lor:poll poll="${preparedMessage.poll}"/>
            </c:otherwise>
        </c:choose>

        <c:if test="${topic.commited}">
          <p>&gt;&gt;&gt; <a href="${topic.linkLastmod}">Результаты</a>
        </c:if>
  </c:if>
  </div>
<c:if test="${not empty preparedMessage.tags}">
  <l:tags list="${preparedMessage.tags}"/>
</c:if>

  <div class=sign>
  <c:choose>
    <c:when test="${preparedMessage.section.premoderated and topic.commited}">
      <lor:sign shortMode="true" postdate="${topic.commitDate}" user="${preparedMessage.author}"/>
    </c:when>
    <c:otherwise>
      <lor:sign shortMode="true" postdate="${topic.postdate}" user="${preparedMessage.author}"/>
    </c:otherwise>
  </c:choose>
</div>
<div class="nav">
<c:if test="${not moderateMode and messageMenu.commentsAllowed}">
  [<a href="comment-message.jsp?topic=${topic.id}">Добавить&nbsp;комментарий</a>]
</c:if>
  <c:if test="${moderateMode and template.sessionAuthorized}">
    <c:if test="${template.moderatorSession}">
      [<a href="commit.jsp?msgid=${topic.id}">Подтвердить</a>]
    </c:if>

    <c:if test="${messageMenu.deletable}">
       [<a href="delete.jsp?msgid=${topic.id}">Удалить</a>]
    </c:if>

    <c:if test="${messageMenu.editable}">
       [<a href="edit.jsp?msgid=${topic.id}">Править</a>]
    </c:if>
  </c:if>
  <c:out value="${commentsLinks}" escapeXml="false"/>
  </div>
  </div>
</article>
</c:if>

<c:if test="${topic.minor}">
<article class="infoblock mini-news" id="topic-${topic.id}">
Мини-новость:
  <a href="${fn:escapeXml(topic.link)}"><l:title>${topic.title}</l:title></a>

<c:if test="${multiPortal}">
    <c:if test="${not topic.commited and preparedMessage.section.premoderated}">
      (не подтверждено)
    </c:if>
</c:if>

  <c:out value="${commentsLinks}" escapeXml="false"/>
</article>
</c:if>
