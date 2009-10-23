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
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.handinteractive.mobile.UAgentInfo;

import ru.org.linux.storage.StorageException;
import ru.org.linux.storage.StorageNotFoundException;
import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class Template {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final Properties cookies;
  private String style;
  private String formatMode;
  private Profile userProfile;
  private final Config config;
  private final HttpSession session;

  public final DateFormat dateFormat = DateFormats.createDefault();
  public static final String PROPERTY_MAIN_URL = "MainUrl";

  private final UAgentInfo userAgent;

  public String getSecret() {
    return config.getProperties().getProperty("Secret");
  }

  public Template(HttpServletRequest request, Properties properties, HttpServletResponse response)
      throws ClassNotFoundException, IOException, SQLException, StorageException {
//    request.setCharacterEncoding("koi8-r"); // блядский tomcat
    request.setCharacterEncoding("utf-8"); // блядский tomcat

    userAgent = new UAgentInfo(request.getHeader("User-Agent"), request.getHeader("Accept"));

    boolean debugMode= false;
    if (request.getParameter("debug") != null) {
      debugMode = true;
    }

    // TODO use better initialization

    config = new Config(properties);

    // read profiles
    cookies = LorHttpUtils.getCookies(request.getCookies());

    String profile = getCookie("profile");

    if (profile != null && ("".equals(profile) || "anonymous".equals(profile))) {
      profile = null;
    }

    /* restore password */
    session = request.getSession();

    if (isSessionAuthorized(session)) {
      if (!session.getValue("nick").equals(profile)) {
        profile = (String) session.getValue("nick");

        Cookie prof = new Cookie("profile", profile);
        prof.setMaxAge(60 * 60 * 24 * 31 * 12);
        prof.setPath("/");
        response.addCookie(prof);
      }
    } else if (session==null) {
      profile = null;
    } else {
      if (profile!=null && getCookie("password") != null) {
        Connection db = null;
        try {
          db = LorDataSource.getConnection();
          User user = User.getUser(db, profile);

          if (user.getMD5(getSecret()).equals(getCookie("password")) && !user.isBlocked()) {
            session.putValue("login", Boolean.TRUE);
            session.putValue("nick", profile);
            session.putValue("moderator", user.canModerate());
            session.putValue("corrector", user.canCorrect());
            User.updateUserLastlogin(db, profile, new Date()); // update user `lastlogin` time in DB
            user.acegiSecurityHack(response, session);
          } else {
            profile = null;
          }
        } catch (UserNotFoundException ex) {
          logger.warning("Can't restore password for user: " + profile + " - " + ex.toString());
          profile = null;
        } finally {
          if (db != null) {
            db.close();
          }
        }
      } else {
        profile = null;
      }
    }

    userProfile = new Profile(profile);

    if (profile != null) {
      try {
        userProfile = readProfile(profile);
      } catch (IOException e) {
        logger.info("Bad profile: "+profile);
        logger.fine(e.toString()+": "+StringUtil.getStackTrace(e));
      } catch (StorageNotFoundException e) {
      }
    }

    userProfile.getHashtable().addBoolean("DebugMode", debugMode);

    styleFixup();
    formatModeFixup();

    response.addHeader("Cache-Control", "private");
  }

  private void styleFixup() {
    style = getStyle(getProf().getString("style"));

    userProfile.getHashtable().setString("style", style);
  }

  private static String getStyle(String style) {
    if (!DefaultProfile.isStyle(style)) {
      return (String) Profile.getDefaults().get("style");
    }

    return style;
  }

  private void formatModeFixup() {
    formatMode = getFormatMode(getProf().getString("format.mode"));

    userProfile.getHashtable().setString("format.mode", formatMode);
  }

  private String getFormatMode(String mode) {
    if (!"ntobrq".equals(mode) &&
        !"quot".equals(mode) &&
        !"tex".equals(mode) &&
        !"ntobr".equals(mode) &&
        !"lorcode".equals(mode)) {
      return (String) Profile.getDefaults().get("format.mode");
    }

    return mode;
  }

  public Properties getConfig() {
    return config.getProperties();
  }

  public String getProfileName() {
    return userProfile.getName();
  }

  private Profile readProfile(String name) throws ClassNotFoundException, IOException, StorageException {
    InputStream df = null;
    try {
      df = config.getStorage().getReadStream("profile", name);

      return new Profile(df, name);
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
      df = config.getStorage().getWriteStream("profile", name);
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
    return style;
  }

  public String getFormatMode() {
    return formatMode;
  }

  public ProfileHashtable getProf() {
    return userProfile.getHashtable();
  }

  public boolean getHover() throws UtilException {
    return getProf().getBoolean("hover");
  }

  public boolean isUsingDefaultProfile() {
    return userProfile.isDefault();
  }

  public String getMainUrl() {
    return config.getProperties().getProperty(PROPERTY_MAIN_URL);
  }

  public Config getObjectConfig() {
    return config;
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

    return (Boolean) session.getValue("moderator");
  }

  public boolean isCorrectorSession() {
    if (!isSessionAuthorized(session)) {
      return false;
    }
    return session.getValue("corrector")!=null ? (Boolean) session.getValue("corrector") : false;
  }

  public String getNick() {
    return getNick(session);
  }

  public static String getNick(HttpSession session) {
    if (isSessionAuthorized(session)) {
      return (String) session.getValue("nick");
    } else {
      return null;
    }
  }

  public static Template getTemplate(ServletRequest request) {
    return (Template) request.getAttribute("template");
  }

  public boolean isMobile() {
    if (!style.equals("tango")) {
      return false;
    }

    return userAgent.detectAndroidWebKit();
  }
}
