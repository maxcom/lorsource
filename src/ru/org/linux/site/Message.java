package ru.org.linux.site;

import java.io.IOException;
import java.sql.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ru.org.linux.util.*;

public class Message {
  public static final int SCROLL_NOSCROLL = 0;
  public static final int SCROLL_SECTION = 1;
  public static final int SCROLL_GROUP = 2;

  public static final int SECTION_LINKS = 4;

  private int msgid;
  private int postscore;
  private boolean imagepost;
  private String linktext;
  private String url;
  private String title;
  private String photo;
  private String nick;
  private int guid;
  private boolean deleted;
  private boolean expired;
  private int commitby;
  private boolean havelink;
  private Timestamp postdate;
  private Timestamp commitDate;
  private final String portalTitle;
  private final String groupTitle;
  private Timestamp lastModified;
  private final int section;
  private boolean comment;
  private final int commentCount;
  private final int userScore;
  private final int userMaxScore;
  private final boolean moderate;
  private final String message;

  public Message(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st=db.createStatement();

    ResultSet rs=st.executeQuery("SELECT postdate, nick, topics.id as msgid, topics.title, sections.comment, topics.groupid as guid, photo, topics.url, topics.linktext, sections.name as ptitle, groups.title as gtitle, imagepost, havelink, section, postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, commitdate, topics.stat1, score, max_score, postscore, topics.moderate, message FROM topics, users, groups, sections, msgbase WHERE topics.id="+msgid+" AND topics.userid=users.id AND topics.groupid=groups.id AND groups.section=sections.id AND topics.id=msgbase.id");
    if (!rs.next()) throw new MessageNotFoundException(msgid);

    this.msgid=rs.getInt("msgid");
    postscore =rs.getInt("postscore");
    userScore=rs.getInt("score");
    userMaxScore=rs.getInt("max_score");
    imagepost=rs.getBoolean("imagepost");
    linktext=rs.getString("linktext");
    url=rs.getString("url");
    title=StringUtil.makeTitle(rs.getString("title"));
    photo=rs.getString("photo");
    nick=rs.getString("nick");
    guid=rs.getInt("guid");
    deleted=rs.getBoolean("deleted");
    expired=rs.getBoolean("expired");
    havelink=rs.getBoolean("havelink");
    postdate=rs.getTimestamp("postdate");
    commitDate=rs.getTimestamp("commitdate");
    commitby = rs.getInt("commitby");
    portalTitle = rs.getString("ptitle");
    groupTitle = rs.getString("gtitle");
    lastModified = rs.getTimestamp("lastmod");
    section=rs.getInt("section");
    comment=rs.getBoolean("comment");
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");

    rs.close();
    st.close();
  }

