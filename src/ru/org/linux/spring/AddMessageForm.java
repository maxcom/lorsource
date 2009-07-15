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

package ru.org.linux.spring;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import ru.org.linux.site.*;
import ru.org.linux.util.*;

public class AddMessageForm {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private boolean preview;
  private int guid = 0;
  private String msg = "";
  private String title = null;
  private String returnUrl = null;
  private String sessionId = null;
  private String noinfo = null;
  private String password = null;
  private String nick = null;
  private String image = "";
  private String captchaResponse = "";
  private boolean autourl = true;
  private String mode = "";
  private String tags = null;
  private String url = null;
  private String linktext = null;
  private final String userAgent;
  private final String postIP;
  private String previewImagePath = null;
  private List<String> pollList;

  public boolean isPreview() {
    return preview;
  }

  public int getGuid() {
    return guid;
  }

  public String getMsg() {
    return msg;
  }

  public String getTitle() {
    return title;
  }

  public String getTitleHTML() {
    return title == null ? "" : HTMLFormatter.htmlSpecialChars(title);
  }

  public String getReturnUrl() {
    return returnUrl;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getNoinfo() {
    return noinfo;
  }

  public String getPassword() {
    return password;
  }

  public String getNick() {
    return nick;
  }

  public String getImage() {
    return image;
  }

  public String getCaptchaResponse() {
    return captchaResponse;
  }

  public boolean isAutourl() {
    return autourl;
  }

  public String getMode() {
    return mode;
  }

  public String getTags() {
    return tags;
  }

  public String getTagsHTML() {
    return tags == null ? "" : StringUtils.strip(tags);
  }

  public String getUrl() {
    return url;
  }

  public String getLinktext() {
    return linktext;
  }

  public String getLinktextHTML() {
    return linktext == null ? "" : HTMLFormatter.htmlSpecialChars(linktext);
  }

  public AddMessageForm(HttpServletRequest request, Template tmpl) throws IOException, ScriptErrorException, ServletParameterException {
    userAgent = request.getHeader("user-agent");
    postIP = request.getRemoteAddr();

    noinfo = request.getParameter("noinfo");
    sessionId = request.getParameter("session");
    preview = request.getParameter("preview") != null;
    if (!"GET".equals(request.getMethod())) {
      captchaResponse = request.getParameter("j_captcha_response");
      nick = request.getParameter("nick");
      password = request.getParameter("password");
      mode = request.getParameter("mode");
      autourl = "1".equals(request.getParameter("autourl"));
      title = request.getParameter("title");
      msg = request.getParameter("msg");
    }

    if (request.getParameter("group") == null) {
      throw new ScriptErrorException("missing group parameter");
    }

    guid = new ServletParameterParser(request).getInt("group");

    linktext = request.getParameter("linktext");
    url = request.getParameter("url");
    returnUrl = request.getParameter("return");
    tags = request.getParameter("tags");

    if (request instanceof MultipartHttpServletRequest) {
      MultipartFile multipartFile = ((MultipartHttpServletRequest) request).getFile("image");
      if (multipartFile != null && !multipartFile.isEmpty()) {
        File uploadedFile = File.createTempFile("preview", "", new File(tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/"));
        image = uploadedFile.getPath();
        if ((uploadedFile.canWrite() || uploadedFile.createNewFile())) {
          try {
            Logger.getLogger("ru.org.linux").fine("Transfering upload to: " + image);
            multipartFile.transferTo(uploadedFile);
          } catch (Exception e) {
            throw new ScriptErrorException("Failed to write uploaded file", e);
          }
        } else {
          Logger.getLogger("ru.org.linux").info("Bad target file name: " + image);
        }
      }
    }

    pollList = new ArrayList<String>();

    for (int i = 0; i < Poll.MAX_POLL_SIZE; i++) {
      String poll = request.getParameter("var" + i);

      if (poll != null) {
        pollList.add(poll);
      }
    }

    if (pollList.isEmpty()) {
      pollList = null;
    }
  }

  public void validate(Group group, User user) throws BadInputException, AccessViolationException {
    if ("".equals(title.trim())) {
      throw new BadInputException("заголовок сообщения не может быть пустым");
    }

    if (guid < 1) {
      throw new BadInputException("Bad group id");
    }

    String message = processMessage(group);

    if (user.isAnonymous()) {
      if (message.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (message.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }

    if (!group.isTopicPostingAllowed(user)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }
  }

  public void processUpload(HttpSession session, Template tmpl) throws IOException, BadImageException, UtilException, InterruptedException {
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
      previewImagePath = screenshot.getMainFile().getAbsolutePath();
      session.setAttribute("image", screenshot.getMainFile().getAbsolutePath());
    }
  }

  public User validateAndGetUser(HttpSession session, Connection db) throws BadInputException, UserNotFoundException, SQLException, BadPasswordException, AccessViolationException {
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
      if (msg!=null && msg.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (msg!=null && msg.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }

    return user;
  }

  public String processMessage(Group group) {
    if (msg == null) {
      return "";
    }

    if ("lorcode".equals(mode)) {
      return msg;
    } else {
      // Format message
      HTMLFormatter formatter = new HTMLFormatter(msg);
      int maxlength = 80;
      if (group.getSectionId() == 1) {
        maxlength = 40;
      }
      formatter.setMaxLength(maxlength);

      if (autourl) {
        formatter.enableUrlHighLightMode();
      }
      if ("ntobrq".equals(mode)) {
        formatter.enableNewLineMode();
      }
      if ("ntobr".equals(mode)) {
        formatter.enableNewLineMode();
      }
      if ("tex".equals(mode)) {
        formatter.enableTexNewLineMode();
      }

      return formatter.process();
    }
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getPostIP() {
    return postIP;
  }

  public String getPreviewImagePath() {
    return previewImagePath;
  }

  public List<String> getPollList() {
    return pollList;
  }
}
