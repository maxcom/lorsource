<%@ tag import="java.io.File" %>
<%@ tag import="java.io.IOException" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ attribute name="currentUser" required="false" type="ru.org.linux.site.User" %>
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
<div class=news id="topic-${message.id}">
<%
  Template tmpl = Template.getTemplate(request);

  int msgid = message.getId();
  String url = message.getUrl();
  boolean imagepost = message.getSection().isImagepost();
  boolean votepoll = message.isVotePoll();

  Group group;
  try {
    group = new Group(db, message.getGroupId());
  } catch (BadGroupException e) {
    throw new RuntimeException(e);
  }

  String image = group.getImage();

  int pages = message.getPageCount(tmpl.getProf().getInt("messages"));
%>
<h2>
  <a href="${fn:escapeXml(message.link)}">${message.title}</a>
</h2>
<c:if test="${multiPortal}">
  <div class="group">
    ${message.section.title} - ${message.groupTitle}
    <c:if test="${not message.commited and message.section.premoderated}">
      (не подтверждено)
    </c:if>
  </div>
</c:if>
<c:set var="group" value="<%= group %>"/>

<c:if test="${group.image != null}">
<div class="entry-userpic">
  <a href="${group.url}">
  <%
    try {
      ImageInfo info = new ImageInfo(tmpl.getConfig().getProperty("HTMLPathPrefix") + tmpl.getProf().getString("style") + image);
      out.append("<img src=\"/").append(tmpl.getProf().getString("style")).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (IOException e) {
//      NewsViewer.logger.warn("Bad Image for group "+ message.getGroupId(), e);
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getProf().getString("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (BadImageException e) {
//      NewsViewer.logger.warn("Bad Image for group "+ message.getGroupId(), e);
      out.append("[bad image] <img class=newsimage src=\"/").append(tmpl.getProf().getString("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
    }
%>
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  <c:if test="<%= imagepost %>">
    <%
      NewsViewer.showMediumImage(tmpl.getConfig().getProperty("HTMLPathPrefix"), out, url, message.getTitle(), message.getLinktext(), !tmpl.isMobile());
    %>
  </c:if>
<c:if test="${not message.votePoll}">
<%
      out.append(message.getProcessedMessage(db, moderateMode));
%>
</c:if>
<%
  if (url != null && !imagepost && !votepoll) {
    if (url.length()==0) {
      url = message.getLink();
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(message.getLinktext()).append("</a>");
  } else if (imagepost) {
    String imageFilename = tmpl.getConfig().getProperty("HTMLPathPrefix") + url;
    ImageInfo info = new ImageInfo(imageFilename, ImageInfo.detectImageType(new File(imageFilename)));

    out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
    out.append(" (<i>").append(Integer.toString(info.getWidth())).append('x').append(Integer.toString(info.getHeight())).append(", ").append(info.getSizeString()).append("</i>)");
  } else if (votepoll) {
    try {
      Poll poll = Poll.getPollByTopic(db, msgid);
      %>
        <lor:poll poll="<%= new PreparedPoll(db, poll) %>"/>
      <%
      if (poll.isCurrent()) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Голосовать</a>");
      }

      out.append("<p>&gt;&gt;&gt; <a href=\"").append(message.getLinkLastmod()).append("\">Результаты</a>");
    } catch (IOException e) {
//      NewsViewer.logger.warn("Bad Image for poll msgid="+msgid, e);
      out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
    } catch (PollNotFoundException e) {
//     NewsViewer.logger.warn("Bad poll msgid="+msgid, e);
      out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
    }
  }
%>
  </div>
<c:if test="${message.section.premoderated}">
<%
  String tagLinks = Tags.getTagLinks(db, msgid);

  if (tagLinks.length() > 0) {
    out.append("<p class=\"tags\">Метки: <span class=tag>");
    out.append(tagLinks);
    out.append("</span></p>");
  }
%>
</c:if>
  <%
  User user;
  try {
    user = User.getUserCached(db, message.getUid());
  } catch (UserNotFoundException e) {
    throw new RuntimeException(e);
  }
%>
<div class=sign>
  <c:choose>
    <c:when test="${message.section.premoderated and message.commited}">
      <lor:sign shortMode="true" postdate="${message.commitDate}" user="<%= user %>"/>
    </c:when>
    <c:otherwise>
      <lor:sign shortMode="true" postdate="${message.postdate}" user="<%= user %>"/>
    </c:otherwise>
  </c:choose>
</div>
<div class="nav">
<c:if test="${not moderateMode and not message.expired}">
  [<a href="comment-message.jsp?msgid=${message.id}">Добавить&nbsp;комментарий</a>]
</c:if>
  <c:if test="${moderateMode and template.sessionAuthorized}">
    <c:if test="${template.moderatorSession}">
      [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
    </c:if>
<%
      if (tmpl.isModeratorSession() || currentUser.getId() == message.getUid()) {
        out.append(" [<a href=\"delete.jsp?msgid=").append(Integer.toString(msgid)).append("\">Удалить</a>]");
      }

      if (message.isEditable(db, currentUser)) {
        if (!votepoll) {
          out.append(" [<a href=\"edit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
        } else {
          out.append(" [<a href=\"edit-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
        }
      }
%>
  </c:if>
  <c:if test="${message.commentCount > 0}">
  <%
      out.append(" [<a href=\"");

      if (pages <= 1) {
        out.append(message.getLinkLastmod());
      } else {
        out.append(message.getLink());
      }

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
        String urlAdd = message.isExpired() ? "" : ("?lastmod=" + message.getLastModified().getTime());

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

          if (i == pages - 1) {
            out.append(" <a href=\"").append(message.getLinkPage(i)).append(urlAdd).append("\">").append(Integer.toString(i + 1)).append("</a>");
          } else {
            out.append(" <a href=\"").append(message.getLinkPage(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
          }
        }

        out.append(')');
      }
      out.append(']');
  %>
  </c:if>
  </div>
  </div>
</div>