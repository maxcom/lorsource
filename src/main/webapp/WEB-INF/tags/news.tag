<%@ tag import="java.io.File" %>
<%@ tag import="java.io.IOException" %>
<%@ tag import="ru.org.linux.site.Group" %>
<%@ tag import="ru.org.linux.site.NewsViewer" %>
<%@ tag import="ru.org.linux.spring.dao.TagDao" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.site.PreparedMessage" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ attribute name="disablePoll" required="false" type="java.lang.Boolean" %>
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
<%
  Template tmpl = Template.getTemplate(request);
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
  int msgid = message.getId();
  String url = message.getUrl();
  boolean imagepost = preparedMessage.getSection().isImagepost();
  boolean votepoll = message.isVotePoll();

  String image = preparedMessage.getGroup().getImage();
  Group group = preparedMessage.getGroup();
%>
<h2>
  <a href="${fn:escapeXml(message.link)}">${message.title}</a>
</h2>
<c:if test="${multiPortal}">
  <div class="group">
    ${preparedMessage.section.title} - ${message.groupTitle}
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
      ImageInfo info = new ImageInfo(tmpl.getConfig().getProperty("HTMLPathPrefix") + tmpl.getProf().getStyle() + image);
      out.append("<img src=\"/").append(tmpl.getProf().getStyle()).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (IOException e) {
//      NewsViewer.logger.warn("Bad Image for group "+ message.getGroupId(), e);
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getProf().getStyle()).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (BadImageException e) {
//      NewsViewer.logger.warn("Bad Image for group "+ message.getGroupId(), e);
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getProf().getStyle()).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    }
%>
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  <c:if test="${preparedMessage.section.imagepost}">
    <%
      out.append(NewsViewer.showMediumImage(tmpl.getConfig().getProperty("HTMLPathPrefix"), message, true));
    %>
  </c:if>
  
  ${preparedMessage.processedMessage}
<%
  if (url != null && !imagepost && !votepoll) {
    if (url.length()==0) {
      url = message.getLink();
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(url)).append("\">").append(message.getLinktext()).append("</a>");
  } else if (imagepost) {
    String imageFilename = tmpl.getConfig().getProperty("HTMLPathPrefix") + url;
    out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
    try {
      ImageInfo info = new ImageInfo(imageFilename, ImageInfo.detectImageType(new File(imageFilename)));

      out.append(" (<i>").append(Integer.toString(info.getWidth())).append('x').append(Integer.toString(info.getHeight())).append(", ").append(info.getSizeString()).append("</i>)");
    } catch (IOException e) {
      out.append("(BAD IMAGE)");
    } catch (BadImageException e) {
      out.append("(BAD IMAGE)");
    }
  } else if (votepoll) {
      %>
        <c:choose>
            <c:when test="${disablePoll}">
                <lor:disabledPoll poll="${preparedMessage.poll}"/>
            </c:when>
            <c:otherwise>
                <lor:poll poll="${preparedMessage.poll}"/>
            </c:otherwise>
        </c:choose>
        <c:if test="${preparedMessage.poll.poll.current}">
          <p>&gt;&gt;&gt; <a href="vote-vote.jsp?msgid=${message.id}">Голосовать</a>
        </c:if>

        <p>&gt;&gt;&gt; <a href="${message.linkLastmod}">Результаты</a>
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
<c:set var="commentsAllowed"><%= message.isCommentsAllowed(tmpl.getCurrentUser()) %></c:set>
<c:if test="${not moderateMode and commentsAllowed}">
  [<a href="comment-message.jsp?topic=${message.id}">Добавить&nbsp;комментарий</a>]
</c:if>
  <c:if test="${moderateMode and template.sessionAuthorized}">
    <c:if test="${template.moderatorSession}">
      [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
    </c:if>

    <c:if test="${template.moderatorSession or template.currentUser.id == message.uid}">
       [<a href="delete.jsp?msgid=${message.id}">Удалить</a>]
    </c:if>
<%
      if (preparedMessage.isEditable(tmpl.getCurrentUser())) {
        out.append(" [<a href=\"edit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
      }
%>
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
