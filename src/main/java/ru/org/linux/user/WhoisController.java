/*
 * Copyright 1998-2022 Linux.org.ru
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
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.Template;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.util.bbcode.LorCodeService;
import scala.Option;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Controller
public class WhoisController {
  private static final Logger logger = LoggerFactory.getLogger(WhoisController.class);

  private final UserDao userDao;

  private final IgnoreListDao ignoreListDao;

  private final LorCodeService lorCodeService;

  private final UserTagService userTagService;

  private final TopicPermissionService topicPermissionService;

  private final UserService userService;

  private final UserStatisticsService userStatisticsService;

  private final UserLogDao userLogDao;

  private final UserLogPrepareService userLogPrepareService;

  private final MemoriesDao memoriesDao;

  private final TopicDao topicDao;

  private final RemarkDao remarkDao;

  public WhoisController(UserStatisticsService userStatisticsService, UserDao userDao, IgnoreListDao ignoreListDao,
                         LorCodeService lorCodeService, UserTagService userTagService,
                         TopicPermissionService topicPermissionService, UserService userService,
                         UserLogDao userLogDao, UserLogPrepareService userLogPrepareService, RemarkDao remarkDao,
                         MemoriesDao memoriesDao, TopicDao topicDao) {
    this.userStatisticsService = userStatisticsService;
    this.userDao = userDao;
    this.ignoreListDao = ignoreListDao;
    this.lorCodeService = lorCodeService;
    this.userTagService = userTagService;
    this.topicPermissionService = topicPermissionService;
    this.userService = userService;
    this.userLogDao = userLogDao;
    this.userLogPrepareService = userLogPrepareService;
    this.remarkDao = remarkDao;
    this.memoriesDao = memoriesDao;
    this.topicDao = topicDao;
  }

  @RequestMapping(value="/people/{nick}/profile", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView getInfoNew(@PathVariable String nick) throws Exception {
    Template tmpl = Template.getTemplate();

    User user = userService.getUser(nick);

    if (user.isBlocked() && !tmpl.isSessionAuthorized()) {
      throw new UserBanedException(user, userDao.getBanInfoClass(user));
    }

    if (!user.isActivated() && !tmpl.isModeratorSession()) {
      throw new UserNotFoundException(user.getName());
    }

    ModelAndView mv = new ModelAndView("whois");
    mv.getModel().put("user", user);
    mv.getModel().put("userInfo", userDao.getUserInfoClass(user));

    mv.getModel().put("userpic", userService.getUserpic(user, tmpl.getProf().getAvatarMode(), true));

    if (user.isBlocked()) {
      mv.getModel().put("banInfo", userDao.getBanInfoClass(user));
    }

    // add the isFrozen to simplify controller,
    // and put information about moderator who
    // freezes the user, if frozen
    if (user.isFrozen()) {
      mv.getModel().put("isFrozen", true);
      User freezer = userService.getUserCached(user.getFrozenBy());
      mv.getModel().put("freezer", freezer);
    }

    boolean viewByOwner = tmpl.isSessionAuthorized() && AuthUtil.getNick().equals(nick);

    if (tmpl.isModeratorSession()) {
      mv.getModel().put(
              "otherUsers",
              userDao.getAllByEmail(user.getEmail()).stream().filter(u -> u.getId() != user.getId()).collect(Collectors.toList()));
    }

    if (!user.isAnonymous()) {
      UserStats userStat = userStatisticsService.getStats(user);
      mv.getModel().put("userStat", userStat);
      mv.getModel().put("watchPresent", memoriesDao.isWatchPresetForUser(user));
      mv.getModel().put("favPresent", memoriesDao.isFavPresetForUser(user));

      if (viewByOwner || tmpl.isModeratorSession()) {
        mv.getModel().put("hasDrafts", topicDao.hasDrafts(user));
        mv.getModel().put("invitedUsers", userService.getAllInvitedUsers(user));
      }
    }

    mv.getModel().put("moderatorOrCurrentUser", viewByOwner || tmpl.isModeratorSession());
    mv.getModel().put("viewByOwner", viewByOwner);
    mv.getModel().put("canInvite", viewByOwner && userService.canInvite(user));

    if (tmpl.isSessionAuthorized() && !viewByOwner) {
        Set<Integer> ignoreList = ignoreListDao.getJava(AuthUtil.getCurrentUser());

      mv.getModel().put("ignored", ignoreList.contains(user.getId()));

        Option<Remark> remark = remarkDao.getRemark(AuthUtil.getCurrentUser(), user);
      if (remark.isDefined()) {
        mv.getModel().put("remark", remark.get());
      }
    }

    if (viewByOwner) {
        mv.getModel().put("hasRemarks", remarkDao.hasRemarks(AuthUtil.getCurrentUser()));
      mv.getModel().put("canLoadUserpic", userService.canLoadUserpic(user));
    }

    String userinfo = userDao.getUserInfo(user);

    if (!Strings.isNullOrEmpty(userinfo)) {
      mv.getModel().put(
              "userInfoText",
              lorCodeService.parseComment(
                      userinfo,
                      !topicPermissionService.followAuthorLinks(user)
              )
      );
    }

    mv.addObject("favoriteTags", userTagService.favoritesGet(user));
    if (viewByOwner || tmpl.isModeratorSession()) {
      mv.addObject("ignoreTags", userTagService.ignoresGet(user));
    }

    if (viewByOwner || tmpl.isModeratorSession()) {
      List<UserLogItem> logItems = userLogDao.getLogItems(user, tmpl.isModeratorSession());

      if (!logItems.isEmpty()) {
        mv.addObject("userlog", userLogPrepareService.prepare(logItems));
      }
    }

    return mv;
  }

  @RequestMapping("/whois.jsp")
  public View getInfo(@RequestParam("nick") String nick) {
    return new RedirectView("/people/"+ URLEncoder.encode(nick, StandardCharsets.UTF_8)+"/profile");
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
  public ModelAndView handleUserNotFound(UserNotFoundException ex) {
    logger.debug("User not found", ex);

    ModelAndView mav = new ModelAndView("errors/good-penguin");
    mav.addObject("msgTitle", "Ошибка: пользователя не существует");
    mav.addObject("msgHeader", "Пользователя не существует");
    mav.addObject("msgMessage", "");
    return mav;
  }

  @RequestMapping(value="/people/{nick}/profile", method = {RequestMethod.GET, RequestMethod.HEAD}, params="year-stats")
  @ResponseBody
  public CompletionStage<Map<Object, Object>> yearStats(@PathVariable String nick, HttpServletRequest request) {
    User user = userService.getUser(nick);

    if (!AuthUtil.isModeratorSession()) {
      user.checkBlocked();
    }

    DateTimeZone timezone = (DateTimeZone) request.getAttribute("timezone");

    return userStatisticsService.getYearStats(user, timezone);
  }
}
