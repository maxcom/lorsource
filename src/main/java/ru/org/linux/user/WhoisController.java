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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Set;

@Controller
public class WhoisController {
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

    ModelAndView mv = new ModelAndView("whois");
    mv.getModel().put("user", user);
    mv.getModel().put("userInfo", userDao.getUserInfoClass(user));

    if (user.isBlocked()) {
      mv.getModel().put("banInfo", userDao.getBanInfoClass(user));
    }

    if (!user.isAnonymous()) {
      mv.getModel().put("userStat", userDao.getUserStatisticsClass(user));
    }

    boolean currentUser = tmpl.isSessionAuthorized() && tmpl.getNick().equals(nick);

    mv.getModel().put("moderatorOrCurrentUser", currentUser || tmpl.isModeratorSession());
    mv.getModel().put("currentUser", currentUser);

    if (tmpl.isSessionAuthorized() && !currentUser) {
      Set<Integer> ignoreList = ignoreListDao.get(tmpl.getCurrentUser());

      mv.getModel().put("ignored", ignoreList.contains(user.getId()));
    }

    String userinfo = userDao.getUserInfo(user);
    mv.getModel().put("userInfoText", (userinfo == null)?"":lorCodeService.parseComment(userinfo, request.isSecure()));

    return mv;
  }

  @RequestMapping("/whois.jsp")
  public View getInfo(@RequestParam("nick") String nick) {
    return new RedirectView("/people/"+ URLEncoder.encode(nick)+"/profile");
  }

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(UserBanedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String handleUserBanedException(UserBanedException ex, HttpServletRequest request, HttpServletResponse response) {
    return "error-user-banned";
  }
}
