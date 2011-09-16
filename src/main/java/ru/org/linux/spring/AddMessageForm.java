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

package ru.org.linux.spring;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;
import ru.org.linux.site.*;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.UtilException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

// TODO: move content to AddMessageRequest
public class AddMessageForm {
  private static final Log logger = LogFactory.getLog(AddMessageForm.class);

  private final boolean preview;
  private String sessionId = null;
  private boolean noinfo = false;
  private String password = null;
  private String nick = null;
  private String image = "";
  private String captchaResponse = "";
  private String mode = "";
  private String tags = null;
  private final String postIP;
  private String previewImagePath = null;
  private final ImmutableList<String> pollList;
  public static final int MAX_MESSAGE_LENGTH_ANONYMOUS = 4096;
  public static final int MAX_MESSAGE_LENGTH = 16384;
  public static final int MAX_TITLE_LENGTH = 255;
  public static final int MAX_URL_LENGTH = 255;
  private boolean multiSelect = false;

  public boolean isPreview() {
    return preview;
  }

  public String getSessionId() {
    return sessionId;
  }

  public boolean getNoinfo() {
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

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getTags() {
    return tags;
  }

  public String getTagsHTML() {
    return tags == null ? "" : StringUtils.strip(tags);
  }

  public AddMessageForm(HttpServletRequest request, Template tmpl) throws IOException, ScriptErrorException {
    postIP = request.getRemoteAddr();

    noinfo = "1".equals(request.getParameter("noinfo"));
    sessionId = request.getParameter("session");
    preview = request.getParameter("preview") != null;
    if (!"GET".equals(request.getMethod())) {
      captchaResponse = request.getParameter("j_captcha_response");
      nick = request.getParameter("nick");
      password = request.getParameter("password");
      mode = request.getParameter("mode");
    }

    tags = request.getParameter("tags");

    if (request instanceof MultipartHttpServletRequest) {
      MultipartFile multipartFile = ((MultipartRequest) request).getFile("image");
      if (multipartFile != null && !multipartFile.isEmpty()) {
        File uploadedFile = File.createTempFile("preview", "", new File(tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/"));
        image = uploadedFile.getPath();
        if ((uploadedFile.canWrite() || uploadedFile.createNewFile())) {
          try {
            logger.debug("Transfering upload to: " + image);
            multipartFile.transferTo(uploadedFile);
          } catch (Exception e) {
            throw new ScriptErrorException("Failed to write uploaded file", e);
          }
        } else {
          logger.info("Bad target file name: " + image);
        }
      }
    }

    List<String> pollList = new ArrayList<String>();

    for (int i = 0; i < Poll.MAX_POLL_SIZE; i++) {
      String poll = request.getParameter("var" + i);

      if (poll != null) {
        pollList.add(poll);
      }
    }

    if (pollList.isEmpty()) {
      this.pollList = null;
    } else {
      this.pollList = ImmutableList.copyOf(pollList);
    }

    if (request.getParameter("multiSelect")!=null) {
      multiSelect = true;
    }
  }

  /**
   *
   * @param session
   * @param tmpl
   * @return pair <icon, image> or null
   * @throws IOException
   * @throws BadImageException
   * @throws UtilException
   * @throws InterruptedException
   */
  public List<String> processUpload(HttpSession session, Template tmpl) throws IOException, BadImageException, UtilException, InterruptedException {
    File uploadedFile = null;

    if (image != null && !"".equals(image)) {
      uploadedFile = new File(image);
    } else if (sessionId != null && !"".equals(sessionId) && session.getAttribute("image") != null && !"".equals(session.getAttribute("image"))) {
      uploadedFile = new File((String) session.getAttribute("image"));
    }

    if (uploadedFile != null && uploadedFile.isFile() && uploadedFile.canRead()) {
      ScreenshotProcessor screenshot = new ScreenshotProcessor(uploadedFile.getAbsolutePath());
      logger.info("SCREEN: " + uploadedFile.getAbsolutePath() + "\nINFO: SCREEN: " + image);
      if (image != null && !"".equals("image")) {
        screenshot.copyScreenshot(tmpl, sessionId);
      }
      previewImagePath = screenshot.getMainFile().getAbsolutePath();
      session.setAttribute("image", screenshot.getMainFile().getAbsolutePath());
      return ImmutableList.of(
              "gallery/preview/" + screenshot.getIconFile().getName(),
              "gallery/preview/" + screenshot.getMainFile().getName()
      );
    }

    return null;
  }

  public User validateAndGetUser(Template tmpl, Connection db) throws UserNotFoundException, BadPasswordException, AccessViolationException, BadInputException {
    User user;

    if (!tmpl.isSessionAuthorized()) {
      if (nick == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = User.getUser(db, nick);
      user.checkPassword(password);
    } else {
      user = tmpl.getCurrentUser();
    }

    user.checkBlocked();
    user.checkAnonymous();

    return user;
  }

  public String getPostIP() {
    return postIP;
  }

  public String getPreviewImagePath() {
    return previewImagePath;
  }

  public ImmutableList<String> getPollList() {
    return pollList;
  }

  public boolean getMultiSelect() {
    return multiSelect;
  }
}
