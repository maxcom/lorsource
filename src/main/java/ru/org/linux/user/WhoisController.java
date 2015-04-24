/*
 * Copyright 1998-2015 Linux.org.ru
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

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.util.bbcode.LorCodeService;
import scala.Option;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

@Controller
public class WhoisController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private UserTagService userTagService;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private UserService userService;

  @Autowired
  private UserStatisticsService userStatisticsService;

  @Autowired
  private UserLogDao userLogDao;

  @Autowired
  private UserLogPrepareService userLogPrepareService;

  @Autowired
  private MemoriesDao memoriesDao;

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private RemarkDao remarkDao;

  @RequestMapping(value="/people/{nick}/profile", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView getInfoNew(@PathVariable String nick, HttpServletRequest request, HttpServletResponse response) throws Exception {
    Template tmpl = Template.getTemplate(request);

    User user = userService.getUser(nick);

    if (user.isBlocked() && !tmpl.isSessionAuthorized()) {
      throw new UserBanedException(user, userDao.getBanInfoClass(user));
    }

    ModelAndView mv = new ModelAndView("whois");
    mv.getModel().put("user", user);
    mv.getModel().put("userInfo", userDao.getUserInfoClass(user));

    mv.getModel().put("userpic", userService.getUserpic(
            user,
            request.isSecure(),
            tmpl.getProf().getAvatarMode(),
            true
    ));

    if (user.isBlocked()) {
      mv.getModel().put("banInfo", userDao.getBanInfoClass(user));
    }

    boolean currentUser = tmpl.isSessionAuthorized() && tmpl.getNick().equals(nick);

    if (!user.isAnonymous()) {
      UserStats userStat = userStatisticsService.getStats(user);
      mv.getModel().put("userStat", userStat);
      mv.getModel().put("watchPresent", memoriesDao.isWatchPresetForUser(user));
      mv.getModel().put("favPresent", memoriesDao.isFavPresetForUser(user));

      if (currentUser || tmpl.isModeratorSession()) {
        mv.getModel().put("hasDrafts", topicDao.hasDrafts(user));
      }
    }

    mv.getModel().put("moderatorOrCurrentUser", currentUser || tmpl.isModeratorSession());
    mv.getModel().put("currentUser", currentUser);

    if (tmpl.isSessionAuthorized() && !currentUser) {
      Set<Integer> ignoreList = ignoreListDao.get(tmpl.getCurrentUser());

      mv.getModel().put("ignored", ignoreList.contains(user.getId()));

      Option<Remark> remark = remarkDao.getRemark(tmpl.getCurrentUser(), user);
      if (remark.isDefined()) {
        mv.getModel().put("remark", remark.get());
      }
    }

    if (tmpl.isSessionAuthorized() && currentUser) {
      mv.getModel().put("hasRemarks", remarkDao.hasRemarks(tmpl.getCurrentUser()));
    }

    String userinfo = userDao.getUserInfo(user);

    if (!Strings.isNullOrEmpty(userinfo)) {
      mv.getModel().put(
              "userInfoText",
              lorCodeService.parseComment(
                      userinfo,
                      request.isSecure(),
                      !topicPermissionService.followAuthorLinks(user)
              )
      );
    }

    mv.addObject("favoriteTags", userTagService.favoritesGet(user));
    if (currentUser || tmpl.isModeratorSession()) {
      mv.addObject("ignoreTags", userTagService.ignoresGet(user));
    }

    if (currentUser || tmpl.isModeratorSession()) {
      List<UserLogItem> logItems = userLogDao.getLogItems(user, tmpl.isModeratorSession());

      if (!logItems.isEmpty()) {
        mv.addObject("userlog", userLogPrepareService.prepare(logItems));
      }
    }

    return mv;
  }

  @RequestMapping(value="/people/{nick}/profile", method = {RequestMethod.GET, RequestMethod.HEAD}, params="wipe")
  public ModelAndView wipe(@PathVariable String nick, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("not moderator");
    }

    User user = userService.getUser(nick);

    user.checkAnonymous();
    user.checkBlocked();

    if (!user.isBlockable()) {
      throw new AccessViolationException("Пользователя нельзя заблокировать");
    }

    ModelAndView mv = new ModelAndView("wipe-user");
    mv.getModel().put("user", user);

    mv.getModel().put("commentCount", userDao.getExactCommentCount(user));

    return mv;
  }

  @RequestMapping("/whois.jsp")
  public View getInfo(@RequestParam("nick") String nick) throws UnsupportedEncodingException{
    return new RedirectView("/people/"+ URLEncoder.encode(nick, "UTF-8")+"/profile");
  }

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(UserBanedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserBanedException(UserBanedException ex) {
    return new ModelAndView("errors/user-banned", "exception", ex);
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleUserNotFound() {
    ModelAndView mav = new ModelAndView("errors/good-penguin");
    mav.addObject("msgTitle", "Ошибка: пользователя не существует");
    mav.addObject("msgHeader", "Пользователя не существует");
    mav.addObject("msgMessage", "");
    return mav;
  }
}