  public boolean isExpired() {
    return expired;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String printMessage(Template tmpl, Connection db, boolean showMenu, String user)
      throws SQLException, IOException, UserNotFoundException, UtilException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(msgid).append(" -->\n");

    out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");

    if (showMenu) {
      out.append("<tr class=title><td>");

      if (!deleted) {
        out.append("[<a href=\"/jump-message.jsp?msgid=").append(msgid).append("\">#</a>]");
      }

      if (!isExpired() && !isDeleted())
        out.append("[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Ответить</a>]");

      if (!isDeleted() && (tmpl.isModeratorSession() || nick.equals(user))) {
        out.append("[<a href=\"delete.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      }

      if (!isDeleted() && tmpl.isModeratorSession()) {
        out.append("[<a href=\"edit.jsp?msgid=").append(msgid).append("\">Править</a>]");
        out.append("[<a href=\"setpostscore.jsp?msgid=").append(msgid).append("\">Установить уровень записи комментариев</a>]");
        out.append("[<a href=\"notop.jsp?msgid=").append(msgid).append("\">Удалить из TOP10</a>]");
        out.append("[<a href=\"mt.jsp?msgid=").append(msgid).append("\">Перенести</a>]");
      }

      if (tmpl.isModeratorSession()) {
        out.append("[<a href=\"sameip.jsp?msgid=").append(msgid).append("\">Другие с этого IP</a>]");
      }

      if (isDeleted()) {
        Statement rts=db.createStatement();
        ResultSet rt=rts.executeQuery("SELECT nick,reason FROM del_info,users WHERE msgid="+msgid+" AND users.id=del_info.delby");

        if (!rt.next())
          out.append("<strong>Сообщение удалено</strong>");
        else
          out.append("<strong>Сообщение удалено ").append(rt.getString("nick")).append(" по причине '").append(rt.getString("reason")).append("'</strong>");

        rt.close();
        rts.close();
      }

      out.append("&nbsp;</td></tr>");
    }

    out.append("<tr class=body><td>");
    out.append("<div class=msg>");

    boolean tbl = false;
    if (imagepost) {
      out.append("<table><tr><td valign=top align=center>");
      tbl=true;

      try {
        ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+linktext);
        out.append("<a href=\"").append(url).append("\"><img src=\"/").append(linktext).append("\" ALT=\"").append(title).append("\" ").append(info.getCode()).append(" ></a>");
      } catch (BadImageException e) {
        out.append("<a href=\"").append(url).append("\">[bad image]</a>");
      }

      out.append("</td><td valign=top>");
    }

    if (!imagepost && photo!=null) {
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

    if (url!=null && havelink)
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">").append(linktext).append("</a>.");

    if (url!=null && imagepost) {
      try {
        ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+url);

        out.append("<p><i>").append(info.getWidth()).append('x').append(info.getHeight()).append(", ").append(info.getSizeString()).append("</i>");

        out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">Просмотр</a>.");
      } catch (BadImageException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">[BROKEN IMAGE!] Просмотр</a>.");
      }
    }

    out.append("<p>");

    out.append(User.getUserInfoLine(tmpl, nick, userScore, userMaxScore, postdate));

    if (commitby!=0) {
      User commiter = new User(db, commitby);

      out.append("<br>");
      out.append(commiter.getCommitInfoLine(postdate, commitDate));
    }

    if (!expired && !deleted && showMenu)
      out.append("<p><font size=2>[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Ответить на это сообщение</a>] ").append(getPostScoreInfo(postscore)).append("</font>");

    if (tbl) out.append("</td></tr></table>");
    out.append("</div></td></tr>");
    out.append("</table><p>");

    return out.toString();
  }

  public String getTitle() {
    return title;
  }

  public String getPortalTitle() {
    return portalTitle;
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public Timestamp getLastModified() {
    if (lastModified==null)
      return new Timestamp(0);

    return lastModified;
  }

  public int getGroupId() {
    return guid;
  }

  public int getSectionId() {
    return section;
  }

  public boolean isCommentEnabled() {
    return comment;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public int getPostScore() {
    return postscore;
  }

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case 0:
        return "";
      case 50:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      case 100:
        return "<b>Ограничение на отправку комментариев</b>: "+ User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на отправку комментариев</b>: "+ User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на отправку комментариев</b>: "+ User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на отправку комментариев</b>: "+ User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: "+ User.getStars(500, 500);
      default:
        return "<b>Ограничение на отправку комментариев</b>: score="+postscore;
    }
  }

  public int getScrollMode() {
   switch (getSectionId()) {
    case 1: /* news*/
    case 3: /* screenshots */
      return SCROLL_SECTION;
    case 2: /* forum */
      return SCROLL_GROUP;
    default:
      return SCROLL_NOSCROLL;
   }
  }

  public int getNextMessage(Connection db, int scrollMode) throws SQLException {
    PreparedStatement pst;

    switch (scrollMode) {
      case SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics, groups WHERE topics.groupid=groups.id AND topics.commitdate=(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, section);
        break;

      case SCROLL_GROUP:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.id=(SELECT min(topics.id) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id>? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setInt(1, msgid);
        pst.setInt(2, guid);
        break;

      case SCROLL_NOSCROLL:
      default:
        return 0;
    }

    try {
      ResultSet rs = pst.executeQuery();

      if (!rs.next())
        return 0;

      return rs.getInt("msgid");
    } finally {
      pst.close();
    }
  }

  public int getPreviousMessage(Connection db, int scrollMode) throws SQLException {
    PreparedStatement pst;

    switch (scrollMode) {
      case SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics, groups WHERE topics.groupid=groups.id AND topics.commitdate=(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, section);
        break;

      case SCROLL_GROUP:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.id=(SELECT max(topics.id) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id<? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setInt(1, msgid);
        pst.setInt(2, guid);
        break;

      case SCROLL_NOSCROLL:
      default:
        return 0;
    }

    try {
      ResultSet rs = pst.executeQuery();

      if (!rs.next())
        return 0;

      return rs.getInt("msgid");
    } finally {
      pst.close();
    }
  }

  public int getMessageId() {
    return msgid;
  }

  public boolean isCommited() {
    return moderate;
  }

  public int getPageCount(int messages) {
    return (int) Math.ceil(commentCount/((double) messages));
  }

  public String getMessageText() {
    return message;
  }


  public void updateMessageText(Connection db, String text) throws SQLException {

    PreparedStatement pst = db.prepareStatement("UPDATE msgbase SET message=? WHERE id=?");
    pst.setString(1, text);
    pst.setInt(2, msgid);
    pst.executeUpdate();
  }

  public String getUrl() {
    return url;
  }

  public String getLinktext() {
    return linktext;
  }

  public static void addTopic(Connection db, Template tmpl, HttpSession session, HttpServletRequest request, Group group) throws SQLException, UserNotFoundException, ServletParameterException, UtilException, IOException, BadImageException, InterruptedException, BadInputException, BadPasswordException, AccessViolationException, DuplicationException {
    String title = request.getParameter("title");
    if (title == null) {
      title = "";
    }

    title = HTMLFormatter.htmlSpecialChars(title);
    if ("".equals(title.trim())) {
      throw new BadInputException("заголовок сообщения не может быть пустым");
    }

    String linktext = request.getParameter("linktext");
    if (linktext != null && "".equals(linktext)) {
      linktext = null;
    }
    if (linktext != null) {
      linktext = HTMLFormatter.htmlSpecialChars(linktext);
    }

    ScreenshotProcessor screenshot = null;

    if (group.isImagePostAllowed()) {
      screenshot = new ScreenshotProcessor(request.getParameter("image"));
    }

    String msg = request.getParameter("msg");

    boolean userhtml = tmpl.getParameters().getBoolean("texttype");
    boolean autourl = tmpl.getParameters().getBoolean("autourl");

    String url = request.getParameter("url");
    if (url != null && "".equals(url)) {
      url = null;
    }

    // checks is over
    User user;

    if (session == null || session.getAttribute("login") == null || !((Boolean) session.getAttribute("login")).booleanValue())     {
      if (request.getParameter("nick") == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = new User(db, request.getParameter("nick"));
      user.checkPassword(request.getParameter("password"));
    } else {
      user = new User(db, (String) session.getAttribute("nick"));
      user.checkBlocked();
    }

    if (user.isAnonymous()) {
      if (msg.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (msg.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }

    Statement st = db.createStatement();

    if (!group.isTopicPostingAllowed(user)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    String mode = tmpl.getParameters().getString("mode");

    if ("pre".equals(mode) && !group.isPreformatAllowed()) {
      throw new AccessViolationException("В группу нельзя добавлять преформатированные сообщения");
    }
    if (("ntobr".equals(mode) || "tex".equals(mode) || "quot".equals(mode)) && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }
    if (userhtml && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }

    if (!group.isImagePostAllowed()) {
      if (url != null) {
        if (linktext == null) {
          throw new BadInputException("указан URL без текста");
        }
        url = URLUtil.fixURL(url);
      }
    }

    int maxlength = 80;
    if (group.getSectionId() == 1) {
      maxlength = 40;
    }
    HTMLFormatter form = new HTMLFormatter(msg);
    form.setMaxLength(maxlength);
    if ("pre".equals(mode)) {
      form.enablePreformatMode();
    }
    if (autourl) {
      form.enableUrlHighLightMode();
    }
    if ("ntobr".equals(mode)) {
      form.enableNewLineMode();
    }
    if ("tex".equals(mode)) {
      form.enableTexNewLineMode();
    }

    if (!userhtml) {
      form.enablePlainTextMode();
    } else {
      form.enableCheckHTML();
    }

    try {
      msg = form.process();
    } catch (UtilBadHTMLException e) {
      throw new BadInputException(e);
    }

    DupeProtector.getInstance().checkDuplication(request.getRemoteAddr());

    // allocation MSGID
    ResultSet rs = st.executeQuery("select nextval('s_msgid') as msgid");
    rs.next();
    int msgid = rs.getInt("msgid");

    if (group.isImagePostAllowed()) {
      screenshot.copyScreenshot(tmpl, msgid);

      url = "gallery/" + screenshot.getMainFile().getName();
      linktext = "gallery/" + screenshot.getIconFile().getName();
    }

    PreparedStatement pst = db.prepareStatement("INSERT INTO topics (postip, groupid, userid, title, url, moderate, postdate, id, linktext, deleted) VALUES ('" + request.getRemoteAddr() + "',?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f')");
//                pst.setString(1, request.getRemoteAddr());
    pst.setInt(1, group.getId());
    pst.setInt(2, user.getId());
    pst.setString(3, title);
    pst.setString(4, url);
    pst.setInt(5, msgid);
    pst.setString(6, linktext);
    pst.executeUpdate();
    pst.close();

    // insert message text
    PreparedStatement pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message) values (?,?)");
    pstMsgbase.setLong(1, msgid);
    pstMsgbase.setString(2, msg);
    pstMsgbase.executeUpdate();
    pstMsgbase.close();

    String logmessage = "Написана тема " + msgid + " " + LorHttpUtils.getRequestIP(request);
    tmpl.getLogger().notice("add2", logmessage);

    db.commit();

    rs.close();
    st.close();
  }
}
