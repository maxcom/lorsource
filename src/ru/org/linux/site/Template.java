package ru.org.linux.site;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
  private boolean debugMode = false;
  private Profile userProfile;
  private final Config config;
  private final HttpSession session;

  public static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("ru"));
  public static final DateFormat RFC822 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

  public String getSecret() {
    return config.getProperties().getProperty("Secret");
  }

  public Template(HttpServletRequest request, Properties properties, HttpServletResponse response)
      throws ClassNotFoundException, IOException, SQLException, StorageException {
//    request.setCharacterEncoding("koi8-r"); // блядский tomcat
    request.setCharacterEncoding("utf-8"); // блядский tomcat

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
      if (getCookie("password")!=null) {
        if (isAnonymousProfile(profile)) {
          Cookie cookie = new Cookie("password", "");
          cookie.setMaxAge(0);
          cookie.setPath("/");
          response.addCookie(cookie);
        } else {
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
            if (db!=null) {
              db.close();
            }
          }
        }
      } else {
        if (!isAnonymousProfile(profile)) {
          profile = null;
        }
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

    response.addHeader("Cache-Control", "private");
  }

  private void styleFixup() {
    style = getStyle(getProf().getString("style"));

    userProfile.getHashtable().setString("style", style);
  }

  private String getStyle(String style) {
    if (!"black".equals(style) &&
        !"white".equals(style) &&
        !"white2".equals(style) &&
        !"trans".equals(style) &&
        !"blackbeta".equals(style)) {
      return (String) Profile.getDefaults().get("style");
    }

    return style;
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

  public String getHead() {
    return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n<html lang=ru>\n<head>\n";
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
    return config.getProperties().getProperty("MainUrl");
  }

  public Config getObjectConfig() {
    return config;
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

  public static boolean isAnonymousProfile(String name) {
    if (name==null) {
      return true;
    }

    return name.startsWith("_");
  }

  public static String getNick(HttpSession session) {
    if (isSessionAuthorized(session)) {
      return (String) session.getValue("nick");
    } else {
      return null;
    }
  }

  public static Template getTemplate(HttpServletRequest request) {
    return (Template) request.getAttribute("template");
  }
}
