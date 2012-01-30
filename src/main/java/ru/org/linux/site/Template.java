/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;
import ru.org.linux.user.*;
import ru.org.linux.storage.StorageException;
import ru.org.linux.storage.StorageNotFoundException;
import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.util.StringUtil;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Properties;

public final class Template {
  private static final Log logger = LogFactory.getLog(Template.class);

  private final Properties cookies;
  private final Profile userProfile;
  private final Configuration configuration;
  private final HttpSession session;

  private final UserDao userDao;

  private User currentUser = null;

  public final DateFormat dateFormat = DateFormats.createDefault();

  private final Storage storage;

  public Template(
          HttpServletRequest request,
          HttpServletResponse response,
          UserDao userDao,
          Configuration configuration
  )
      throws IOException, StorageException {
    request.setCharacterEncoding("utf-8"); // блядский tomcat

    this.userDao = userDao;

    this.configuration = configuration;

    storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");

    // read profiles
    cookies = LorHttpUtils.getCookies(request.getCookies());

    /* restore password */
    session = request.getSession();

    if (isSessionAuthorized(session)) {
      initCurrentUser(userDao, false);
    } else if (session != null) {
      String profileCookie = getCookie("profile");

      if (profileCookie != null && !profileCookie.isEmpty() &&
          !"anonymous".equals(profileCookie) && getCookie("password") != null) {

        try {
          User user = userDao.getUser(profileCookie);

          if (user.getMD5(configuration.getSecret()).equals(getCookie("password")) && !user.isBlocked()) {
            performLogin(response, user);
          }
        } catch (UserNotFoundException ex) {
          logger.warn("Can't restore password for user: " + profileCookie, ex);
        }
      }
    }

    Profile userProfile = new Profile();

    if (getNick() != null) {
      try {
        userProfile = readProfile();
      } catch (IOException e) {
        logger.info("Bad profile for user "+getNick(), e);
      } catch (StorageNotFoundException e) {
      } catch (ClassNotFoundException e) {
        logger.info("Bad profile for user "+getNick(), e);
      }
    }

    this.userProfile = userProfile;

    response.addHeader("Cache-Control", "private");
  }

  public void performLogin(HttpServletResponse response, User user) {
    session.setAttribute("login", Boolean.TRUE);
    session.setAttribute("nick", user.getNick());
    userDao.updateLastlogin(user);
    user.acegiSecurityHack(response, session);
    currentUser = user;
  }

  public String getProfileName() {
    return getNick();
  }

  private Profile readProfile() throws ClassNotFoundException, IOException, StorageException {
    InputStream df = null;
    try {
      df = storage.getReadStream("profile", getNick());

      return new Profile(df);
    } finally {
      if (df!=null) {
        df.close();
      }
    }
  }

  public void writeProfile(String name) throws IOException, AccessViolationException, StorageException {
    if (name.charAt(0) == '_') {
      throw new AccessViolationException("нельзя менять специальный профиль");
    }

    if (!StringUtil.checkLoginName(name)) {
      throw new AccessViolationException("некорректное имя пользователя");
    }

    if ("anonymous".equals(name)) {
      throw new AccessViolationException("нельзя менять профиль по умолчанию");
    }

    OutputStream df = null;
    try {
      df = storage.getWriteStream("profile", name);
      userProfile.write(df);
    } finally {
      if (df!=null) {
        df.close();
      }
    }
  }

  private String getCookie(String key) {
    return cookies.getProperty(key);
  }

  public String getStyle() {
    User user = getCurrentUser();
    if(user == null) {
      return "tango"; // TODO move to properties?
    } else {
      return user.getStyle();
    }
  }

  public String getFormatMode() {
    return userProfile.getProperties().getFormatMode();
  }

  public ProfileProperties getProf() {
    return userProfile.getProperties();
  }

  public boolean isUsingDefaultProfile() {
    return userProfile.isDefault();
  }

  public String getMainUrl() {
    return configuration.getMainUrl();
  }

  public String getMainUrlNoSlash() {
    return configuration.getMainUrl().replaceFirst("/$", "");
  }

  public String getSecureMainUrl() {
    return configuration.getMainUrl().replaceFirst("http", "https");
  }

  public Configuration getConfig() {
    return configuration;
  }

  public boolean isSessionAuthorized() {
    return isSessionAuthorized(session);
  }

  public static boolean isSessionAuthorized(HttpSession session) {
    return session != null && session.getAttribute("login") != null && (Boolean) session.getAttribute("login");
  }

  public boolean isModeratorSession() {
    if (!isSessionAuthorized(session)) {
      return false;
    }

    return currentUser.isModerator();
  }

  public boolean isCorrectorSession() {
    if (!isSessionAuthorized(session)) {
      return false;
    }

    return currentUser.isCorrector();
  }

  /**
   * Get current authorized users nick
   * @return nick or null if not authorized
   */
  public String getNick() {
    if (!isSessionAuthorized()) {
      return null;
    } else {
      return currentUser.getNick();
    }
  }

  public static Template getTemplate(ServletRequest request) {
    return (Template) request.getAttribute("template");
  }

  public void updateCurrentUser(UserDao userDao) {
    initCurrentUser(userDao, true);
  }

  private void initCurrentUser(UserDao userDao, boolean forceUpdate) {
    if (!isSessionAuthorized()) {
      return;
    }

    if (currentUser != null && !forceUpdate) {
      return;
    }

    try {
      currentUser = userDao.getUser((String) session.getAttribute("nick"));
    } catch (UserNotFoundException e) {
      throw new RuntimeException("Can't find currentUser!?", e);
    }
  }

  public User getCurrentUser()  {
    if (!isSessionAuthorized()) {
      return null;
    }

    if (currentUser == null) {
      throw new IllegalStateException("currentUser==null!? Please call initCurrentUser() first");
    }

    return currentUser;
  }
}
