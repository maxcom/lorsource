/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

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
    ArrayList<String> xff = Collections.list(request.getHeaders(HttpHeaders.X_FORWARDED_FOR));

    if (!xff.isEmpty()) {
      logmessage = logmessage + " XFF:" + Joiner.on(", ").join(xff);
    }

    return logmessage;
  }
}
