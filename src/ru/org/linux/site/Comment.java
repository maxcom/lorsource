package ru.org.linux.site;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;

import ru.org.linux.util.*;

public class Comment {
  private final int msgid;
  private String title;
  private String photo;
  private String nick;
  private int userScore;
  private int userMaxScore;
  private int replyto = 0;
  private int topic;
  private boolean deleted;
  private boolean expired;
  private Timestamp postdate;
  private boolean show = true;
  private final String message;

  public Comment(ResultSet rs) throws SQLException {
    msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    photo=rs.getString("photo");
    nick=rs.getString("nick");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    expired=rs.getBoolean("expired");
    postdate=rs.getTimestamp("postdate");
    userScore=rs.getInt("score");
    userMaxScore=rs.getInt("max_score");
    message=rs.getString("message");
  }

  public Comment(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs=st.executeQuery("SELECT postdate, topic, nick, score, max_score, comments.id as msgid, comments.title, photo, 'f', deleted, 'f' as expired, replyto, message FROM comments, users, msgbase WHERE comments.id="+msgid+" AND comments.id=msgbase.id AND comments.userid=users.id");

    if (!rs.next()) throw new MessageNotFoundException(replyto);

    this.msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    photo=rs.getString("photo");
    nick=rs.getString("nick");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    expired=rs.getBoolean("expired");
    postdate=rs.getTimestamp("postdate");
    userScore=rs.getInt("score");
    userMaxScore=rs.getInt("max_score");
    message=rs.getString("message");

    st.close();
  }

  public String printMessage(Template tmpl, Connection db, boolean showMenu, boolean masterMode, String urladd, boolean moderatorMode, String user)
      throws IOException, SQLException, UtilException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(msgid).append(" -->\n");

    out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");

    if (showMenu) {
      out.append("<tr class=title><td>");

      if (!deleted) {
        out.append("[<a href=\"/jump-message.jsp?msgid=").append(topic).append('#').append(msgid).append("\">#</a>]");
      }

      if (!show) {
        out.append("[hide] ");
      }

      if (!expired && !deleted) {
        if (masterMode) {
          out.append("[<a href=\"#rep\">Ответить</a>]");
        } else {
          out.append("[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append(urladd).append("\">Ответить</a>]");
        }
      }

      if (!deleted && (moderatorMode || nick.equals(user))) {
        out.append("[<a href=\"delete_comment.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      }

      if (moderatorMode) {
        out.append("[<a href=\"sameip.jsp?msgid=").append(msgid).append("\">Другие с этого IP</a>]");
      }

      if (deleted) {
        DeleteInfo deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);

        if (deleteInfo==null) {
          out.append("<strong>Сообщение удалено</strong>");
        } else {
          out.append("<strong>Сообщение удалено ").append(deleteInfo.getNick()).append(" по причине '").append(HTMLFormatter.htmlSpecialChars(deleteInfo.getReason())).append("'</strong>");
        }
      }

      out.append("&nbsp;</td></tr>");
    }

    if (replyto!=0 && !masterMode) {
      out.append("<tr class=title><td>");
      Statement rts=db.createStatement();
      ResultSet rt=rts.executeQuery("SELECT users.nick, comments.title, comments.postdate FROM comments, users WHERE users.id=comments.userid AND comments.id=" + replyto);

      if (rt.next())
        out.append("Ответ на: <a href=\"").append("view-message.jsp?msgid=").append(topic).append('#').append(replyto).append("\">").append(StringUtil.makeTitle(rt.getString("title"))).append("</a> от ").append(rt.getString("nick")).append(' ').append(Template.dateFormat.format(rt.getTimestamp("postdate")));

      rt.close();
      rts.close();
    }

    out.append("<tr class=body><td>");
    out.append("<div class=msg>");

    boolean tbl = false;
    if (photo!=null) {
      if (tmpl.getProf().getBoolean("photos")) {
        out.append("<table><tr><td valign=top align=center>");
        tbl=true;

        try {
          ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+"/photos/"+photo);
          out.append("<img src=\"/photos/").append(photo).append("\" alt=\"").append(nick).append(" (фотография)\" ").append(info.getCode()).append(" >");
        } catch (BadImageException e) {}

        out.append("</td><td valign=top>");
      }
    }

    out.append("<h2><a name=").append(msgid).append('>').append(title).append("</a></h2>");

//    out.append(storage.readMessage("msgbase", String.valueOf(msgid)));
    out.append(message);

    out.append("<p><i>").append(nick).append(' ');

    if (!"anonymous".equals(nick)) {
      out.append(User.getStars(userScore, userMaxScore)).append(' ');
        if (moderatorMode)
          out.append("(Score: ").append(userScore).append(" MaxScore: ").append(userMaxScore).append(") ");
    }

    out.append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>) (").append(Template.dateFormat.format(postdate)).append(")</i>");

    if (!expired && !deleted && !masterMode && showMenu)
      out.append("<p><font size=2>[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append(urladd).append("\">Ответить на это сообщение</a>]</font>");

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

  public void setShow(boolean show) {
    this.show = show;
  }

  public boolean isShowable() {
    return show;
  }

  public boolean isAnonymous() {
    return "anonymous".equals(nick) || userScore<User.ANONYMOUS_LEVEL_SCORE;
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
}
