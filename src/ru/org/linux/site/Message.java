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
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.javabb.bbcode.BBCodeProcessor;

import ru.org.linux.spring.AddMessageForm;
import ru.org.linux.util.*;

public class Message {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final int msgid;
  private final int postscore;
  private final boolean votepoll;
  private final boolean sticky;
  private String linktext;
  private String url;
  private final Tags tags;
  private final String title;
  private final int userid;
  private final int guid;
  private final boolean deleted;
  private final boolean expired;
  private final int commitby;
  private final boolean havelink;
  private final Timestamp postdate;
  private final Timestamp commitDate;
  private final String groupTitle;
  private final Timestamp lastModified;
  private final int sectionid;
  private final int commentCount;
  private final boolean moderate;
  private final String message;
  private final boolean notop;
  private final String userAgent;
  private final String postIP;
  private final boolean lorcode;

  private final Section section;

  public Message(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st=db.createStatement();

    ResultSet rs=st.executeQuery(
        "SELECT " +
            "postdate, topics.id as msgid, users.id as userid, topics.title, " +
            "topics.groupid as guid, topics.url, topics.linktext, user_agents.name as useragent, " +
            "groups.title as gtitle, vote, havelink, section, topics.sticky, topics.postip, " +
            "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
            "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode " +
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

    this.msgid=rs.getInt("msgid");
    postscore =rs.getInt("postscore");
    votepoll=rs.getBoolean("vote");
    sticky=rs.getBoolean("sticky");
    linktext=rs.getString("linktext");
    url=rs.getString("url");
    tags=new Tags(db, msgid);
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
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");
    notop = rs.getBoolean("notop");
    userAgent = rs.getString("useragent");
    postIP = rs.getString("postip");
    lorcode = rs.getBoolean("bbcode");

    rs.close();
    st.close();

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Message(Connection db, ResultSet rs) throws SQLException {
    this.msgid=rs.getInt("msgid");
    postscore =rs.getInt("postscore");
    votepoll=rs.getBoolean("vote");
    sticky=rs.getBoolean("sticky");
    linktext=rs.getString("linktext");
    url=rs.getString("url");
    tags=new Tags(db, msgid);
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
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");
    notop = rs.getBoolean("notop");
    userAgent = rs.getString("useragent");
    postIP = rs.getString("postip");
    lorcode = rs.getBoolean("bbcode");

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Message(Connection db, AddMessageForm form, User user)
      throws  SQLException, UtilException, ScriptErrorException, UserErrorException {
    // Init fields

    userAgent = form.getUserAgent();
    postIP = form.getPostIP();

    guid = form.getGuid();

    Group group = new Group(db, guid);

    linktext = form.getLinktextHTML();
    url = form.getUrl();

    // url check
    if (!group.isImagePostAllowed()) {
      if (url != null && !"".equals(url)) {
        if (linktext == null) {
          throw new BadInputException("указан URL без текста");
        }
        url = URLUtil.fixURL(url);
      }
    }
    // Setting Message fields
    tags = new Tags(Tags.parseTags(form.getTagsHTML()));
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
    commentCount = 0;
    moderate = false;
    notop = false;
    userid = user.getId();
    lorcode = "lorcode".equals(form.getMode());

    message = form.processMessage(group);

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Message(Connection db, Message original, ServletRequest request) throws BadGroupException, SQLException, UtilException, UserErrorException {
    userAgent = original.userAgent;
    postIP = original.postIP;
    guid = original.guid;

    Group group = new Group(db, guid);

    if (request.getParameter("linktext")!=null) {
      linktext = request.getParameter("linktext");
    } else {
      linktext = original.linktext;
    }

    if (request.getParameter("url")!=null) {
      url = request.getParameter("url");
    } else {
      url = original.url;
    }

    if (request.getParameter("tags")!=null) {
      List<String> newTags = Tags.parseTags(request.getParameter("tags"));

      tags = new Tags(newTags);
    } else {
      tags = original.tags;
    }

    // url check
    if (!group.isImagePostAllowed()) {
      if (url != null && !"".equals(url)) {
        if (linktext == null) {
          throw new BadInputException("указан URL без текста");
        }
        url = URLUtil.fixURL(url);
      }
    }

    if (request.getParameter("title")!=null) {
      title = request.getParameter("title");
    } else {
      title = original.title;
    }

    havelink = original.havelink;

    sectionid = group.getSectionId();

    msgid = original.msgid;
    postscore = original.getPostScore();
    votepoll = original.votepoll;
    sticky = original.sticky;
    deleted = original.deleted;
    expired = original.expired;
    commitby = original.commitby;
    postdate = original.postdate;
    commitDate = original.commitDate;
    groupTitle = original.groupTitle;
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = original.commentCount;
    moderate = original.moderate;
    notop = original.notop;
    userid = original.userid;
    lorcode = original.lorcode;

    if (request.getParameter("newmsg")!=null) {
      message = request.getParameter("newmsg");
    } else {
      message = original.message;
    }

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

  public int getCommentCount() {
    return commentCount;
  }

  public int getPostScore() {
    if (commentCount>3000 && postscore < 200  && !sticky) {
      return 200;
    }

    if (commentCount>2000 && postscore < 100  && !sticky) {
      return 100;
    }

    if (commentCount>1000 && postscore < 50 && !sticky) {
      return 50;
    }

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

    int scrollMode = Section.getScrollMode(sectionid);

    switch (scrollMode) {
      case Section.SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.commitdate=(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, sectionid);
        break;

      case Section.SCROLL_GROUP:
        pst = db.prepareStatement("SELECT min(topics.id) as msgid FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id>? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted");
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

      int nextMsgid = rs.getInt("msgid");

      if (rs.wasNull()) {
        return null;
      }

      return new Message(db, nextMsgid);
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      pst.close();
    }
  }

  public Message getPreviousMessage(Connection db) throws SQLException {
    PreparedStatement pst;

    int scrollMode = Section.getScrollMode(sectionid);

    switch (scrollMode) {
      case Section.SCROLL_SECTION:
        pst = db.prepareStatement("SELECT topics.id as msgid FROM topics WHERE topics.commitdate=(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)");
        pst.setTimestamp(1, commitDate);
        pst.setInt(2, sectionid);
        break;

      case Section.SCROLL_GROUP:
        pst = db.prepareStatement("SELECT max(topics.id) as msgid FROM topics, groups, sections WHERE sections.id=groups.section AND topics.id<? AND topics.groupid=? AND topics.groupid=groups.id AND (topics.moderate OR NOT sections.moderate) AND NOT deleted");
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

      int prevMsgid = rs.getInt("msgid");

      if (rs.wasNull()) {
        return null;
      }

      return new Message(db, prevMsgid);
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

  public static int getPageCount(int commentCount, int messages) {
    return (int) Math.ceil(commentCount/((double) messages));
  }

  public boolean isVotePoll() {
    return votepoll;
  }

  public boolean isSticky() {
    return sticky;
  }

  public void updateMessageText(Connection db) throws SQLException {
    PreparedStatement pst = db.prepareStatement("UPDATE msgbase SET message=? WHERE id=?");
    pst.setString(1, message);
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

    Group group = new Group(db, guid);
	
    DupeProtector.getInstance().checkDuplication(request.getRemoteAddr());

    int msgid = allocateMsgid(db);

    if (group.isImagePostAllowed()) {
      ScreenshotProcessor screenshot = new ScreenshotProcessor(previewImagePath);
      screenshot.copyScreenshotFromPreview(tmpl, msgid);

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
    PreparedStatement pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message, bbcode) values (?,?, ?)");
    pstMsgbase.setLong(1, msgid);
    pstMsgbase.setString(2, message);
    pstMsgbase.setBoolean(3, lorcode);
    pstMsgbase.executeUpdate();
    pstMsgbase.close();

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return msgid;
  }

  private int allocateMsgid(Connection db) throws SQLException {
    Statement st = null;
    ResultSet rs = null;

    try {
      st = db.createStatement();
      rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      return rs.getInt("msgid");
    } finally {
      if (rs!=null) {
        rs.close();
      }

      if (st!=null) {
        st.close();
      }
    }
  }

  public void checkCommentsAllowed(Connection db, User user) throws AccessViolationException, SQLException, BadGroupException {
    Group group = new Group(db, guid);

    if (!group.isCommentPostingAllowed(user)) {
      throw new AccessViolationException("В эту группу нельзя добавлять комментарии");
    }

    user.checkBlocked();

    if (deleted) {
      throw new AccessViolationException("Нельзя добавлять комментарии к удаленному сообщению");
    }

    if (expired) {
      throw new AccessViolationException("группа уже устарела");
    }

    int score = getPostScore();

    if (score != 0) {
      if (user.getScore() < score || user.isAnonymous() || (score == -1 && !user.canModerate())) {
        throw new AccessViolationException("Вы не можете добавлять комментарии в эту тему");
      }
    }
  }

  public boolean isEditable(Connection db, User by) throws SQLException, UserNotFoundException {
    if (expired || deleted) {
      return false;
    }

    if (by.canModerate()) {
      if (User.getUserCached(db, userid).canModerate()) {
        return true;
      }

      return section.isPremoderated();
    }

    if (by.canCorrect()) {
      return sectionid==1;
    }

    if (sectionid==1 && by.getId()==userid && !moderate && lorcode) {
      return (new Date().getTime() - postdate.getTime()) < 30*60*1000;
    }

    return false;
  }

  public int getUid() {
    return userid;
  }

  public boolean isNotop() {
    return notop;
  }

  public String getLinkLastmod() {
    String link;

    if (expired) {
      link = "view-message.jsp?msgid="+msgid;
    } else {
      link = "view-message.jsp?msgid="+msgid+"&lastmod="+getLastModified().getTime();
    }

    return link;
  }

  public Tags getTags() {
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

  public String getProcessedMessage(Connection db) throws SQLException {
    if (lorcode) {
      BBCodeProcessor proc = new BBCodeProcessor();
      return proc.preparePostText(db, message);      
    } else {
      return message;
    }
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

  public boolean isLorcode() {
    return lorcode;
  }
}
