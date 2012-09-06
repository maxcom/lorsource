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

package ru.org.linux.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.user.User;
import ru.org.linux.util.LorHttpUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Properties;

/**
 */
@Component
public class AuthenticationSuccessHandlerImpl extends SimpleUrlAuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
    forWikiManipulation(request, response, authentication);
    CSRFManipulation(request, response);
    super.onAuthenticationSuccess(request, response, authentication);
  }

  private void forWikiManipulation(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    User user = ((UserDetailsImpl) authentication.getCredentials()).getUser();
    HttpSession session = request.getSession();
    user.acegiSecurityHack(response, session);
  }

  private void CSRFManipulation(HttpServletRequest request, HttpServletResponse response) {
    Properties cookies = LorHttpUtils.getCookies(request.getCookies());
    if (cookies.get(CSRFProtectionService.CSRF_COOKIE) == null) {
      CSRFProtectionService.generateCSRFCookie(request, response);
    } else {
      request.setAttribute(CSRFProtectionService.CSRF_ATTRIBUTE, cookies.getProperty(CSRFProtectionService.CSRF_COOKIE).trim());
    }
    response.addHeader("Cache-Control", "private");
  }


}
