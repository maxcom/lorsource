/*
 * Copyright 1998-2026 Linux.org.ru
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CSRFProtectionService {
  private static final Logger logger = LoggerFactory.getLogger(CSRFProtectionService.class);

  public static final String CSRF_COOKIE = "CSRF_TOKEN";
  public static final String CSRF_INPUT_NAME = "csrf";

  /**
   * Check if user is authorized for request
   * @param request
   * @return true when ok, false when not authorized
   */
  public static boolean checkCSRF(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, CSRF_COOKIE);
    String cookieValue = (cookie != null) ? cookie.getValue() : null;

    if (Strings.isNullOrEmpty(cookieValue)) {
      logger.info("Missing CSRF cookie");
      return false;
    }

    String inputValue = request.getParameter(CSRF_INPUT_NAME);

    if (Strings.isNullOrEmpty(inputValue)) {
      logger.info("Missing CSRF input");
      return false;
    }

    boolean r = MessageDigest.isEqual(
      cookieValue.trim().getBytes(StandardCharsets.UTF_8),
      inputValue.trim().getBytes(StandardCharsets.UTF_8));

    if (!r) {
      logger.info(String.format(
        "CSRF cookie does not match: cookie.len=%s param.len=%s) ip=%s url=%s",
        cookieValue.length(),
        inputValue.length(),
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
