package ru.org.linux.site;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import ru.org.linux.spring.AddMessageForm;
import ru.org.linux.util.*;

public class Message {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private int msgid;
  private int postscore;
  private boolean votepoll;
  private boolean sticky;
  private boolean preview;
  private String linktext;
  private String url;
  private String tags;
  private String title;
  private final int userid;
  private int guid;
  private boolean deleted;
  private boolean expired;
  private int commitby;
  private boolean havelink;
  private Timestamp postdate;
  private Timestamp commitDate;
  private final String groupTitle;
  private Timestamp lastModified;
  private final int sectionid;
  private boolean comment;
  private final int commentCount;
  private final boolean moderate;
  private final String message;
  private final boolean notop;
  private final String userAgent;
  private final String postIP;

  private final Section section;

  public Message(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st=db.createStatement();

    ResultSet rs=st.executeQuery(
        "SELECT " +
            "postdate, topics.id as msgid, users.id as userid, topics.title, sections.comment, " +
            "topics.groupid as guid, topics.url, topics.linktext, user_agents.name as useragent, " +
            "groups.title as gtitle, vote, havelink, section, topics.sticky, topics.postip, " +
            "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
            "commitdate, topics.stat1, postscore, topics.moderate, message, notop " +
            "FROM topics " +
            "INNER JOIN users ON (users.id=topics.userid) " +
            "INNER JOIN groups ON (groups.id=topics.groupid) " +
            "INNER JOIN sections ON (sections.id=groups.section) " +
            "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
            "LEFT JOIN user_agents ON (user_agents.id=topics.ua_id) " +
            "WHERE topics.id="+msgid
    );
    if (!rs.next()) {
      throw new MessageNotFoundException(msgid);
    }

    preview =false;
    this.msgid=rs.getInt("msgid");
    postscore =rs.getInt("postscore");
    votepoll=rs.getBoolean("vote");
    sticky=rs.getBoolean("sticky");
    linktext=rs.getString("linktext");
    url=rs.getString("url");
    tags=Tags.getPlainTags(db,msgid);
    userid = rs.getInt("userid");
    title=StringUtil.makeTitle(rs.getString("title"));
    guid=rs.getInt("guid");
    deleted=rs.getBoolean("deleted");
    expired= !sticky && rs.getBoolean("expired");
    havelink=rs.getBoolean("havelink");
    postdate=rs.getTimestamp("postdate");
    commitDate=rs.getTimestamp("commitdate");
    commitby = rs.getInt("commitby");
    groupTitle = rs.getString("gtitle");
    lastModified = rs.getTimestamp("lastmod");
    sectionid =rs.getInt("section");
    comment=rs.getBoolean("comment");
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");
    notop = rs.getBoolean("notop");
    userAgent = rs.getString("useragent");
    postIP = rs.getString("postip");

    rs.close();
    st.close();

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Message(Connection db, AddMessageForm form, User user)
      throws BadInputException, SQLException, UtilException, ScriptErrorException {
    // Init fields

    userAgent = form.getUserAgent();
    postIP = form.getPostIP();
    preview = form.isPreview();

    guid = form.getGuid();

    Group group = new Group(db, guid);

    // url check
    if (!group.isImagePostAllowed()) {
      if (url != null && !"".equals(url)) {
        if (linktext == null) {
          if (!preview) {
            throw new BadInputException("указан URL без текста");
          }
        }
        url = URLUtil.fixURL(url);
      }
    }
    // Setting Message fields
    linktext = form.getLinktextHTML();
    url = form.getUrl();
    tags = form.getTagsHTML();
    title = form.getTitleHTML();
    havelink = form.getUrl() != null && form.getLinktext() != null && form.getUrl().length() > 0 && form.getLinktext().length() > 0 && !group.isImagePostAllowed();
    sectionid = group.getSectionId();
    // Defaults
    msgid = 0;
    postscore = 0;
    votepoll = false;
    sticky = false;
    deleted = false;
    expired = false;
    commitby = 0;
    postdate = new Timestamp(System.currentTimeMillis());
    commitDate = null;
    groupTitle = "";
    lastModified = new Timestamp(System.currentTimeMillis());
    comment = false;
    commentCount = 0;
    moderate = false;
    notop = false;
    userid = user.getId();

    message = form.processMessage(group);

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean isExpired() {
    return expired;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String getTitle() {
    return title;
  }

  public String getSectionTitle() {
    return section.getName();
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public Timestamp getLastModified() {
    if (lastModified==null) {
      return new Timestamp(0);
    }

    return lastModified;
  }

  public int getGroupId() {
    return guid;
  }

  public int getSectionId() {
    return sectionid;
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
      case -1:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      default:
        return "<b>Ограничение на отправку комментариев</b>: score="+postscore;
    }
  }

  public Message getNextMessage(Connection db) throws SQLException {
    PreparedStatement pst;

    int scrollMode = Section.getScrollMode(getSectionId());

    switch (scrollMode) {
      case Section.SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics, groups WHERE topics.groupid=groups.id AND topics.commitdate=(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, sectionid);
        break;

      case Section.SCROLL_GROUP:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.id=(SELECT min(topics.id) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id>? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setInt(1, msgid);
        pst.setInt(2, guid);
        break;

      case Section.SCROLL_NOSCROLL:
      default:
        return null;
    }

    try {
      ResultSet rs = pst.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return new Message(db, rs.getInt("msgid"));
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      pst.close();
    }
  }

  public Message getPreviousMessage(Connection db) throws SQLException {
    PreparedStatement pst;

    int scrollMode = Section.getScrollMode(getSectionId());

    switch (scrollMode) {
      case Section.SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics, groups WHERE topics.groupid=groups.id AND topics.commitdate=(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, sectionid);
        break;

      case Section.SCROLL_GROUP:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.id=(SELECT max(topics.id) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id<? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setInt(1, msgid);
        pst.setInt(2, guid);
        break;

      case Section.SCROLL_NOSCROLL:
      default:
        return null;
    }

    try {
      ResultSet rs = pst.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return new Message(db, rs.getInt("msgid"));
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
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

  public boolean isVotePoll() {
    return votepoll;
  }

  public boolean isSticky() {
    return sticky;
  }

  public boolean isPreview() {
    return preview;
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

  public int addTopicFromPreview(Connection db, Template tmpl, HttpServletRequest request, String previewImagePath, User user)
      throws SQLException, UtilException, IOException, BadImageException, InterruptedException,  DuplicationException, BadGroupException {
    ScreenshotProcessor screenshot = null;

    Group group = new Group(db, guid);
	
    if (group.isImagePostAllowed()) {
      screenshot = new ScreenshotProcessor(previewImagePath);
    }

    Statement st = db.createStatement();

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

    PreparedStatement pst = db.prepareStatement("INSERT INTO topics (postip, groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id) VALUES ('" + request.getRemoteAddr() + "',?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f', create_user_agent(?))");
//                pst.setString(1, request.getRemoteAddr());
    pst.setInt(1, group.getId());
    pst.setInt(2, user.getId());
    pst.setString(3, title);
    pst.setString(4, url);
    pst.setInt(5, msgid);
    pst.setString(6, linktext);
    pst.setString(7, request.getHeader("User-Agent"));
    pst.executeUpdate();
    pst.close();

    // insert message text
    PreparedStatement pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message) values (?,?)");
    pstMsgbase.setLong(1, msgid);
    pstMsgbase.setString(2, message);
    pstMsgbase.executeUpdate();
    pstMsgbase.close();

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    rs.close();
    st.close();
    
    return msgid;
  }

  public void checkPostAllowed(User user, boolean moderator) throws AccessViolationException {
    if (isDeleted()) {
      throw new AccessViolationException("Нельзя добавлять комментарии к удаленному сообщению");
    }

    if (!isCommentEnabled()) {
      throw new AccessViolationException("В эту группу нельзя добавлять комментарии");
    }

    if (isExpired()) {
      throw new AccessViolationException("группа уже устарела");
    }

    if (postscore != 0) {
      if (user.getScore() < postscore || user.isAnonymous() || (postscore == -1 && !moderator)) {
        throw new AccessViolationException("Вы не можете добавлять комментарии в эту тему");
      }
    }
  }

  public boolean isEditable(Connection db, User by) throws SQLException, UserNotFoundException {
    if (!by.canModerate() && !by.canCorrect()) {
      return false;
    }

    if (isExpired() || isDeleted()) {
      return false;
    }

    if (User.getUser(db, userid).canModerate()) {
      return true;
    }
    
    if (sectionid==1 && by.canCorrect()) {
      return true;
    }
    
    if (sectionid!=1 && by.canCorrect()) {
      return false;
    }
    
    return section.isPremoderated();
  }

  public int getUid() {
    return userid;
  }

  public boolean isNotop() {
    return notop;
  }

  public String getLinkLastmod() {
    String link;

    if (isExpired()) {
      link = "view-message.jsp?msgid="+msgid;
    } else {
      link = "view-message.jsp?msgid="+msgid+"&lastmod="+getLastModified().getTime();
    }

    return link;
  }

  public String getPlainTags() {
    return tags;
  }

  public boolean isHaveLink() {
    return havelink;
  }

  public int getId() {
    return msgid;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public Section getSection() {
    return section;
  }

  public String getMessage() {
    return message;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public String getPostIP() {
    return postIP;
  }

  public int getCommitby() {
    return commitby;
  }

  public Timestamp getCommitDate() {
    return commitDate;
  }
}
