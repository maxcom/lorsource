/*
 * Copyright 1998-2010 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.validation.Errors;
import ru.org.linux.spring.AddMessageForm;
import ru.org.linux.spring.AddMessageRequest;
import ru.org.linux.spring.dao.TagDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.*;
import ru.org.linux.util.bbcode.ParserUtil;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Message implements Serializable {
  private static final Log logger = LogFactory.getLog(Message.class);

  private final int msgid;
  private final int postscore;
  private final boolean votepoll;
  private final boolean sticky;
  private String linktext;
  private String url;
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
  private final String groupUrl;
  private final Timestamp lastModified;
  private final int sectionid;
  private final int commentCount;
  private final boolean moderate;
  private final String message;
  private final boolean notop;
  private final int userAgent;
  private final String postIP;
  private final boolean lorcode;
  private final boolean resolved;
  private final int groupCommentsRestriction;
  private final int sectionCommentsRestriction;
  private final boolean minor;

  private static final long serialVersionUID = 807240555706110851L;
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;
  private static final String UTF8 = "UTF-8";

  public Message(ResultSet rs) throws SQLException {
    msgid = rs.getInt("msgid");

    int ps = rs.getInt("postscore");

    if (rs.wasNull()) {
      postscore = POSTSCORE_UNRESTRICTED;
    } else {
      postscore = ps;
    }

    votepoll = rs.getBoolean("vote");
    sticky = rs.getBoolean("sticky");
    linktext = rs.getString("linktext");
    url = rs.getString("url");
    userid = rs.getInt("userid");
    title = StringUtil.makeTitle(rs.getString("title"));
    guid = rs.getInt("guid");
    deleted = rs.getBoolean("deleted");
    expired = !sticky && rs.getBoolean("expired");
    havelink = rs.getBoolean("havelink");
    postdate = rs.getTimestamp("postdate");
    commitDate = rs.getTimestamp("commitdate");
    commitby = rs.getInt("commitby");
    groupTitle = rs.getString("gtitle");
    groupUrl = rs.getString("urlname");
    lastModified = rs.getTimestamp("lastmod");
    sectionid = rs.getInt("section");
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");
    notop = rs.getBoolean("notop");
    userAgent = rs.getInt("ua_id");
    postIP = rs.getString("postip");
    lorcode = rs.getBoolean("bbcode");
    resolved = rs.getBoolean("resolved");
    groupCommentsRestriction = rs.getInt("restrict_comments");
    minor = rs.getBoolean("minor");
    sectionCommentsRestriction = Section.getCommentPostscore(sectionid);
  }

  public Message(AddMessageForm oldForm, AddMessageRequest form, User user, String message)
          throws UtilException {
    // Init fields

    userAgent = 0;
    postIP = oldForm.getPostIP();

    guid = form.getGroup().getId();

    Group group = form.getGroup();

    groupCommentsRestriction = group.getCommentsRestriction();

    if (form.getLinktext()!=null) {
      linktext = HTMLFormatter.htmlSpecialChars(form.getLinktext());
    } else {
      linktext = null;
    }

    url = form.getUrl();

    // url check
    if (!group.isImagePostAllowed()) {
      if (url != null && !"".equals(url)) {
        url = URLUtil.fixURL(url);
      }
    }
    // Setting Message fields
    if (form.getTitle()!=null) {
      title = HTMLFormatter.htmlSpecialChars(form.getTitle());
    } else {
      title = null;
    }

    havelink = form.getUrl() != null && form.getLinktext() != null && !form.getUrl().isEmpty() && !form.getLinktext().isEmpty() && !group.isImagePostAllowed();
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
    groupUrl = "";
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = 0;
    moderate = false;
    notop = false;
    userid = user.getId();
    lorcode = true;
    resolved = false;
    minor = false;

    this.message = message;
    sectionCommentsRestriction = Section.getCommentPostscore(sectionid);
  }

  public Message(Connection db, Message original, ServletRequest request) throws BadGroupException, UtilException, UserErrorException {
    userAgent = original.userAgent;
    postIP = original.postIP;
    guid = original.guid;

    Group group = Group.getGroup(db, guid);
    groupCommentsRestriction = group.getCommentsRestriction();

    if (request.getParameter("linktext") != null) {
      linktext = request.getParameter("linktext");
    } else {
      linktext = original.linktext;
    }

    if (request.getParameter("url") != null) {
      url = request.getParameter("url");
    } else {
      url = original.url;
    }

//    if (request.getParameter("tags")!=null) {
//      List<String> newTags = Tags.parseTags(request.getParameter("tags"));
//
//      tags = new Tags(newTags);
//    } else {
//      tags = original.tags;
//    }

    // url check
    if (!group.isImagePostAllowed()) {
      if (url != null && !"".equals(url)) {
        if (linktext == null) {
          throw new BadInputException("указан URL без текста");
        }
        url = URLUtil.fixURL(url);
      }
    }

    if (request.getParameter("title") != null) {
      title = HTMLFormatter.htmlSpecialChars(request.getParameter("title"));
    } else {
      title = original.title;
    }

    if (request.getParameter("resolve") != null) {
      resolved = "yes".equals(request.getParameter("resolve"));
    } else {
      resolved = original.resolved;
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
    groupUrl = original.groupUrl;
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = original.commentCount;
    moderate = original.moderate;
    notop = original.notop;
    userid = original.userid;
    lorcode = original.lorcode;
    minor = original.minor;

    if (request.getParameter("newmsg") != null) {
      message = request.getParameter("newmsg");
    } else {
      message = original.message;
    }

    sectionCommentsRestriction = Section.getCommentPostscore(sectionid);
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

  public String getGroupTitle() {
    return groupTitle;
  }

  public Timestamp getLastModified() {
    if (lastModified == null) {
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

  private int getCommentCountRestriction() {
    int commentCountPS = POSTSCORE_UNRESTRICTED;

    if (!sticky) {
      if (commentCount > 3000) {
        commentCountPS = 200;
      } else if (commentCount > 2000) {
        commentCountPS = 100;
      } else if (commentCount > 1000) {
        commentCountPS = 50;
      }
    }

    return commentCountPS;
  }

  public int getPostScore() {
    int effective = Math.max(postscore, groupCommentsRestriction);

    effective = Math.max(effective, sectionCommentsRestriction);

    effective = Math.max(effective, getCommentCountRestriction());

    return effective;
  }

  public String getPostScoreInfo() {
    return getPostScoreInfo(getPostScore());
  }

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(500, 500);
      case POSTSCORE_MOD_AUTHOR:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore;
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

      return getMessage(db, nextMsgid);
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

      return getMessage(db, prevMsgid);
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
    return (int) Math.ceil(commentCount / ((double) messages));
  }

  public static int getPageCount(int commentCount, int messages) {
    return (int) Math.ceil(commentCount / ((double) messages));
  }

  public boolean isVotePoll() {
    return votepoll;
  }

  public boolean isSticky() {
    return sticky;
  }

  public boolean updateMessageText(Connection db, User editor, List<String> newTags) throws SQLException {
    SingleConnectionDataSource scds = new SingleConnectionDataSource(db, true);

    PreparedStatement pstGet = db.prepareStatement("SELECT message,title,linktext,url FROM msgbase JOIN topics ON msgbase.id=topics.id WHERE topics.id=? FOR UPDATE");

    pstGet.setInt(1, msgid);
    ResultSet rs = pstGet.executeQuery();
    if (!rs.next()) {
      throw new RuntimeException("Can't fetch previous message text");
    }

    String oldMessage = rs.getString("message");
    String oldTitle = rs.getString("title");
    String oldLinkText = rs.getString("linktext");
    String oldURL = rs.getString("url");

    rs.close();
    pstGet.close();

    List<String> oldTags = TagDao.getMessageTags(db, msgid);

    EditInfoDTO editInfo = new EditInfoDTO();

    editInfo.setMsgid(msgid);
    editInfo.setEditor(editor.getId());

    boolean modified = false;

    SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(scds);

    if (!oldMessage.equals(message)) {
      editInfo.setOldmessage(oldMessage);
      modified = true;

      jdbcTemplate.update(
        "UPDATE msgbase SET message=:message WHERE id=:msgid",
        ImmutableMap.of("message", message, "msgid", msgid)
      );
    }

    if (!oldTitle.equals(title)) {
      modified = true;
      editInfo.setOldtitle(oldTitle);

      jdbcTemplate.update(
        "UPDATE topics SET title=:title WHERE id=:id",
        ImmutableMap.of("title", title, "id", msgid)
      );
    }

    if (!oldLinkText.equals(linktext)) {
      modified = true;
      editInfo.setOldlinktext(oldLinkText);

      jdbcTemplate.update(
        "UPDATE topics SET linktext=:linktext WHERE id=:id",
        ImmutableMap.of("linktext", linktext, "id", msgid)
      );
    }

    if (!oldURL.equals(url)) {
      modified = true;
      editInfo.setOldurl(oldURL);

      jdbcTemplate.update(
        "UPDATE topics SET url=:url WHERE id=:id",
        ImmutableMap.of("url", url, "id", msgid)
      );
    }

    if (newTags != null) {
      boolean modifiedTags = TagDao.updateTags(db, msgid, newTags);

      if (modifiedTags) {
        editInfo.setOldtags(TagDao.toString(oldTags));
        TagDao.updateCounters(db, oldTags, newTags);
        modified = true;
      }
    }

    if (modified) {
      SimpleJdbcInsert insert =
        new SimpleJdbcInsert(scds)
          .withTableName("edit_info")
          .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl");

      insert.execute(new BeanPropertySqlParameterSource(editInfo));
    }

    return modified;
  }

  public String getUrl() {
    return url;
  }

  public String getLinktext() {
    return linktext;
  }

  public int addTopicFromPreview(Connection db, Template tmpl, HttpServletRequest request, String previewImagePath, User user)
    throws SQLException, UtilException, IOException, BadImageException, ScriptErrorException {

    Group group = Group.getGroup(db, guid);

    int msgid = allocateMsgid(db);

    if (group.isImagePostAllowed()) {
      if (previewImagePath == null) {
        throw new ScriptErrorException("previewImagePath==null!?");
      }

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

  private static int allocateMsgid(Connection db) throws SQLException {
    Statement st = null;
    ResultSet rs = null;

    try {
      st = db.createStatement();
      rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      return rs.getInt("msgid");
    } finally {
      if (rs != null) {
        rs.close();
      }

      if (st != null) {
        st.close();
      }
    }
  }

  public boolean isCommentsAllowed(User user) {
    if (user != null && user.isBlocked()) {
      return false;
    }

    if (deleted || expired) {
      return false;
    }

    int score = getPostScore();

    if (score == POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (user == null || user.isAnonymous()) {
      return false;
    }

    if (user.canModerate()) {
      return true;
    }

    if (score == POSTSCORE_REGISTERED_ONLY) {
      return true;
    }

    if (score == POSTSCORE_MODERATORS_ONLY) {
      return false;
    }

    boolean isAuthor = user.getId() == userid;

    if (score == POSTSCORE_MOD_AUTHOR) {
      return isAuthor;
    }

    if (isAuthor) {
      return true;
    } else {
      return user.getScore() >= score;
    }
  }

  public void checkCommentsAllowed(User user, Errors errors) {
    if (deleted) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению");
      return;
    }

    if (expired) {
      errors.reject(null, "Сообщение уже устарело");
      return;
    }

    if (!isCommentsAllowed(user)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
  }

  public int getUid() {
    return userid;
  }

  public boolean isNotop() {
    return notop;
  }

  public String getLinkLastmod() {
    if (expired) {
      return getLink();
    } else {
      return getLink() + "?lastmod=" + getLastModified().getTime();
    }
  }

  public boolean isHaveLink() {
    return havelink;
  }

  public int getId() {
    return msgid;
  }

  public int getUserAgent() {
    return userAgent;
  }

  public String getMessage() {
    return message;
  }

  public String getProcessedMessage(Connection db) {
    return getProcessedMessage(db, false);
  }

  @Deprecated
  public String getProcessedMessage(Connection db, boolean includeCut, String mainUrl) {
    if (lorcode) {
      String okMainUrl;
      // Откусяываем последний слэш у mainUrl если он есть
      if(mainUrl.endsWith("/")){
        okMainUrl = mainUrl.substring(0, mainUrl.length()-1);
      }else{
        okMainUrl = mainUrl;
      }
      return ParserUtil.bb2xhtml(message, includeCut, false, okMainUrl + getLink(), db);
    } else {
      return "<p>" + message;
    }
  }

  public String getProcessedMessage(UserDao userDao, boolean includeCut, String mainUrl) {
    if (lorcode) {
      String okMainUrl;
      // Откусяываем последний слэш у mainUrl если он есть
      if(mainUrl.endsWith("/")){
        okMainUrl = mainUrl.substring(0, mainUrl.length()-1);
      }else{
        okMainUrl = mainUrl;
      }
      return ParserUtil.bb2xhtml(message, includeCut, false, okMainUrl + getLink(), userDao);
    } else {
      return "<p>" + message;
    }
  }

  public String getProcessedMessage(Connection db, boolean includeCut) {
    if (lorcode) {
      return ParserUtil.bb2xhtml(message, includeCut, false, getLink(), db);
    } else {
      return "<p>" + message;
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

  @Deprecated
  public List<EditInfoDTO> loadEditInfo(Connection db)  {
    SingleConnectionDataSource scds = new SingleConnectionDataSource(db, true);

    SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(scds);

    List<EditInfoDTO> list = jdbcTemplate.query(
      "SELECT * FROM edit_info WHERE msgid=? ORDER BY id DESC",
      BeanPropertyRowMapper.newInstance(EditInfoDTO.class),
      msgid
    );

    return ImmutableList.copyOf(list);
  }

  public boolean isResolved() {
    return resolved;
  }

  public void resolveMessage(Connection db, boolean b) throws SQLException {
    PreparedStatement pstMsgbase = db.prepareStatement("UPDATE topics SET resolved=?,lastmod=lastmod+'1 second'::interval WHERE id=?");
    pstMsgbase.setBoolean(1, b);
    pstMsgbase.setInt(2, msgid);
    pstMsgbase.executeUpdate();
  }

  public String getLink() {
    try {
      return Section.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getLinkPage(int page) {
    if (page == 0) {
      return getLink();
    }

    try {
      return Section.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid + "/page" + page;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public void commit(Connection db, User commiter, int bonus) throws SQLException, UserErrorException {
    if (bonus < 0 || bonus > 20) {
      throw new UserErrorException("Неверное значение bonus");
    }

    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement("UPDATE topics SET moderate='t', commitby=?, commitdate='now' WHERE id=?");
      pst.setInt(2, msgid);
      pst.setInt(1, commiter.getId());
      pst.executeUpdate();

      User author;
      try {
        author = User.getUser(db, userid);
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }

      author.changeScore(db, bonus);
    } finally {
      if (pst != null) {
        pst.close();
      }
    }
  }

  public void changeGroup(Connection db, int changeGroupId) throws SQLException {
    Statement st = db.createStatement();
    st.executeUpdate("UPDATE topics SET groupid=" + changeGroupId + " WHERE id=" + msgid);
    /* to recalc counters */
    st.executeUpdate("UPDATE groups SET stat4=stat4+1 WHERE id=" + guid + " or id=" + changeGroupId);
    st.close();
  }

  public boolean isMinor() {
    return minor;
  }

  public static Message getMessage(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery(
      "SELECT " +
        "postdate, topics.id as msgid, userid, topics.title, " +
        "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
        "groups.title as gtitle, urlname, vote, havelink, section, topics.sticky, topics.postip, " +
        "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
        "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode, " +
        "topics.resolved, restrict_comments, minor " +
        "FROM topics " +
        "INNER JOIN groups ON (groups.id=topics.groupid) " +
        "INNER JOIN sections ON (sections.id=groups.section) " +
        "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
        "WHERE topics.id=" + msgid
    );

    if (!rs.next()) {
      throw new MessageNotFoundException(msgid);
    }

    return new Message(rs);
  }

  /**
   * Проверка может ли пользователь удалить топик
   * @param user пользователь удаляющий сообщение
   * @return признак возможности удаления
   */
  public boolean isDeletableByUser(User user) {
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Timestamp hourDeltaTime = new Timestamp(calendar.getTimeInMillis());

    return (postdate.compareTo(hourDeltaTime) >= 0 && userid == user.getId());
  }

  /**
   * Проверка, может ли модератор удалить топик
   * @param user пользователь удаляющий сообщение
   * @param section местоположение топика
   * @return признак возможности удаления
   */
  public boolean isDeletableByModerator(User user, Section section) {
    // TODO убрать от сюда аргумент функции section
    if(!user.canModerate()) {
      return false;
    }
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    Timestamp monthDeltaTime = new Timestamp(calendar.getTimeInMillis());

    boolean ret = false;

    // Если раздел премодерируемый и топик не подтвержден удалять можно
    if(section.isPremoderated() && !moderate) {
      ret = true;
    }

    // Если раздел премодерируемый, топик подтвержден и прошло меньше месяца с подтверждения удалять можно
    if(section.isPremoderated() && moderate && postdate.compareTo(monthDeltaTime) >= 0) {
      ret = true;
    }

    // Если раздел не премодерируем, удалять можно
    if(!section.isPremoderated()) {
      ret = true;
    }

    return ret;
  }
}
