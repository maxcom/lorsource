/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.logging.Logger;

import ru.org.linux.util.*;

public class CommentView {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  public String printMessage(Comment comment, Template tmpl, Connection db, CommentList comments, boolean showMenu, boolean moderatorMode, String user, boolean expired)
      throws IOException, UtilException, SQLException, UserNotFoundException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(comment.getMessageId()).append(" -->\n");

    User author = User.getUserCached(db, comment.getUserid());

    out.append("<div class=msg id=\"comment-").append(comment.getMessageId()).append("\">");
    
    if (showMenu) {
      printMenu(out, comment, comments, tmpl, db, expired);
    }

    out.append("<div class=\"msg_body\">");

    boolean tbl = false;
    if (author.getPhoto()!=null) {
      if (tmpl.getProf().getBoolean("photos")) {
        out.append("<table><tr><td valign=top align=center>");
        tbl=true;

        try {
          ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+"/photos/"+author.getPhoto());
          out.append("<img src=\"/photos/").append(author.getPhoto()).append("\" alt=\"").append(author.getNick()).append(" (фотография)\" ").append(info.getCode()).append(" >");
        } catch (BadImageException e) {
          logger.warning(StringUtil.getStackTrace(e));
        }

        out.append("</td><td valign=top>");
      }
    }

    out.append("<h2>").append(comment.getTitle()).append("</h2>");

    out.append(comment.getMessageText());

    out.append("<div class=sign>").append(author.getSignature(moderatorMode, comment.getPostdate()));
    
    if (moderatorMode) {
      out.append(" (<a href=\"sameip.jsp?msgid=").append(comment.getMessageId()).append("\">").append(comment.getPostIP()).append("</a>)");
      if (comment.getUserAgent()!=null) {
        out.append("<br>").append(HTMLFormatter.htmlSpecialChars(comment.getUserAgent()));
      }
    }
    
    out.append("</div>");

    if (!comment.isDeleted() && showMenu) {
      out.append("<div class=reply>");
      if (!expired) {
        out.append("[<a href=\"add_comment.jsp?topic=").append(comment.getTopic()).append("&amp;replyto=").append(comment.getMessageId()).append("\">Ответить на это сообщение</a>] ");
      }

      if ((moderatorMode || author.getNick().equals(user))) {
        out.append("[<a href=\"delete_comment.jsp?msgid=").append(comment.getMessageId()).append("\">Удалить</a>]");
      }

      out.append("</div>");
    }

    if (tbl) {
      out.append("</td></tr></table>");
    }

    out.append("</div>");
    out.append("</div>");

    return out.toString();
  }

  private void printMenu(StringBuffer out, Comment comment,
                         CommentList comments, Template tmpl,
                         Connection db, boolean expired) throws UtilException, SQLException, UserNotFoundException {
    DateFormat dateFormat = DateFormats.createDefault();

    out.append("<div class=title>");

    if (!comment.isDeleted()) {
      out.append("[<a href=\"/jump-message.jsp?msgid=").append(comment.getTopic()).append("&amp;cid=").append(comment.getMessageId()).append("\">#</a>]");
    }

    if (comment.isDeleted()) {
      if (comment.getDeleteInfo() ==null) {
        out.append("<strong>Сообщение удалено</strong>");
      } else {
        out.append("<strong>Сообщение удалено ").append(comment.getDeleteInfo().getNick()).append(" по причине '").append(HTMLFormatter.htmlSpecialChars(comment.getDeleteInfo().getReason())).append("'</strong>");
      }
    }

    if (comment.getReplyTo() != 0) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());
      if (replyNode != null) {
        Comment reply = replyNode.getComment();

        out.append(" Ответ на: <a href=\"");

        String urladd = "";
        if (!expired) {
          urladd = "&amp;lastmod="+comments.getLastModified();
        }

        int replyPage = comments.getCommentPage(reply, tmpl);
        if (replyPage > 0) {
          out.append("view-message.jsp?msgid=").append(comment.getTopic()).append(urladd).append("&amp;page=").append(replyPage).append('#').append(comment.getReplyTo());
        } else {
          out.append("view-message.jsp?msgid=").append(comment.getTopic()).append(urladd).append('#').append(comment.getReplyTo());
        }

        out.append("\"onclick=\"highlightMessage(").append(reply.getMessageId()).append(");\">");

        User replyAuthor = User.getUserCached(db, reply.getUserid());

        out.append(StringUtil.makeTitle(reply.getTitle())).append("</a> от ").append(replyAuthor.getNick()).append(' ').append(dateFormat.format(reply.getPostdate()));
      } else {
        logger.warning("Weak reply #" + comment.getReplyTo() + " on comment=" + comment.getMessageId() + " msgid=" + comment.getTopic());
      }
    }

    out.append("&nbsp;</div>");
  }
}
