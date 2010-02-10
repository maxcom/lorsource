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

<div class=news id="topic-${message.id}">
<%
  Template tmpl = Template.getTemplate(request);

  int msgid = message.getId();
  String url = message.getUrl();
  String subj = message.getTitle();
  String linktext = message.getLinktext();
  boolean imagepost = message.getSection().isImagepost();
  boolean votepoll = message.isVotePoll();

  Group group;
  try {
    group = new Group(db, message.getGroupId());
  } catch (BadGroupException e) {
    throw new RuntimeException(e);
  }

  String image = group.getImage();
  boolean expired = message.isExpired();

  double messages = tmpl.getProf().getInt("messages");

  String mainlink = message.getLink();
  String jumplink = message.getLinkLastmod();
%>
<h2>
<a href="${fn:escapeXml(message.linkLastmod)}">${message.title}</a>
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
      NewsViewer.showMediumImage(tmpl.getConfig().getProperty("HTMLPathPrefix"), out, url, subj, linktext, !tmpl.isMobile());
    %>
  </c:if>
<%
  if (!votepoll) {
      out.append(message.getProcessedMessage(db, moderateMode));
  }
%>
<%
  if (url != null && !imagepost && !votepoll) {
    if (url.length()==0) {
      url = message.getLink();
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(linktext).append("</a>");
  } else if (imagepost) {
    ImageInfo info = new ImageInfo(tmpl.getConfig().getProperty("HTMLPathPrefix") + url);

    out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
    out.append(" (<i>").append(Integer.toString(info.getWidth())).append('x').append(Integer.toString(info.getHeight())).append(", ").append(info.getSizeString()).append("</i>)");
  } else if (votepoll) {
    try {
      Poll poll = Poll.getPollByTopic(db, msgid);
      out.append(poll.renderPoll(db, tmpl.getConfig(), tmpl.getProf()));
      if (poll.isCurrent()) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Голосовать</a>");
      }
      
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(jumplink).append("\">Результаты</a>");
    } catch (BadImageException e) {
//      NewsViewer.logger.warn("Bad Image for poll msgid="+msgid, e);
      out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
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
<%
  if (message.getSection().isPremoderated()) {
    String tagLinks = Tags.getTagLinks(db, msgid);

    if (tagLinks.length()>0) {
      out.append("<p class=\"tags\">Метки: <span class=tag>");
      out.append(tagLinks);
      out.append("</span></p>");
    }
  }

  User user;
  try {
    user = User.getUserCached(db, message.getUid());
  } catch (UserNotFoundException e) {
    throw new RuntimeException(e);
  }
%>
<div class=sign>
  <%
  if (message.getSection().isPremoderated() && message.isCommited()) {
    out.append(user.getSignature(false, message.getCommitDate(), true));
  } else {
    out.append(user.getSignature(false, message.getPostdate(), true));
  }
%>
</div>
<div class="nav">
  <%
    if (!moderateMode) {
      if (!expired) {
        out.append("[<a href=\"comment-message.jsp?msgid=").append(Integer.toString(msgid)).append("\">Добавить&nbsp;комментарий</a>]");
      }
    } else {
      if (currentUser != null && currentUser.canModerate()) {
        out.append("[<a href=\"commit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Подтвердить</a>]");
      }

      out.append(" [<a href=\"delete.jsp?msgid=").append(Integer.toString(msgid)).append("\">Удалить</a>]");

      if (currentUser != null && message.isEditable(db, currentUser)) {
        if (!votepoll) {
          out.append(" [<a href=\"edit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
        } else {
          out.append(" [<a href=\"edit-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
        }
      }
    }

    int stat1 = message.getCommentCount();

    if (stat1 > 0) {
      int pages = (int) Math.ceil(stat1 / messages);

      out.append(" [<a href=\"");

      if (pages <= 1) {
        out.append(jumplink);
      } else {
        out.append(mainlink);
      }

      out.append("\">");

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

        out.append("&nbsp;(стр.");
        for (int i = 1; i < pages; i++) {
          if (i == pages - 1) {
            out.append(" <a href=\"").append(message.getLinkPage(i)).append(urlAdd).append("\">").append(Integer.toString(i + 1)).append("</a>");
          } else {
            out.append(" <a href=\"").append(message.getLinkPage(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
          }
        }
        out.append(')');
      }
      out.append(']');
    }

  %>
  </div>
  </div>
</div>