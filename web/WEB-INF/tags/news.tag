<%@ tag import="java.io.IOException" %>
<%@ tag import="java.sql.Timestamp" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
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
<div class=news>
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
  Timestamp lastmod = message.getLastModified();
  boolean expired = message.isExpired();

  if (lastmod == null) {
    lastmod = new Timestamp(0);
  }
  double messages = tmpl.getProf().getInt("messages");

  out.print("<h2>");

  String mainlink = "view-message.jsp?msgid=" + msgid;
  String jumplink;

  if (!expired) {
    jumplink = mainlink+ "&amp;lastmod=" + lastmod.getTime();
  } else {
    jumplink = mainlink;
  }

  out.append("<a href=\"").append(jumplink).append("\">");
  out.append(subj);
  out.append("</a>");

  out.append("</h2>");
%>
<c:if test="${multiPortal}">
  <div class="group">
    ${message.section.title} - ${message.groupTitle}
    <c:if test="${not message.commited and message.section.premoderated}">
      (не подтверждено)
    </c:if>
  </div>
</c:if>
  <%

  if (image != null) {
    out.append("<div class=\"entry-userpic\">");
    out.append("<a href=\"view-news.jsp?section=").append(Integer.toString(message.getSectionId())).append("&amp;group=").append(Integer.toString(message.getGroupId())).append("\">");
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
    out.append("</a>");
    out.append("</div>");
  }

  out.append("<div class=\"entry-body\">");
  out.append("<div class=msg>\n");

  if (!votepoll) {
      out.append(message.getProcessedMessage(db, moderateMode));
  }

  if (url != null && !imagepost && !votepoll) {
    if (url.length()==0) {
      url = "view-message.jsp?msgid="+msgid;
    }

    out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(linktext).append("</a>");
  } else if (imagepost) {
    NewsViewer.showMediumImage(tmpl.getConfig().getProperty("HTMLPathPrefix"), out, url, subj, linktext);
  } else if (votepoll) {
    try {
      Poll poll = Poll.getPollByTopic(db, msgid);
  out.append(poll.renderPoll(db, tmpl.getConfig(), tmpl.getProf()));
      out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Голосовать</a>");
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

  out.append("</div>");

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

  out.append("<div class=sign>");
  if (message.getSection().isPremoderated() && message.isCommited()) {
    out.append(user.getSignature(false, message.getCommitDate(), true, tmpl.getStyle()));    
  } else {
    out.append(user.getSignature(false, message.getPostdate(), true, tmpl.getStyle()));
  }

  out.append("</div>");

  if (!(boolean) moderateMode) {
    out.append("<div class=\"nav\">");

    if (!expired) {
      out.append("[<a href=\"comment-message.jsp?msgid=").append(Integer.toString(msgid)).append("\">Добавить&nbsp;комментарий</a>]");
    }

    int stat1 = message.getCommentCount();

    if (stat1 > 0) {
      int pages = (int) Math.ceil(stat1 / messages);

  out.append(" [<a href=\"");

      if (pages<=1) {
        out.append(jumplink);
      } else {
        out.append(mainlink);
      }

      out.append("\">");

//        if (stat1 % 10 == 1 && stat1 % 100 != 11) {
//          out.append("Добавлен&nbsp;");
//        } else {
//          out.append("Добавлено&nbsp;");
//        }

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

  if (pages != 1){
    out.append("&nbsp;(стр.");
    for (int i = 1; i < pages; i++) {
          if (i==pages-1) {
            out.append(" <a href=\"").append(jumplink).append("&amp;page=").append(Integer.toString(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
          } else {
            out.append(" <a href=\"").append(mainlink).append("&amp;page=").append(Integer.toString(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
          }
        }
    out.append(')');
  }
  out.append(']');
    }

    out.append("</div>");
  } else if ((boolean) moderateMode) {
    out.append("<div class=nav>");
    out.append("[<a href=\"commit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Подтвердить</a>]");
    out.append(" [<a href=\"delete.jsp?msgid=").append(Integer.toString(msgid)).append("\">Удалить</a>]");
    if (!votepoll) {
      out.append(" [<a href=\"edit.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
    } else {
      out.append(" [<a href=\"edit-vote.jsp?msgid=").append(Integer.toString(msgid)).append("\">Править</a>]");
    }

    out.append("</div>");
  }
  out.append("</div>");
%>
</div>