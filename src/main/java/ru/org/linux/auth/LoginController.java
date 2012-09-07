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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;
import ru.org.linux.user.UserBanedException;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collections;

@Controller
public class LoginController {
  private static final Log logger = LogFactory.getLog(LoginController.class);

  public static final String ACEGI_COOKIE_NAME = "SPRING_SECURITY_REMEMBER_ME_COOKIE";

  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  @Qualifier("authenticationManager")
  private AuthenticationManager authenticationManager;

  @RequestMapping(value = "/ajax_login_process", method = RequestMethod.POST)
  @ResponseBody
  public LoginStatus loginAjax(@RequestParam("nick") final String username, @RequestParam("passwd") final String password) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
    try {
      UserDetailsImpl details = (UserDetailsImpl)userDetailsService.loadUserByUsername(username);
      token.setDetails(details);
      Authentication auth = authenticationManager.authenticate(token);
      SecurityContextHolder.getContext().setAuthentication(auth);
      return new LoginStatus(auth.isAuthenticated(), auth.getName());
    } catch (UsernameNotFoundException e) {
      return new LoginStatus(false, "Bad credentials");
    } catch (BadCredentialsException e) {
      return new LoginStatus(false, e.getMessage());
    }
  }

  public class LoginStatus {
    private final boolean success;
    private final String username;

    public LoginStatus(boolean success, String username) {
      this.success = success;
      this.username = username;
    }

    public boolean isLoggedIn() {
      return success;
    }

    public String getUsername() {
      return username;
    }
  }

  @RequestMapping(value = "/login.jsp", method = RequestMethod.GET)
  public ModelAndView loginForm() {
    return new ModelAndView("login-form");
  }

  @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView logout(
          HttpServletRequest request,
          HttpSession session,
          HttpServletResponse response,
          @RequestParam(required = false) String sessionId
  ) {
    Template tmpl = Template.getTemplate(request);

    if (tmpl.isSessionAuthorized()) {
      if (sessionId == null || !session.getId().equals(sessionId)) {
        return new ModelAndView("logout");
      }

      session.removeAttribute("login");
      session.removeAttribute("nick");
      session.removeAttribute("moderator");
      session.removeAttribute("ACEGI_SECURITY_CONTEXT"); // if any
      Cookie cookie = new Cookie("password", "");
      cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie.setPath("/");
      response.addCookie(cookie);

      Cookie cookie2 = new Cookie("profile", "");
      cookie2.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie2.setPath("/");
      response.addCookie(cookie2);

      Cookie cookie3 = new Cookie(ACEGI_COOKIE_NAME, "");
      cookie3.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie3.setPath("/wiki");
      response.addCookie(cookie3);

      // отчистка seesion id для wiki
      Cookie cookie4 = new Cookie("JSESSIONID", "");
      cookie4.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie4.setPath("/wiki");
      response.addCookie(cookie4);
    }

    return new ModelAndView(new RedirectView("/"));
  }

  private void createCookies(HttpServletResponse response, HttpServletRequest request, User user) {
    Cookie cookie = new Cookie("password", user.getMD5(configuration.getSecret()));
    cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    response.addCookie(cookie);

    Cookie prof = new Cookie("profile", user.getNick());
    prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    prof.setPath("/");
    response.addCookie(prof);

    user.acegiSecurityHack(response, request.getSession());

    CSRFProtectionService.generateCSRFCookie(request, response);
  }

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(UserBanedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserBanedException(UserBanedException ex) {
    return new ModelAndView("errors/user-banned", "exception", ex);
  }
}
