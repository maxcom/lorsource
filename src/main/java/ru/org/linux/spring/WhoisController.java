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

package ru.org.linux.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.HTMLFormatter;

import javax.servlet.ServletRequest;
import java.net.URLEncoder;

@Controller
public class WhoisController {

  private static final Log logger = LogFactory.getLog(VoteController.class);

  @Autowired
  UserDao userDao;

  @RequestMapping("/people/{nick}/profile")
  public ModelAndView getInfoNew(@PathVariable String nick, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    User user = userDao.getUser(nick);

    if (user.isBlocked() && !tmpl.isSessionAuthorized()) {
      throw new UserNotFoundException(nick);
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

    if (tmpl.isSessionAuthorized() && !currentUser) {
      mv.getModel().put("ignoreList", userDao.getIgnoreList(userDao.getUser(tmpl.getCurrentUser().getId())));
    }

    String userinfo = userDao.getUserInfo(user);
    mv.getModel().put("userInfoText", (userinfo == null)?"":HTMLFormatter.nl2br(userinfo));

    return mv;
  }

  @RequestMapping("/whois.jsp")
  public View getInfo(@RequestParam("nick") String nick) {
    return new RedirectView("/people/"+ URLEncoder.encode(nick)+"/profile");
  }
}
