package ru.org.linux.site;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;

import ru.org.linux.cache.Cache;
import ru.org.linux.logger.Logger;
import ru.org.linux.logger.ServletLogger;
import ru.org.linux.logger.SimpleFileLogger;
import ru.org.linux.site.cli.mkdefprofile;
import ru.org.linux.storage.StorageException;
import ru.org.linux.storage.StorageNotFoundException;
import ru.org.linux.util.*;

public class Template {
  private final Properties cookies;
  private String style;
  private boolean searchMode = false;
  private boolean debugMode = false;
  private Map userProfile;
  private final Map defaultProfile;
  private boolean usingDefaultProfile;
  private String profileName;
  private ProfileHashtable profileHashtable;
  private static Properties properties = null;
  private boolean mainPage;
  private final ServletParameterParser parameters;
  private static Logger logger;
  private final Config config;
  private final HttpSession session;
  private final Date startDate = new Date();
  private final String requestString;

  private static final Cache cache;
  public static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("ru"));
  public static final int WARNING_EXEC_TIME = 15000;

  static {
    cache = new Cache();
  }

  static {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Template(HttpServletRequest request, ServletConfig Config, HttpServletResponse response)
      throws ClassNotFoundException, IOException, SQLException, StorageException {
    this(request, Config, response, false);
  }

  private static synchronized void initServletLogger(ServletContext context) {
    if (logger == null) {
      logger = new ServletLogger(context);
    }
  }

  public String getSecret() {
    return config.getProperties().getProperty("Secret");
  }

  private static synchronized void initProperties(String configFileName) throws IOException {
    File propFile = new File(configFileName);
    if (properties == null) {
      Properties tmp = new Properties();
      tmp.load(new FileInputStream(propFile));
      properties = tmp;
      logger.notice("template", "loaded config file");
      cache.clear();
      logger.close();
      logger = new SimpleFileLogger(properties.getProperty("Logfile"));
    }
  }

  public Template(HttpServletRequest request, ServletConfig config, HttpServletResponse response, boolean isErrorPage)
      throws ClassNotFoundException, IOException, SQLException, StorageException {
    initServletLogger(config.getServletContext());

    requestString = request.getRequestURI() + '?' + request.getQueryString();

    if (request.getParameter("debug") != null) {
      debugMode = true;
    }

    mainPage = false;

    parameters = new ServletParameterParser(request);

    // read properties
    String configFileName = config.getServletContext().getInitParameter("config");
    if (configFileName==null) {
      throw new RuntimeException("Server misconfigured: config file name not defined");
    }

    initProperties(configFileName);

    this.config = new Config(properties);

    // search detector
    searchMode = StringUtil.isSearchEnguine(request.getHeader("User-Agent"));

    // read profiles
    cookies = LorHttpUtils.getCookies(request.getCookies());
    // TODO static initializaion???
    defaultProfile = mkdefprofile.getDefaultProfile();

    usingDefaultProfile = true;

    String profile;

    if (isErrorPage) {
      profile = cookies.getProperty("profile");
    } else {
      profile = getProfile(request);
    }

    if (profile != null && ("".equals(profile) || "anonymous".equals(profile))) {
      profile = null;
    }

    if (isSearchMode()) {
      profile = "_search";
    }
//		if (DebugMode) profile="_debug";

    if (profile != null) {
      usingDefaultProfile = false;
      profileName = profile;

      try {
        userProfile = readProfile(profile);
      } catch (IOException e) {
        getLogger().error("template", e.toString()+": "+StringUtil.getStackTrace(e));        
        userProfile = new Hashtable();
      } catch (StorageNotFoundException e) {
        userProfile = new Hashtable();
      }
    } else {
      userProfile = new Hashtable();
    }

    if (profile != null) {
      userProfile.put("ProfileName", profile);
    }

    userProfile.put("DebugMode", Boolean.valueOf(debugMode));
    userProfile.put("Storage", this.config.getStorage());

    profileHashtable = new ProfileHashtable(defaultProfile, userProfile);

    style = getStyle(getProf().getStringProperty("style"));
// style fixup hack
    if (getCookie("StyleCookie") == null ||
      !getCookie("StyleCookie").equals(getStyle())) {
      Cookie style = new Cookie("StyleCookie", getStyle());
      style.setMaxAge(60 * 60 * 24 * 31 * 12);
      style.setPath("/");
      if (!isSearchMode()) {
        response.addCookie(style);
      }
    }

    userProfile.put("style", style);

    /* restore password */
    session = request.getSession();
    if (getCookie("password") != null &&
      session != null &&
      profile != null &&
      (session.getAttribute("login") == null ||
        !((Boolean) session.getAttribute("login")).booleanValue())) {

      try {
        Connection db = getConnection("user-cookie-auth");
        User user = new User(db, profile);

        if (user.getMD5(getSecret()).equals(getCookie("password")) && !user.isBlocked()) {
          session.putValue("login", Boolean.TRUE);
          session.putValue("nick", profile);
          session.putValue("moderator", Boolean.valueOf(user.canModerate()));
          User.updateUserLastlogin(db, profile, new Date()); // update user `lastlogin` time in DB
        }
      } catch (UserNotFoundException ex) {
        getLogger().error("template", "Can't restore password for user: " + profile + " - " + ex.toString());
      } finally {
        this.config.SQLclose();
      }
    }

    if (!isAnonymousProfile() && !isSessionAuthorized(session) && !isErrorPage) {
      logger.notice("template", "redirecting " + request.getRequestURI() + " to " + replaceProfile(request, null) + " because not logged in");
      response.setHeader("Location", replaceProfile(request, null));
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      Cookie prof = new Cookie("profile", "");
      prof.setMaxAge(60 * 60 * 24 * 31 * 12);
      prof.setPath("/");
      if (!isSearchMode()) {
        response.addCookie(prof);
      }
      return;
    }

    String profileCookie = getCookie("profile");
    if (profileCookie != null && "".equals(profileCookie)) {
      profileCookie = null;
    }

    /* redirects */
    if (usingDefaultProfile) {
      if (profileCookie != null) {
        response.setHeader("Location", replaceProfile(request, getCookie("profile")));
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        logger.notice("template", "redirecting " + request.getRequestURI() + " to " + replaceProfile(request, getCookie("profile")));
      }
    } else if (profileCookie != null &&
      profileCookie.equals(profileName)) {
      /* update profile */
      Cookie prof = new Cookie("profile", profileName);
      prof.setMaxAge(60 * 60 * 24 * 31 * 12);
      prof.setPath("/");
      if (!isSearchMode()) {
        response.addCookie(prof);
      }
    } else if (profileCookie != null &&
      !profileCookie.equals(profileName)) {
      /* redirect to users page */
      response.setHeader("Location", replaceProfile(request, getCookie("profile")));
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      logger.notice("template", "redirecting " + request.getRequestURI() + " to " + replaceProfile(request, getCookie("profile")));
    }

    if (isSessionAuthorized(session)) {
      response.addHeader("Cache-Control", "private");
    }
  }

  private String getStyle(String style) {
    if (!"black".equals(style) &&
        !"white".equals(style) &&
        !"white2".equals(style) &&
        !"blue".equals(style) &&
        !"blackbeta".equals(style)) {
      return (String) defaultProfile.get("style");
    }

    return style;
  }

  public void setMainPage() {
    mainPage = true;
  }

  public boolean isMainPage() {
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

  private static final RE profileRE;
  static {
    try {
      profileRE = new RE("^/profile/([a-zA-Z0-9_-]+)/");
    } catch (REException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getProfile(HttpServletRequest request) {
    String path = request.getRequestURI();

    REMatch found = profileRE.getMatch(path);
    if (found != null) {
      return path.substring(found.getSubStartIndex(1), found.getSubEndIndex(1));
    } else {
      return null;
    }
  }

  public String replaceProfile(HttpServletRequest request, String profile) {
    String path = request.getRequestURI();

    REMatch found = profileRE.getMatch(path);
    if (found != null) {
      String begin = path.substring(0, found.getSubStartIndex(1));
      String end = path.substring(found.getSubEndIndex(1));

      if (profile == null) {
        if (request.getQueryString() != null) {
          return end + '?' + request.getQueryString();
        } else {
          return end;
        }
      } else {
        if (request.getQueryString() != null) {
          return begin + profile + end + '?' + request.getQueryString();
        } else {
          return begin + profile + end;
        }
      }
    } else {
      if (request.getQueryString() != null) {
        return getRedirectUrl(profile) + path.substring(1) + '?' + request.getQueryString();
      } else {
        return getRedirectUrl(profile) + path.substring(1);
      }
    }
  }

  public String getProfileName() {
    return profileName;
  }

  private Map readProfile(String name) throws ClassNotFoundException, IOException, StorageException {
    InputStream df = config.getStorage().getReadStream("profile", name);
    ObjectInputStream dof = new ObjectInputStream(df);
    Map profile = (Map) dof.readObject();
    dof.close();
    df.close();

    return profile;
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
    userProfile.put("system.timestamp", new Long(new Date().getTime()));
    userProfile.remove("Storage");

    OutputStream df = config.getStorage().getWriteStream("profile", name);
    ObjectOutputStream dof = new ObjectOutputStream(df);
    dof.writeObject(userProfile);
    dof.close();
    df.close();
    userProfile.put("Storage", config.getStorage());
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

  public boolean isSearchMode() {
    return searchMode;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public String head() {
    return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n<html lang=ru>\n<head>\n";
  }

  public ProfileHashtable getProf() {
    return profileHashtable;
  }

  public String DocumentHeader() throws IOException, StorageException {
    StringBuffer out = new StringBuffer();
    out.append("<link rel=\"search\" title=\"Search L.O.R.\" href=\"search.php\">\n");
    out.append("<link rel=\"top\" title=\"Linux.org.ru\" href=\"index.jsp\">\n");

    // form submit on ctrl-enter js
    out.append("<script src=\"/js/ctrlenter.js\" language=\"javascript\" type=\"text/javascript\">;</script>\n"); 

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

    if (!isSearchMode()) { // hidden counters
      if (isMainPage()) {
        out.append(config.getStorage().readMessage("buttons", "top100-main-hidden"));
        out.append(config.getStorage().readMessage("buttons", "toplist-main-hidden"));
      } else {
        out.append(config.getStorage().readMessage("buttons", "toplist-hidden"));
      }
    }

    return out.toString();
  }

  public String DocumentFooter(boolean closeHtml) throws IOException, StorageException {
    StringBuffer out = new StringBuffer();

    if (isSearchMode()) {
      out.append("<p><i><a href=\"").append(getMainUrl()).append("\">").append(getMainUrl()).append("</a></i>");
    } else {
      out.append("<p><i><a href=\"").append(getRedirectUrl()).append("\">").append(getMainUrl()).append("</a></i>");
    }

    if (!isMainPage()) {
      out.append("<div align=center><iframe src=\"dw.jsp?width=468&amp;height=60&amp;main=0\" width=\"468\" height=\"60\" scrolling=\"no\" frameborder=\"0\"></iframe></div>");
    }

    if (!isSearchMode()) { // counters / buttons
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
    }

    // Google analytics
    out.append("<script src=\"http://www.google-analytics.com/urchin.js\" type=\"text/javascript\"></script><script type=\"text/javascript\">_uacct = \"UA-1826606-1\";urchinTracker(); </script>\n");

    if (closeHtml) {
      out.append("</body></html>");
    }

    Date currentDate = new Date();
    long millis = currentDate.getTime() - startDate.getTime();

    if (millis>WARNING_EXEC_TIME) {
      logger.notice("template", "execTime="+millis/1000+" seconds (dbWait="+config.getDbWaitTime()/1000+" seconds): "+requestString);
    }

    return out.toString();
  }

  public String DocumentFooter() throws IOException, StorageException {
    return DocumentFooter(true);
  }

  public boolean isUsingDefaultProfile() {
    return usingDefaultProfile;
  }

  public String getMainUrl() {
    return config.getProperties().getProperty("MainUrl");
  }

  public String getRedirectUrl() {
    if (usingDefaultProfile || searchMode) {
      return getMainUrl();
    } else {
      return getMainUrl() + "profile/" + profileName + '/';
    }
  }

  public String getRedirectUrl(String prof) {
    if (prof == null) {
      return getMainUrl();
    }

    if (searchMode) {
      return getMainUrl();
    }

    return getMainUrl() + "profile/" + prof + '/';
  }

  public ServletParameterParser getParameters() {
    return parameters;
  }

  public Config getObjectConfig() {
    return config;
  }

  public Cache getCache() {
    return cache;
  }

  public Logger getLogger() {
    return logger;
  }

  public static boolean isSessionAuthorized(HttpSession session) {
    return session != null && session.getAttribute("login") != null && ((Boolean) session.getAttribute("login")).booleanValue();
  }

  public boolean isModeratorSession() {
    if (!isSessionAuthorized(session)) {
      return false;
    }

    return ((Boolean) session.getValue("moderator")).booleanValue();
  }

  public boolean isAnonymousProfile() {
    if (isUsingDefaultProfile()) {
      return true;
    }

    return profileName.startsWith("_");
  }
}
