/*
 * Copyright 1998-2017 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.user.UserBanedException;
import ru.org.linux.user.UserDao;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LoginController {
  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  private RememberMeServices rememberMeServices;

  @Autowired
  @Qualifier("authenticationManager")
  private AuthenticationManager authenticationManager;

  @RequestMapping(value = "/login_process", method = RequestMethod.POST)
  public ModelAndView loginProcess(
      @RequestParam("nick") final String username,
      @RequestParam("passwd") final String password,
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

    try {
      UserDetailsImpl details = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
      token.setDetails(details);
      Authentication auth = authenticationManager.authenticate(token);
      UserDetailsImpl userDetails = (UserDetailsImpl)auth.getDetails();

      if(!userDetails.getUser().isActivated()) {
        return new ModelAndView(new RedirectView("/login.jsp?error=not_activated"));
      } else {
        SecurityContextHolder.getContext().setAuthentication(auth);
        rememberMeServices.loginSuccess(request, response, auth);
        AuthUtil.updateLastLogin(auth, userDao);

        return new ModelAndView(new RedirectView("/"));
      }
    } catch (LockedException | BadCredentialsException | UsernameNotFoundException e) {
      logger.warn("Login of " + username + " failed; remote IP: "+request.getRemoteAddr()+"; " + e.getMessage());
      return new ModelAndView(new RedirectView("/login.jsp?error=true"));
    }
  }

  @RequestMapping(value = "/logout", method = RequestMethod.POST)
  public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null) {
      new SecurityContextLogoutHandler().logout(request, response, auth);
    }

    Cookie cookie = new Cookie("remember_me", null);
    cookie.setMaxAge(0);
    cookie.setPath("/");
    response.addCookie(cookie);

    return new ModelAndView(new RedirectView("/login.jsp"));
  }

  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public ModelAndView logoutLink() {
    if (AuthUtil.isSessionAuthorized()) {
      return new ModelAndView(new RedirectView("/people/"+AuthUtil.getNick()+"/profile"));
    } else {
      return new ModelAndView(new RedirectView("/login.jsp"));
    }
  }

  @RequestMapping(value = "/ajax_login_process", method = RequestMethod.POST)
  @ResponseBody
  public LoginStatus loginAjax(
      @RequestParam("nick") final String username,
      @RequestParam("passwd") final String password,
      HttpServletRequest request, HttpServletResponse response) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
    try {
      UserDetailsImpl details = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
      token.setDetails(details);
      Authentication auth = authenticationManager.authenticate(token);
      UserDetailsImpl userDetails = (UserDetailsImpl)auth.getDetails();
      if(!userDetails.getUser().isActivated()) {
        return new LoginStatus(false, "User not activated");
      }
      SecurityContextHolder.getContext().setAuthentication(auth);
      rememberMeServices.loginSuccess(request, response, auth);
      AuthUtil.updateLastLogin(auth, userDao);

      return new LoginStatus(auth.isAuthenticated(), auth.getName());
    } catch (LockedException e) {
      logger.warn("Login of " + username + " failed; remote IP: "+request.getRemoteAddr()+"; " + e.getMessage());

      return new LoginStatus(false, "User locked");
    } catch (UsernameNotFoundException e) {
      logger.warn("Login of " + username + " failed; remote IP: "+request.getRemoteAddr()+"; " + e.getMessage());

      return new LoginStatus(false, "Bad credentials");
    } catch (BadCredentialsException e) {
      logger.warn("Login of " + username + " failed; remote IP: "+request.getRemoteAddr()+"; " + e.getMessage());

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
}
