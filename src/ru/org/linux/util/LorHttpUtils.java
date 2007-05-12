package ru.org.linux.util;

import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public final class LorHttpUtils {
  private LorHttpUtils() {
  }

  public static Properties getCookies(Cookie[] cookies) {
    Properties c = new Properties();

    if (cookies == null) {
      return c;
    }

    for (int i = 0; i < cookies.length; i++) {
      String n = cookies[i].getName();
      if (n != null) {
        c.put(n, cookies[i].getValue());
      }
    }

    return c;
  }

  public static String getRequestIP(HttpServletRequest request) {
    String logmessage = "ip:" + request.getRemoteAddr();
    if (request.getHeader("X-Forwarded-For") != null) {
      logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
    }

    return logmessage;
  }
}
