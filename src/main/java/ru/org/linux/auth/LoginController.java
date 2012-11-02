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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.user.UserBanedException;
import ru.org.linux.user.UserDao;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LoginController {

  public static final String ACEGI_COOKIE_NAME = "SPRING_SECURITY_REMEMBER_ME_COOKIE";

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  RememberMeServices rememberMeServices;

  @Autowired
  @Qualifier("authenticationManager")
  private AuthenticationManager authenticationManager;

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
      SecurityContextHolder.getContext().setAuthentication(auth);
      rememberMeServices.loginSuccess(request, response, auth);
      AuthUtil.updateLastLogin(auth, userDao);
      return new LoginStatus(auth.isAuthenticated(), auth.getName());
    } catch (LockedException e) {
      return new LoginStatus(false, "User locked");
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

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(UserBanedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserBanedException(UserBanedException ex) {
    return new ModelAndView("errors/user-banned", "exception", ex);
  }
}
