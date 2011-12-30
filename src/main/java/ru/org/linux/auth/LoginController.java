/*
 * Copyright 1998-2010 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.site.BadInputException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.StringUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collections;

@Controller
public class LoginController {
  public static final String ACEGI_COOKIE_NAME = "SPRING_SECURITY_REMEMBER_ME_COOKIE";

  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  private static boolean isAjax(HttpServletRequest request) {
    String header = request.getHeader("X-Requested-With");

    return header != null && "XMLHttpRequest".equals(header);
  }

  @RequestMapping(value = "/login.jsp", method = RequestMethod.GET)
  public ModelAndView loginForm() {
    return new ModelAndView("login-form");
  }

  @RequestMapping(value = "/login.jsp", method = RequestMethod.POST)
  public ModelAndView doLogin(
          HttpServletRequest request,
          HttpServletResponse response,
          @RequestParam(required = false) String activation
  ) throws Exception {
    final Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    boolean ajax = isAjax(request);

    String nick = request.getParameter("nick");

    if (nick == null || "".equals(nick)) {
      return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Не указан nick"));
    }

    if (!StringUtil.checkLoginName(nick)) {
      return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Некорректный nick"));
    }

    final User user = userDao.getUser(nick);

    user.checkAnonymous();

    if (!user.isActivated()) {
      if (activation == null) {
        return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Требуется активация"));
      }

      String regcode = user.getActivationCode(configuration.getSecret());

      if (regcode.equals(activation)) {
        userDao.activateUser(user);

        tmpl.getProf().setHideAdsense(false);
        tmpl.writeProfile(user.getNick());
      } else {
        throw new AccessViolationException("Bad activation code");
      }
    }

    String password = request.getParameter("passwd");
    if (password == null || !user.matchPassword(password)) {
      return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Неверный пароль"));
    }

    if (session == null) {
      throw new BadInputException("не удалось открыть сессию; возможно отсутствует поддержка Cookie");
    }

    createCookies(response, session, user);

    tmpl.performLogin(response, user);

    if (ajax) {
      return new ModelAndView("login-xml", Collections.singletonMap("ok", "welcome"));
    } else {
      return new ModelAndView(new RedirectView("/"));
    }
  }

  @RequestMapping(value = "/logout.jsp", method = RequestMethod.GET)
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

  private void createCookies(HttpServletResponse response, HttpSession session, User user) {
    Cookie cookie = new Cookie("password", user.getMD5(configuration.getSecret()));
    cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
    cookie.setPath("/");
    response.addCookie(cookie);

    Cookie prof = new Cookie("profile", user.getNick());
    prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    prof.setPath("/");
    response.addCookie(prof);

    user.acegiSecurityHack(response, session);
  }
}
