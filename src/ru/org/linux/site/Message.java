package ru.org.linux.site;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

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

  private final Section section;

  public Message(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st=db.createStatement();

    ResultSet rs=st.executeQuery(
        "SELECT " +
            "postdate, topics.id as msgid, users.id as userid, topics.title, sections.comment, " +
            "topics.groupid as guid, topics.url, topics.linktext, " +
            "groups.title as gtitle, vote, havelink, section, topics.sticky, " +
            "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
            "commitdate, topics.stat1, postscore, topics.moderate, message, notop " +
            "FROM topics, users, groups, sections, msgbase " +
            "WHERE topics.id="+msgid+" AND topics.userid=users.id AND topics.groupid=groups.id " +
            "AND groups.section=sections.id AND topics.id=msgbase.id"
    );
    if (!rs.next()) throw new MessageNotFoundException(msgid);

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

    rs.close();
    st.close();

    try {
      section = new Section(db, sectionid);
    } catch (BadSectionException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Message(Connection db, Template tmpl, HttpSession session, HttpServletRequest request)
      throws BadInputException, SQLException, UtilException, ScriptErrorException, RuntimeException,
      BadPasswordException, AccessViolationException, IOException,  FileUploadException, BadImageException, InterruptedException {
    // Init fields
    String linktext = null;
    String url = null;
    String tags = null;
    String mode = "";
    boolean autourl = true;
    boolean texttype = false;
    String captchaResponse = "";
    String image = "";
    String nick = null;
    String password = null;
    String noinfo = null;
    String sessionId = null;
    String returnUrl = null;
    String title = null;
    String msg = null;
    int guid = 0;
    boolean preview;

    // Check that we have a file upload request
    if (!ServletFileUpload.isMultipartContent(request) || request.getParameter("group") != null) {
      // Load fields from request
      noinfo = request.getParameter("noinfo");
      sessionId = request.getParameter("session");
      preview = request.getParameter("preview") != null;
      if (!request.getMethod().equals("GET")) {
        captchaResponse = request.getParameter("j_captcha_response");
        nick = request.getParameter("nick");
        password = request.getParameter("password");
        mode = request.getParameter("mode");
        autourl = "1".equals(request.getParameter("autourl"));
        texttype = "1".equals(request.getParameter("texttype"));
        title = request.getParameter("title");
        msg = request.getParameter("msg");
      }
      try {
        guid = request.getParameter("group") != null ? Integer.parseInt(request.getParameter("group")) : 0;
      } catch (NumberFormatException e) {
      }
      linktext = request.getParameter("linktext");
      url = request.getParameter("url");
      returnUrl = request.getParameter("return");
      tags = request.getParameter("tags");
    } else {
      // Load fields from multipart request
      File rep = new File(tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/");
      // Create a factory for disk-based file items
      DiskFileItemFactory factory = new DiskFileItemFactory();
      // Set factory constraints
      factory.setSizeThreshold(500000);
      factory.setRepository(rep);
      // Create a new file upload handler
      ServletFileUpload upload = new ServletFileUpload(factory);
      // Set overall request size constraint
      upload.setSizeMax(600000);
      // Parse the request
      List items = upload.parseRequest(request);
      // Process the uploaded items
      Iterator iter = items.iterator();
      // Defaults
      preview = false;
      while (iter.hasNext()) {
        FileItem item = (FileItem) iter.next();
        if (item.isFormField()) {
          String name = item.getFieldName();
          String value = item.getString("UTF-8");
          //System.out.println("\nField: "+name+" => "+value);
          if (name.compareToIgnoreCase("j_captcha_response") == 0) {
            captchaResponse = value;
          } else if (name.compareToIgnoreCase("noinfo") == 0) {
            noinfo = value;
          } else if (name.compareToIgnoreCase("session") == 0) {
            sessionId = value;
          } else if (name.compareToIgnoreCase("preview") == 0) {
            preview = (!(value == null || "".equals(value)));
          } else if (name.compareToIgnoreCase("nick") == 0) {
            nick = value;
          } else if (name.compareToIgnoreCase("password") == 0) {
            password = value;
          } else if (name.compareToIgnoreCase("mode") == 0) {
            mode = value;
          } else if (name.compareToIgnoreCase("autourl") == 0) {
            autourl = Boolean.parseBoolean(value);
          } else if (name.compareToIgnoreCase("textype") == 0) {
            texttype = Boolean.parseBoolean(value);
          } else if (name.compareToIgnoreCase("title") == 0) {
            title = value;
          } else if (name.compareToIgnoreCase("msg") == 0) {
            msg = value;
          } else if (name.compareToIgnoreCase("group") == 0) {
            guid = Integer.parseInt(value);
          } else if (name.compareToIgnoreCase("linktext") == 0) {
            linktext = value;
          } else if (name.compareToIgnoreCase("url") == 0) {
            url = value;
          } else if (name.compareToIgnoreCase("tags") == 0) {
            tags = value;
          } else if (name.compareToIgnoreCase("return") == 0) {
            returnUrl = value;
          }
        } else {
          String fieldName = item.getFieldName();
          String fileName = item.getName();       
          //System.out.print("\nFile: "+fieldName+" => "+fileName);
          if (fieldName.compareToIgnoreCase("image") == 0 && fileName != null && !"".equals(fileName)) {
            image = tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/" + fileName;
            File uploadedFile = new File(image);
            if (uploadedFile != null && (uploadedFile.canWrite() || uploadedFile.createNewFile())) {
              try {
                item.write(uploadedFile);
              } catch (Exception e) {
                throw new ScriptErrorException("Failed to write uploaded file", e);
              }
            } else {
              Logger.getLogger("ru.org.linux").info("Bad target file name: " + image);
            }
          } else {
            Logger.getLogger("ru.org.linux").info("Bad source file name: " + fileName);
          }
        }
      }
    }

    // Save fields as request attributes
    this.preview = preview;
    request.setAttribute("j_captcha_response", captchaResponse);
    request.setAttribute("image", image);
    request.setAttribute("session", sessionId);
    request.setAttribute("preview", preview);
    request.setAttribute("mode", mode);
    request.setAttribute("autourl", autourl);
    request.setAttribute("texttype", texttype);
    request.setAttribute("title", title);
    request.setAttribute("msg", msg);
    request.setAttribute("group", guid);
    request.setAttribute("linktext", linktext);
    request.setAttribute("url", url);
    request.setAttribute("tags", tags);
    request.setAttribute("return", returnUrl);
    request.setAttribute("noinfo", noinfo);
    request.setAttribute("nick", nick);
    request.setAttribute("password", password);
    // If we under get
    if (request.getMethod().equals("GET")) {
      throw new MessageNotFoundException(0);
    }
    if (guid < 1) {
      throw new BadInputException("Bad group id");
    }
    Group group = new Group(db, guid);
    // Posting checks...
    if (!preview) {
      // Flood protection
      if (!session.getId().equals(sessionId)) {
        Logger.getLogger("ru.org.linux").info("Flood protection (session variable differs) " + request.getRemoteAddr());
        Logger.getLogger("ru.org.linux").info("Flood protection (session variable differs) " + session.getId() + " != " + sessionId);
        throw new BadInputException("сбой добавления");
      }
      // Captch
      if (!Template.isSessionAuthorized(session)) {
        CaptchaSingleton.checkCaptcha(session, request);
      }
      // Blocked IP
      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());
    }

    if (group.isImagePostAllowed()) {
      File uploadedFile = null;
      if (image != null && !"".equals(image)) {
        uploadedFile = new File(image);
      } else
      if (sessionId != null && !"".equals(sessionId) && session.getAttribute("image") != null && !"".equals(session.getAttribute("image"))) {
        uploadedFile = new File((String) session.getAttribute("image"));
      }
      if (uploadedFile != null && uploadedFile.isFile() && uploadedFile.canRead()) {
        ScreenshotProcessor screenshot = new ScreenshotProcessor(uploadedFile.getAbsolutePath());
        logger.info("SCREEN: " + uploadedFile.getAbsolutePath() + "\nINFO: SCREEN: " + image);
        if (image != null && !"".equals("image")) {
          screenshot.copyScreenshot(tmpl, sessionId);
        }
        url = "gallery/preview/" + screenshot.getMainFile().getName();
        linktext = "gallery/preview/" + screenshot.getIconFile().getName();
        request.setAttribute("linktext", linktext);
        request.setAttribute("url", url);
        request.setAttribute("image", screenshot.getMainFile().getAbsolutePath());
        session.setAttribute("image", screenshot.getMainFile().getAbsolutePath());
      }
    }
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
    this.linktext = linktext == null ? "" : HTMLFormatter.htmlSpecialChars(linktext);
    this.url = url == null ? "" : HTMLFormatter.htmlSpecialChars(url);
    this.tags = tags == null ? "" : StringUtils.strip(tags);
    this.title = title == null ? "" : HTMLFormatter.htmlSpecialChars(title);
    this.guid = guid;
    havelink = url != null && linktext != null && url.length() > 0 && linktext.length() > 0 && !group.isImagePostAllowed();
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
    // Checks TODO: checks for anonymous
    User user;

    if (!Template.isSessionAuthorized(session)) {
      if (nick == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = User.getUser(db, nick);
      user.checkPassword(password);
    } else {
      user = User.getUser(db, (String) session.getAttribute("nick"));
    }

    user.checkBlocked();

    if (user.isAnonymous()) {
      if (msg.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (msg.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }
    userid = user.getId();

    if (!group.isTopicPostingAllowed(user)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    // Format message
    HTMLFormatter form = new HTMLFormatter(msg);
    int maxlength = 80;
    if (group.getSectionId() == 1) {
      maxlength = 40;
    }
    form.setMaxLength(maxlength);

    if ("pre".equals(mode) && !group.isPreformatAllowed()) {
      throw new AccessViolationException("В группу нельзя добавлять преформатированные сообщения");
    }
    if (("ntobr".equals(mode) || "tex".equals(mode) || "quot".equals(mode)) && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }
    if (texttype && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }

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
    if (!texttype) {
      form.enablePlainTextMode();
    } else {
      form.enableCheckHTML();
    }
    try {
      message = form.process();
    } catch (UtilBadHTMLException e) {
      throw new BadInputException(e);
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

  public String printMessage(Template tmpl, Connection db, boolean showMenu, String user) 
      throws SQLException, IOException, UserNotFoundException, UtilException {
    return printMessage(tmpl, db, showMenu, user, 0);
  }
  
  public String printMessage(Template tmpl, Connection db, boolean showMenu, String user, int highlight)
      throws SQLException, IOException, UserNotFoundException, UtilException {
    StringBuffer out=new StringBuffer();

    User author = User.getUserCached(db, userid);

    out.append("\n\n<!-- ").append(msgid).append(" -->\n");

    if (showMenu) {
      out.append("<div class=title>");

      if (!deleted) {
        out.append("[<a href=\"/view-message.jsp?msgid=").append(msgid).append("\">#</a>]");
      }

//      if (!isExpired() && !isDeleted())
//        out.append("[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Ответить</a>]");

      if (!isDeleted() && (tmpl.isModeratorSession() || author.getNick().equals(user))) {
        out.append("[<a href=\"delete.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      }

      if (!isDeleted() && tmpl.isModeratorSession()) {
        if(votepoll)
	    out.append("[<a href=\"edit-vote.jsp?msgid=").append(msgid).append("\">Править</a>]");
	else
    	    out.append("[<a href=\"edit.jsp?msgid=").append(msgid).append("\">Править</a>]");
        out.append("[<a href=\"setpostscore.jsp?msgid=").append(msgid).append("\">Установить параметры</a>]");
        out.append("[<a href=\"mt.jsp?msgid=").append(msgid).append("\">Перенести</a>]");
				if (sectionid==1) {
					out.append("[<a href=\"mtn.jsp?msgid=").append(msgid).append("\">Группа</a>]");
				}
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

      out.append("&nbsp;</div>");
    }

    out.append("<div class=msg>");

    boolean tbl = false;
    if (section.isImagepost()) {
      out.append("<table><tr><td valign=top align=center>");
      tbl=true;

      try {
        ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+linktext);
        out.append("<a href=\"/").append(url).append("\"><img src=\"/").append(linktext).append("\" ALT=\"").append(title).append("\" ").append(info.getCode()).append(" ></a>");
      } catch (BadImageException e) {
        out.append("<a href=\"/").append(url).append("\">[bad image]</a>");
      } catch (FileNotFoundException e) {
		out.append("<a href=\"/").append(url).append("\">[no image]</a>");
	  }

      out.append("</td><td valign=top>");
    }

    if (!section.isImagepost() && author.getPhoto()!=null) {
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

    out.append("<h1><a name=").append(msgid).append('>').append(title).append("</a></h1>");

//    out.append(storage.readMessage("msgbase", String.valueOf(msgid)));
    if (votepoll) {
      //Render poll
      try {
        int id = Poll.getPollIdByTopic(db, msgid);
        Poll poll = new Poll(db, id);
        out.append(poll.renderPoll(db, tmpl.getConfig(), tmpl.getProf(), highlight));
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(msgid).append("\">Проголосовать</a>");
      } catch (PollNotFoundException e) {
        out.append("[BAD POLL: not found]");
      } catch (BadImageException e) {
        out.append("[BAD POLL: bad image]");
      }
    } else {
      out.append(message);
    }

    if (url!=null && havelink)
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">").append(linktext).append("</a>.");

    if (url!=null && section.isImagepost()) {
      try {
        ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+url);

        out.append("<p><i>").append(info.getWidth()).append('x').append(info.getHeight()).append(", ").append(info.getSizeString()).append("</i>");

        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>.");
      } catch (BadImageException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>.");
      } catch (FileNotFoundException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">[NO IMAGE!] Просмотр</a>.");	  
	  }
    }

    if (sectionid==1) {
      String tagLinks = Tags.getTagLinks(db, msgid);

      if (tagLinks.length()>0) {
        out.append("<p>Метки: <i>");
        out.append(tagLinks);
        out.append("</i>");
      }
    }


    out.append("<div class=sign>");

    out.append(author.getSignature(tmpl.isModeratorSession(), postdate));

    if (commitby!=0) {
      User commiter = User.getUser(db, commitby);

      out.append("<br>");
      out.append(commiter.getCommitInfoLine(postdate, commitDate));
    }

    out.append("</div>");

    if (!deleted && showMenu) {
      out.append("<div class=reply>");
      if (!expired) {
        out.append("[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Ответить на это сообщение</a>] ").append(getPostScoreInfo(postscore));
      }
      out.append("</div>");
    }


    if (tbl) out.append("</td></tr></table>");
    out.append("</div>");

    return out.toString();
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
    if (lastModified==null)
      return new Timestamp(0);

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

      if (!rs.next())
        return null;

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

      if (!rs.next())
        return null;

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

  public static int addTopic(Connection db, Template tmpl, HttpSession session, HttpServletRequest request, Group group) throws SQLException, UserNotFoundException, ServletParameterException, UtilException, IOException, BadImageException, InterruptedException, BadInputException, BadPasswordException, AccessViolationException, DuplicationException {
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
    String vmsg = (String)request.getAttribute("msg");
    if (msg == null && vmsg != null && vmsg.length()>0) msg = vmsg;
    request.setAttribute("msg", null);

    boolean userhtml = new ServletParameterParser(request).getBoolean("texttype");
    boolean autourl = new ServletParameterParser(request).getBoolean("autourl");

    String url = request.getParameter("url");
    if (url != null && "".equals(url)) {
      url = null;
    }

    // checks is over
    User user;

    if (!Template.isSessionAuthorized(session))     {
      if (request.getParameter("nick") == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = User.getUser(db, request.getParameter("nick"));
      user.checkPassword(request.getParameter("password"));
    } else {
      user = User.getUser(db, (String) session.getAttribute("nick"));
    }

    user.checkBlocked();

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

    String mode = new ServletParameterParser(request).getString("mode");

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
    logger.info(logmessage);

    rs.close();
    st.close();
    
    return msgid;
  }

  public int addTopicFromPreview(Connection db, Template tmpl, HttpSession session, HttpServletRequest request) throws SQLException, UserNotFoundException,  UtilException, IOException, BadImageException, InterruptedException, BadInputException, BadPasswordException, AccessViolationException, DuplicationException, BadGroupException {
    if ("".equals(title.trim())) {
      throw new BadInputException("заголовок сообщения не может быть пустым");
    }

    ScreenshotProcessor screenshot = null;

    Group group = new Group(db, guid);
	
    if (group.isImagePostAllowed()) {
      screenshot = new ScreenshotProcessor((String)request.getAttribute("image"));
    }

    User user;

    if (!Template.isSessionAuthorized(session))     {
      if (request.getAttribute("nick") == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = User.getUser(db, (String)request.getAttribute("nick"));
      user.checkPassword((String)request.getAttribute("password"));
    } else {
      user = User.getUser(db, (String) session.getAttribute("nick"));
    }

    user.checkBlocked();

    if (user.isAnonymous()) {
      if (message.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (message.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }

    Statement st = db.createStatement();

    if (!group.isTopicPostingAllowed(user)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
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
    } else {
      if (url != null) {
        url = StringEscapeUtils.unescapeHtml(url);
      }
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
    pstMsgbase.setString(2, message);
    pstMsgbase.executeUpdate();
    pstMsgbase.close();

    String logmessage = "Написана тема " + msgid + " " + LorHttpUtils.getRequestIP(request);
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
    if (!by.canModerate()) {
      return false;
    }

    if (isExpired() || isDeleted()) {
      return false;
    }

    if (User.getUser(db, userid).canModerate()) {
      return true;
    }

    return section.isPremoderated();
  }

  public int getUid() {
    return userid;
  }

  public boolean isNotop() {
    return notop;
  }

  public String getLinkLastmod(boolean encode) {
    String link;

    if (isExpired()) {
      link = "view-message.jsp?msgid="+msgid;
    } else {
      link = "view-message.jsp?msgid="+msgid+"&lastmod="+getLastModified().getTime();
    }

    if (encode) {
      return HTMLFormatter.htmlSpecialChars(link);
    } else {
      return link;
    }
  }

  public String getPlainTags() {
    return tags;
  }

  public boolean containsLink() {
    return havelink;
  }
}
