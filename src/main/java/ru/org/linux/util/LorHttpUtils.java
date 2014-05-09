/*
 * Copyright 1998-2014 Linux.org.ru
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

    for (Cookie cooky : cookies) {
      String n = cooky.getName();
      if (n != null) {
        c.put(n, cooky.getValue());
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
