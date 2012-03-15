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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.ApplicationController;
import ru.org.linux.site.Template;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

@Controller
public class WhoisController extends ApplicationController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private LorCodeService lorCodeService;

  @RequestMapping("/people/{nick}/profile")
  public ModelAndView getInfoNew(@PathVariable String nick, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    User user = userDao.getUser(nick);

    if (user.isBlocked() && !tmpl.isSessionAuthorized()) {
      throw new UserBanedException(user, userDao.getBanInfoClass(user));
    }

    ModelAndView modelAndView = new ModelAndView("whois");
    modelAndView.addObject("user", user);
    modelAndView.addObject("userInfo", userDao.getUserInfoClass(user));

    if (user.isBlocked()) {
      modelAndView.addObject("banInfo", userDao.getBanInfoClass(user));
    }

    if (!user.isAnonymous()) {
      modelAndView.addObject("userStat", userDao.getUserStatisticsClass(user));
    }

    boolean currentUser = tmpl.isSessionAuthorized() && tmpl.getNick().equals(nick);

    modelAndView.addObject("moderatorOrCurrentUser", currentUser || tmpl.isModeratorSession());
    modelAndView.addObject("currentUser", currentUser);

    if (tmpl.isSessionAuthorized() && !currentUser) {
      Set<Integer> ignoreList = ignoreListDao.get(tmpl.getCurrentUser());

      modelAndView.addObject("ignored", ignoreList.contains(user.getId()));
    }

    String userinfo = userDao.getUserInfo(user);
    modelAndView.addObject("userInfoText", (userinfo == null) ? "" : lorCodeService.parseComment(userinfo, request.isSecure()));

    return render(modelAndView);
  }

  @RequestMapping("/whois.jsp")
  public ModelAndView getInfo(@RequestParam("nick") String nick)
    throws UnsupportedEncodingException {
    return redirect("/people/" + URLEncoder.encode(nick, "UTF-8") + "/profile");
  }

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(UserBanedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserBanedException(UserBanedException ex, HttpServletRequest request, HttpServletResponse response) {
    return render(new ModelAndView("error-user-banned", "exception", ex));
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleUserNotFound(Exception ex, HttpServletRequest request, HttpServletResponse response) {
    ModelAndView modelAndView = new ModelAndView("error-good-penguin");
    modelAndView.addObject("msgTitle", "Ошибка: пользователя не существует");
    modelAndView.addObject("msgHeader", "Пользователя не существует");
    modelAndView.addObject("msgMessage", "");
    return render(modelAndView);
  }
}
