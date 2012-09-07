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
<c:set var="pages" value="${currentProfile.properties.messages}"/>
<c:set var="style" value="${currentStyle}" />
<c:set var="config" value="${configuration}" />

<%
  Topic topic = (Topic)request.getAttribute("topic");
  int pages = (Integer)request.getAttribute("pages");
%>

<c:set var="commentsLinks">
  <c:if test="${topic.commentCount > 0}">
  <%
      out.append(" [<a href=\"");
      out.append(topic.getLink());
      out.append("\">");

      int stat1 = topic.getCommentCount();
      out.append(Integer.toString(stat1));

      if (stat1 % 100 >= 10 && stat1 % 100 <= 20) {
        out.append("&nbsp;комментариев</a>");
      } else {
        switch (stat1 % 10) {
          case 1:
            out.append("&nbsp;комментарий</a>");
            break;
          case 2:
          case 3:
          case 4:
            out.append("&nbsp;комментария</a>");
            break;
          default:
            out.append("&nbsp;комментариев</a>");
            break;
        }
      }

      if (pages != 1) {
        int PG_COUNT=3;

        out.append("&nbsp;(стр.");
        boolean dots = false;

        for (int i = 1; i < pages; i++) {
          if (pages>PG_COUNT*3 && (i>PG_COUNT && i<pages-PG_COUNT)) {
            if (!dots) {
              out.append(" ...");
              dots = true;
            }

            continue;
          }

          out.append(" <a href=\"").append(topic.getLinkPage(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
        }

        out.append(')');
      }
      out.append(']');
  %>
  </c:if>
</c:set>

<c:if test="${not topic.minor}">
<article class=news id="topic-${topic.id}">
<%
  String url = topic.getUrl();
  boolean votepoll = preparedMessage.getSection().isPollPostAllowed();

  String image = preparedMessage.getGroup().getImage();
  Group group = preparedMessage.getGroup();
%>
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
  <%
    Configuration configuration = (Configuration)request.getAttribute("config");
    String currentStyle = (String)request.getAttribute("style");
    try {
      ImageInfo info = new ImageInfo(configuration.getHTMLPathPrefix() + currentStyle + image);
      out.append("<img src=\"/").append(currentStyle).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (IOException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append(currentStyle).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (BadImageException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append(currentStyle).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    }
%>
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  <c:if test="${preparedMessage.image != null}">
    <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showImage="true"/>
  </c:if>
  
  ${preparedMessage.processedMessage}
<%
  if (url != null) {
    if (url.isEmpty()) {
      url = topic.getLink();
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(url)).append("\">").append(topic.getLinktext()).append("</a>");
  }
%>
<c:if test="${preparedMessage.image != null}">
  <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showInfo="true"/>
</c:if>
<%
  if (votepoll) {
      %>
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
  <%
  }
%>
  </div>
<c:if test="${not empty preparedMessage.tags}">
  <l:tags list="${preparedMessage.tags}"/>
</c:if>

  <div class=sign>
  <c:choose>
    <c:when test="${preparedMessage.section.premoderated and topic.commited}">
      <lor:sign shortMode="true" postdate="${message.commitDate}" user="${preparedMessage.author}"/>
    </c:when>
    <c:otherwise>
      <lor:sign shortMode="true" postdate="${message.postdate}" user="${preparedMessage.author}"/>
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
