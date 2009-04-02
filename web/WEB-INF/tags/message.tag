<%@ tag import="java.io.FileNotFoundException" %>
<%@ tag import="java.sql.*" %>
<%@ tag import="java.util.List" %>
<%@ tag import="java.util.Random" %>
<%@ tag import="java.util.Set" %>
<%@ tag import="java.util.logging.Logger" %>
<%@ tag import="javax.servlet.http.HttpServletResponse" %>
<%@ tag import="org.apache.commons.lang.StringUtils" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.spring.AddMessageForm" %>
<%@ tag import="ru.org.linux.util.*" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="message" required="true" type="ru.org.linux.site.Message" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="user" type="java.lang.String"%>
<%@ attribute name="highlight" type="java.lang.Integer" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
                          
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

<%
  Template tmpl = Template.getTemplate(request);

  User author = User.getUserCached(db, message.getUid());
  User currentUser = User.getCurrentUser(db, session);

  int msgid = message.getMessageId();
%>

  <!-- <%= msgid%>  -->

<c:if test="${showMenu}">
  <div class=title>
    <c:if test="${not message.deleted}">[<a href="/view-message.jsp?msgid=<%= msgid %>">#</a>]<%
    if (tmpl.isModeratorSession() && message.getSection().isPremoderated() && !message.isCommited()) {
      out.append("[<a href=\"commit.jsp?msgid="+msgid+"\">Подтвердить</a>]");
    }

    if (tmpl.isModeratorSession() || author.getNick().equals(user)) {
      out.append("[<a href=\"delete.jsp?msgid=");
      out.print(msgid);
      out.append("\">Удалить</a>]");
    }

    if (tmpl.isModeratorSession()) {
      if (message.isVotePoll()) {
        out.append("[<a href=\"edit-vote.jsp?msgid=");
        out.print(msgid);
        out.print("\">Править</a>]");
      }
      
      out.append("[<a href=\"setpostscore.jsp?msgid=");
      out.print(msgid);
      out.print("\">Установить параметры</a>]");
      out.append("[<a href=\"mt.jsp?msgid=");
      out.print(msgid);
      out.append("\">Перенести</a>]");

      if (message.getSectionId() == 1) {
        out.append("[<a href=\"mtn.jsp?msgid=");
        out.print(msgid);
        out.append("\">Группа</a>]");
      }
    }

    if (currentUser!=null && message.isEditable(db, currentUser)) {
      out.append("[<a href=\"edit.jsp?msgid=");
      out.print(msgid);
      out.append("\">Править</a>]");
    }
    %></c:if><%
    if (message.isDeleted()) {
      Statement rts = db.createStatement();
      ResultSet rt = rts.executeQuery("SELECT nick,reason FROM del_info,users WHERE msgid=" + msgid + " AND users.id=del_info.delby");

      if (!rt.next()) {
        out.append("<strong>Сообщение удалено</strong>");
      } else {
        out.append("<strong>Сообщение удалено ").append(rt.getString("nick")).append(" по причине '").append(rt.getString("reason")).append("'</strong>");
      }

      rt.close();
      rts.close();
    }

%>
  &nbsp;</div>
</c:if>
<div class=msg>
<%
  boolean tbl = false;
  if (message.getSection().isImagepost()) {
    out.append("<table><tr><td valign=top align=center>");
    tbl = true;

    try {
      ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + message.getLinktext());
      out.append("<a href=\"/").append(message.getUrl()).append("\"><img src=\"/").append(message.getLinktext()).append("\" ALT=\"").append(message.getTitle()).append("\" ").append(info.getCode()).append(" ></a>");
    } catch (BadImageException e) {
      out.append("<a href=\"/").append(message.getUrl()).append("\">[bad image]</a>");
    } catch (FileNotFoundException e) {
      out.append("<a href=\"/").append(message.getUrl()).append("\">[no image]</a>");
    }

    out.append("</td><td valign=top>");
  }

  if (!message.getSection().isImagepost() && author.getPhoto() != null) {
    if (tmpl.getProf().getBoolean("photos")) {
      out.append("<table><tr><td valign=top align=center>");
      tbl = true;

      try {
        ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + "/photos/" + author.getPhoto());
        out.append("<img src=\"/photos/").append(author.getPhoto()).append("\" alt=\"").append(author.getNick()).append(" (фотография)\" ").append(info.getCode()).append(" >");
      } catch (BadImageException e) {
        Logger.getLogger("ru.org.linux").warning(StringUtil.getStackTrace(e));
      }

      out.append("</td><td valign=top>");
    }
  }

  out.append("<h1><a name=");
  out.print(msgid);
  out.append('>');
  out.append(message.getTitle());
  out.append("</a></h1>");

  if (message.isVotePoll()) {
    //Render poll
    try {
      int id = Poll.getPollIdByTopic(db, msgid);
      Poll poll = new Poll(db, id);
      out.append(poll.renderPoll(db, tmpl.getConfig(), tmpl.getProf(), highlight!=null?highlight:0));
      out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=");
      out.print(msgid);
      out.append("\">Проголосовать</a>");
    } catch (PollNotFoundException e) {
      out.append("[BAD POLL: not found]");
    } catch (BadImageException e) {
      out.append("[BAD POLL: bad image]");
    }
  } else {
    out.append(message.getProcessedMessage(db));
  }

  if (message.getUrl() != null && message.isHaveLink()) {
    out.append("<p>&gt;&gt;&gt; <a href=\"").append(message.getUrl()).append("\">").append(message.getLinktext()).append("</a>.");
  }

  if (message.getUrl() != null && message.getSection().isImagepost()) {
    try {
      ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + message.getUrl());

      out.append("<p><i>");
      out.print(info.getWidth());
      out.append('x');
      out.print(info.getHeight());
      out.append(", ");
      out.append(info.getSizeString());
      out.append("</i>");

      out.append("<p>&gt;&gt;&gt; <a href=\"/").append(message.getUrl()).append("\">Просмотр</a>.");
    } catch (BadImageException e) {
      out.append("<p>&gt;&gt;&gt; <a href=\"/").append(message.getUrl()).append("\">[BAD IMAGE!] Просмотр</a>.");
    } catch (FileNotFoundException e) {
      out.append("<p>&gt;&gt;&gt; <a href=\"/").append(message.getUrl()).append("\">[NO IMAGE!] Просмотр</a>.");
    }
  }

  if (message.getSectionId() == 1) {
    String tagLinks = Tags.getTagLinks(message.getTags());

    if (tagLinks.length() > 0) {
      out.append("<p>Метки: <i>");
      out.append(tagLinks);
      out.append("</i>");
    }
  }


  out.append("<div class=sign>");

  out.append(author.getSignature(tmpl.isModeratorSession(), message.getPostdate()));
  if (tmpl.isModeratorSession()) {
    out.append(" (<a href=\"sameip.jsp?msgid=");
    out.print(msgid);
    out.append("\">").append(message.getPostIP()).append("</a>)");
  }

  if (message.getCommitby() != 0) {
    User commiter = User.getUserCached(db, message.getCommitby());

    out.append("<br>");
    out.append(commiter.getCommitInfoLine(message.getPostdate(), message.getCommitDate()));
  }

  if (showMenu) {
    if (tmpl.isModeratorSession() && message.getUserAgent()!=null) {
      out.append("<br>");
      out.append(HTMLFormatter.htmlSpecialChars(message.getUserAgent()));
    }
  }

  out.append("</div>");

  if (!message.isDeleted() && showMenu) {
    out.append("<div class=reply>");
    if (!message.isExpired()) {
      out.append("[<a href=\"comment-message.jsp?msgid=");
      out.print(msgid);
      out.append("\">Ответить на это сообщение</a>] ").append(Message.getPostScoreInfo(message.getPostScore()));
    }

    out.append("</div>");
  }

  if (tbl) {
    out.append("</td></tr></table>");
  }
%>
</div>
