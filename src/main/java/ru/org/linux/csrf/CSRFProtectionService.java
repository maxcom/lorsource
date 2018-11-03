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

package ru.org.linux.csrf;

import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;

public class CSRFProtectionService {
  private static final Logger logger = LoggerFactory.getLogger(CSRFProtectionService.class);

  public static final String CSRF_COOKIE = "CSRF_TOKEN";
  public static final String CSRF_ATTRIBUTE = "csrfToken";
  private static final int TWO_YEARS = 60 * 60 * 24 * 31 * 24;
  public static final String CSRF_INPUT_NAME = "csrf";

  public static void generateCSRFCookie(HttpServletRequest request, HttpServletResponse response) {
    SecureRandom random = new SecureRandom();

    byte[] value = new byte[16];
    random.nextBytes(value);

    String token = new String(Base64.encodeBase64(value));

    Cookie cookie = new Cookie(CSRF_COOKIE, token);
    cookie.setMaxAge(TWO_YEARS);
    cookie.setPath("/");
    response.addCookie(cookie);

    request.setAttribute(CSRF_ATTRIBUTE, token);
  }

  /**
   * Check if user is authorized for request
   * @param request
   * @return true when ok, false when not authorized
   */
  public static boolean checkCSRF(HttpServletRequest request) {
    String cookieValue = (String) request.getAttribute(CSRF_ATTRIBUTE);

    if (Strings.isNullOrEmpty(cookieValue)) {
      logger.info("Missing CSRF cookie");
      return false;
    }

    String inputValue = request.getParameter(CSRF_INPUT_NAME);

    if (Strings.isNullOrEmpty(inputValue)) {
      logger.info("Missing CSRF input");
      return false;
    }

    boolean r = inputValue.trim().equals(cookieValue.trim());

    if (!r) {
      logger.info(String.format(
        "Flood protection (CSRF cookie differs: cookie=%s param=%s) ip=%s url=%s",
        cookieValue,
        inputValue,
        request.getRemoteAddr(),
        request.getRequestURI()
      ));
    }

    return r;
  }

  public static void checkCSRF(HttpServletRequest request, Errors errors) {
    if (checkCSRF(request)) {
      return;
    }

    errors.reject(null, "сбой добавления, попробуйте еще раз");
  }
}
