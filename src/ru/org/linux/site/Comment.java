package ru.org.linux.site;

import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.Map;
import java.util.logging.Logger;

import ru.org.linux.util.*;

public class Comment implements Serializable {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final int msgid;
  private final String title;
  private final int userid;
  private final int replyto;
  private final int topic;
  private final boolean deleted;
  private final Timestamp postdate;
  private final String message;
  private final DeleteInfo deleteInfo;

  public Comment(Connection db, ResultSet rs) throws SQLException {
    msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userid=rs.getInt("userid");
    message=rs.getString("message");

    if (deleted) {
      deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);
    } else {
      deleteInfo = null;
    }
  }

  public Comment(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs=st.executeQuery("SELECT " +
        "postdate, topic, users.id as userid, comments.id as msgid, comments.title, " +
        "deleted, replyto, message " +
        "FROM comments, users, msgbase " +
        "WHERE comments.id="+msgid+" AND comments.id=msgbase.id AND comments.userid=users.id");

    if (!rs.next()) throw new MessageNotFoundException(msgid);

    this.msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    message=rs.getString("message");
    userid=rs.getInt("userid");

    st.close();

    if (deleted) {
      deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);
    } else {
      deleteInfo = null;
    }
  }

  public String printMessage(Template tmpl, Connection db, CommentList comments, boolean showMenu, boolean moderatorMode, String user, boolean expired)
      throws IOException, UtilException, SQLException, UserNotFoundException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(msgid).append(" -->\n");

    out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");

    User author = User.getUserCached(db, userid);

    if (showMenu) {
      out.append("<tr class=title><td>");

      if (!deleted) {
        out.append("[<a href=\"/jump-message.jsp?msgid=").append(topic).append("&amp;cid=").append(msgid).append("\">#</a>]");
      }

      if (!expired && !deleted) {
        out.append("[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append("\">Ответить</a>]");
      }

      if (!deleted && (moderatorMode || author.getNick().equals(user))) {
        out.append("[<a href=\"delete_comment.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      }

      if (moderatorMode) {
        out.append("[<a href=\"sameip.jsp?msgid=").append(msgid).append("\">Другие с этого IP</a>]");
      }

      if (deleted) {
        if (deleteInfo==null) {
          out.append("<strong>Сообщение удалено</strong>");
        } else {
          out.append("<strong>Сообщение удалено ").append(deleteInfo.getNick()).append(" по причине '").append(HTMLFormatter.htmlSpecialChars(deleteInfo.getReason())).append("'</strong>");
        }
      }

      out.append("&nbsp;</td></tr>");
    }

    if (replyto!=0 && showMenu) {
      CommentNode replyNode = comments.getNode(replyto);
      if (replyNode!=null) {
        Comment reply = replyNode.getComment();

        out.append("<tr class=title><td>");

        out.append("Ответ на: <a href=\"");

        int replyPage = comments.getCommentPage(reply, tmpl);
        if (replyPage>0) {
          out.append("view-message.jsp?msgid=").append(topic).append("&amp;page=").append(replyPage).append('#').append(replyto);
        } else {
          out.append("view-message.jsp?msgid=").append(topic).append('#').append(replyto);          
        }

        out.append("\">");

        User replyAuthor = User.getUserCached(db, reply.getUserid());

        out.append(StringUtil.makeTitle(reply.getTitle())).append("</a> от ").append(replyAuthor.getNick()).append(' ').append(Template.dateFormat.format(reply.getPostdate()));
      } else {
        logger.warning("Weak reply #"+replyto+" on comment="+msgid+" msgid="+topic);
      }
    }

    out.append("<tr class=body><td>");
    out.append("<div class=msg>");

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

    out.append("<h2><a name=").append(msgid).append('>').append(title).append("</a></h2>");

    out.append(message);

    out.append("<p>");

    out.append(author.getSignature(moderatorMode, postdate));

    if (!expired && !deleted && showMenu)
      out.append("<p><font size=2>[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append("\">Ответить на это сообщение</a>]</font>");

    if (tbl) out.append("</td></tr></table>");
      out.append("</div></td></tr>");
      out.append("</table><p>");

    return out.toString();
  }

  public int getMessageId() {
    return msgid;
  }

  public int getReplyTo() {
    return replyto;
  }

  public boolean isIgnored(Map<Integer, String> ignoreList) {
    return ignoreList != null && !ignoreList.isEmpty() && ignoreList.keySet().contains(userid);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public int getTopic() {
    return topic;
  }

  public String getTitle() {
    return title;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public int getUserid() {
    return userid;
  }

  public String getMessageText() {
    return message;
  }
}
