<%@ tag import="ru.org.linux.group.Group" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.topic.Topic" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="java.io.IOException" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.topic.TopicMenu" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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
<c:set var="message" value="${preparedMessage.message}"/>

<%
  Template tmpl = Template.getTemplate(request);
  Topic message = preparedMessage.getMessage();
  int pages = message.getPageCount(tmpl.getProf().getMessages());
%>

<c:set var="commentsLinks">
  <c:if test="${message.commentCount > 0}">
  <%
      out.append(" [<a href=\"");
      out.append(message.getLink());
      out.append("\">");

      int stat1 = message.getCommentCount();
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

          out.append(" <a href=\"").append(message.getLinkPage(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
        }

        out.append(')');
      }
      out.append(']');
  %>
  </c:if>
</c:set>

<c:if test="${not message.minor}">
<div class=news id="topic-${message.id}">
<%
  String url = message.getUrl();
  boolean imagepost = preparedMessage.getSection().isImagepost();
  boolean votepoll = preparedMessage.getSection().isVotePoll();

  String image = preparedMessage.getGroup().getImage();
  Group group = preparedMessage.getGroup();
%>
<h2>
  <a href="${fn:escapeXml(message.link)}">${message.title}</a>
</h2>
<c:if test="${multiPortal}">
  <div class="group">
    ${preparedMessage.section.title} - ${preparedMessage.group.title}
    <c:if test="${not message.commited and preparedMessage.section.premoderated}">
      (не подтверждено)
    </c:if>
  </div>
</c:if>
<c:set var="group" value="${preparedMessage.group}"/>

<c:if test="${group.image != null}">
<div class="entry-userpic">
  <a href="${group.url}">
  <%
    try {
      ImageInfo info = new ImageInfo(tmpl.getConfig().getHTMLPathPrefix() + tmpl.getStyle() + image);
      out.append("<img src=\"/").append(tmpl.getStyle()).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (IOException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getStyle()).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (BadImageException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getStyle()).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    }
%>
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  <c:if test="${preparedMessage.section.imagepost}">
    <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showImage="true"/>
  </c:if>
  
  ${preparedMessage.processedMessage}
<%
  if (url != null && !imagepost && !votepoll) {
    if (url.isEmpty()) {
      url = message.getLink();
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(url)).append("\">").append(message.getLinktext()).append("</a>");
  } else if (imagepost) {
%>
  <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showInfo="true"/>
<%
  } else if (votepoll) {
      %>
        <c:choose>
            <c:when test="${not message.commited}">
                <lor:disabledPoll poll="${preparedMessage.poll}"/>
            </c:when>
            <c:otherwise>
                <lor:poll poll="${preparedMessage.poll}"/>
            </c:otherwise>
        </c:choose>
        <c:if test="${preparedMessage.poll.poll.current}">
          <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${message.id}">Голосовать</a>
        </c:if>

        <c:if test="${message.commited}">
          <p>&gt;&gt;&gt; <a href="${message.linkLastmod}">Результаты</a>
        </c:if>
  <%
  }
%>
  </div>
<c:if test="${preparedMessage.section.premoderated and not empty preparedMessage.tags}">
  <lor:tags list="${preparedMessage.tags}"/>
</c:if>

  <div class=sign>
  <c:choose>
    <c:when test="${preparedMessage.section.premoderated and message.commited}">
      <lor:sign shortMode="true" postdate="${message.commitDate}" user="${preparedMessage.author}"/>
    </c:when>
    <c:otherwise>
      <lor:sign shortMode="true" postdate="${message.postdate}" user="${preparedMessage.author}"/>
    </c:otherwise>
  </c:choose>
</div>
<div class="nav">
<c:if test="${not moderateMode and messageMenu.commentsAllowed}">
  [<a href="comment-message.jsp?topic=${message.id}">Добавить&nbsp;комментарий</a>]
</c:if>
  <c:if test="${moderateMode and template.sessionAuthorized}">
    <c:if test="${template.moderatorSession}">
      [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
    </c:if>

    <c:if test="${template.moderatorSession or template.currentUser.id == message.uid}">
       [<a href="delete.jsp?msgid=${message.id}">Удалить</a>]
    </c:if>

    <c:if test="${messageMenu.editable}">
       [<a href="edit.jsp?msgid=${message.id}">Править</a>]
    </c:if>
  </c:if>
  <c:out value="${commentsLinks}" escapeXml="false"/>
  </div>
  </div>
</div>
</c:if>

<c:if test="${message.minor}">
<div class=infoblock id="topic-${message.id}">
Мини-новость:
  <a href="${fn:escapeXml(message.link)}">${message.title}</a>

<c:if test="${multiPortal}">
    <c:if test="${not message.commited and preparedMessage.section.premoderated}">
      (не подтверждено)
    </c:if>
</c:if>

  <c:out value="${commentsLinks}" escapeXml="false"/>
</div>
</c:if>
