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

import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Properties;

public class CSRFProtectionService {
  public static final String CSRF_COOKIE = "CSRF_TOKEN";
  private static final int TWO_YEARS = 60 * 60 * 24 * 31 * 24;

  public static void initCookie(Properties cookies, HttpServletResponse response) {
    if (cookies.get(CSRF_COOKIE)==null) {
      SecureRandom random = new SecureRandom();

      byte[] value = new byte[16];
      random.nextBytes(value);

      Cookie cookie = new Cookie(CSRF_COOKIE, new String(Base64.encodeBase64(value)));
      cookie.setMaxAge(TWO_YEARS);
      cookie.setPath("/");
      response.addCookie(cookie);
    }
  }
}
