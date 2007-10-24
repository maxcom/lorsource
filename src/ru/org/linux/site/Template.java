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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ru.org.linux.storage.StorageException;
import ru.org.linux.storage.StorageNotFoundException;
import ru.org.linux.util.*;

public class Template {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final Properties cookies;
  private String style;
  private boolean debugMode = false;
  private Profile userProfile;
  private static Properties properties = null;
  private boolean mainPage;
  private final ServletParameterParser parameters;
  private final Config config;
  private final HttpSession session;
  private final Date startDate = new Date();
  private final String requestString;

  public static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("ru"));
  public static final DateFormat RFC822 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

  private static final int WARNING_EXEC_TIME = 15000;

  public String getSecret() {
    return config.getProperties().getProperty("Secret");
  }

  private static synchronized void initProperties(ServletContext sc) throws IOException {
    if (properties == null) {
      InputStream is = null;
      try {
        is = sc.getResourceAsStream("/WEB-INF/config.properties");
        if (is==null) {
          is = sc.getResourceAsStream("/WEB-INF/config.properties.dist");
          if (is==null) {
            throw new RuntimeException("Can't find config.properties / config.properties.dist");
          }
        }

        Properties tmp = new Properties();
        tmp.load(is);
        properties = tmp;
        logger.fine("loaded config file");
        MemCachedSettings.setMainUrl(properties.getProperty("MainUrl"));

        FileHandler fh = new FileHandler(properties.getProperty("Logfile")+"j", true);
        fh.setEncoding("koi8-r");
        fh.setFormatter(new SimpleFormatter());
        fh.setLevel(Level.INFO);

        FileHandler fhDebug = new FileHandler(properties.getProperty("Logfile")+"jdebug", true);
        fhDebug.setEncoding("koi8-r");
        fhDebug.setFormatter(new SimpleFormatter());
        fhDebug.setLevel(Level.ALL);

        Logger.getLogger("ru.org.linux").addHandler(fh);
        Logger.getLogger("ru.org.linux").addHandler(fhDebug);
        Logger.getLogger("ru.org.linux").setLevel(Level.FINE);
        logger.info("Applicaton started!");
      } finally {
        if (is!=null) {
          is.close();
        }
      }
    }
  }

  public Template(HttpServletRequest request, ServletConfig config, HttpServletResponse response)
      throws ClassNotFoundException, IOException, SQLException, StorageException {
    request.setCharacterEncoding("koi8-r"); // блядский tomcat

    requestString = request.getRequestURI() + '?' + request.getQueryString();

    if (request.getParameter("debug") != null) {
      debugMode = true;
    }

    mainPage = false;

    parameters = new ServletParameterParser(request);

    initProperties(config.getServletContext());

    this.config = new Config(properties);

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
          try {
            Connection db = getConnection("user-cookie-auth");
            User user = User.getUser(db, profile);

            if (user.getMD5(getSecret()).equals(getCookie("password")) && !user.isBlocked()) {
              session.putValue("login", Boolean.TRUE);
              session.putValue("nick", profile);
              session.putValue("moderator", user.canModerate());
              User.updateUserLastlogin(db, profile, new Date()); // update user `lastlogin` time in DB
            } else {
              profile = null;
            }
          } catch (UserNotFoundException ex) {
            logger.warning("Can't restore password for user: " + profile + " - " + ex.toString());
            profile = null;
          } finally {
            this.config.SQLclose();
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
        logger.severe(e.toString()+": "+StringUtil.getStackTrace(e));
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
        !"blackbeta".equals(style)) {
      return (String) Profile.getDefaults().get("style");
    }

    return style;
  }

  public void setMainPage() {
    mainPage = true;
  }

  private boolean isMainPage() {
    return mainPage;
  }

  public Connection getConnection(String user)
    throws SQLException {
    return config.getConnection(user);
  }

  public Connection getConnectionWhois() throws SQLException {
    return config.getConnectionWhois();
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

  public String getCookie(String key) {
    return cookies.getProperty(key);
  }

  public String getCookie(String key, String def) {
    return cookies.getProperty(key, def);
  }

  public String getStyle() {
    return style;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public String head() {
    return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n<html lang=ru>\n<head>\n";
  }

  public ProfileHashtable getProf() {
    return userProfile.getHashtable();
  }

  public String DocumentHeader() throws IOException, StorageException, UtilException {
    StringBuffer out = new StringBuffer();
    out.append("<LINK REL=STYLESHEET TYPE=\"text/css\" HREF=\"/common.css\" TITLE=\"Normal\">");
    out.append("<link rel=\"search\" title=\"Search L.O.R.\" href=\"/search.jsp\">\n");
    out.append("<link rel=\"top\" title=\"Linux.org.ru\" href=\"/\">\n");

    // form submit on ctrl-enter js
    out.append("<script src=\"/js/ctrlenter.js\" type=\"text/javascript\">;</script>\n");

    if (getProf().getBoolean("hover")) {
      out.append("<LINK REL=STYLESHEET TYPE=\"text/css\" HREF=\"/").append(getStyle()).append("/hover.css\" TITLE=\"Normal\">");
    }

    out.append("<base href=").append(HTMLFormatter.htmlSpecialChars(getMainUrl())).append(">");

    if ("black".equals(style)) {
      if (isMainPage()) {
        out.append(FileUtils.readfile(config.getHTMLPathPrefix() + style + "/head-main.html"));
//        out.append("<div align=center>");
//	out.append("<a href=\"http://www.centerpress.ru/shop/computer_press/linuxformat/lxf-2007/ref_102196\"><img src=\"http://www.linux.org.ru/adv/linuxformat/lxf2007.gif\"></a>");
        // banners
//        out.append("</div>");
        out.append(FileUtils.readfile(config.getHTMLPathPrefix() + style + "/head-main2.html"));
      } else {
        out.append(FileUtils.readfile(config.getHTMLPathPrefix() + style + "/head.html"));
      }
    } else {
      if (isMainPage()) {
        out.append(FileUtils.readfile(config.getHTMLPathPrefix() + style + "/head-main.html"));
      } else {
        out.append(FileUtils.readfile(config.getHTMLPathPrefix() + style + "/head.html"));
      }
    }

    if (isMainPage()) {
      out.append(config.getStorage().readMessage("buttons", "top100-main-hidden"));
      out.append(config.getStorage().readMessage("buttons", "toplist-main-hidden"));
    } else {
      out.append(config.getStorage().readMessage("buttons", "toplist-hidden"));
    }

    return out.toString();
  }

  public String DocumentFooter(boolean closeHtml) throws IOException, StorageException {
    StringBuffer out = new StringBuffer();

    out.append("<p><i><a href=\"").append(getMainUrl()).append("\">").append(getMainUrl()).append("</a></i>");

    if (!isMainPage()) {
      out.append("<div align=center><iframe src=\"dw.jsp?width=728&amp;height=90&amp;main=0\" width=\"728\" height=\"90\" scrolling=\"no\" frameborder=\"0\"></iframe></div>");
    }

    out.append("<p><div align=center>");
    if (isMainPage()) {
      out.append(config.getStorage().readMessage("buttons", "top100-main-button"));
      out.append(config.getStorage().readMessage("buttons", "toplist-main-button"));
//				out.append(config.getStorage().readMessage("buttons", "spylog-main-button"));
    } else {
      out.append(config.getStorage().readMessage("buttons", "toplist-button"));
//				out.append(config.getStorage().readMessage("buttons", "spylog-button"));
    }
    out.append("</div>");

    // Google analytics
    out.append("<script src=\"http://www.google-analytics.com/urchin.js\" type=\"text/javascript\">\n" +
        "</script>\n" +
        "<script type=\"text/javascript\">\n" +
        "_uacct = \"UA-2184304-1\";\n" +
        "urchinTracker();\n" +
        "</script>\n");

    if (closeHtml) {
      out.append("</body></html>");
    }

    Date currentDate = new Date();
    long millis = currentDate.getTime() - startDate.getTime();

    if (millis>WARNING_EXEC_TIME) {
      logger.info("execTime="+millis/1000+" seconds (dbWait="+config.getDbWaitTime()/1000+" seconds): "+requestString);
    }

    return out.toString();
  }

  public String DocumentFooter() throws IOException, StorageException {
    return DocumentFooter(true);
  }

  public boolean isUsingDefaultProfile() {
    return userProfile.isDefault();
  }

  public String getMainUrl() {
    return config.getProperties().getProperty("MainUrl");
  }

  public ServletParameterParser getParameters() {
    return parameters;
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
}
