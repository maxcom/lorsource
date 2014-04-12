/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserEventController {
  @Autowired
  private ReplyFeedView feedView;
  @Autowired
  private UserDao userDao;
  @Autowired
  private UserEventService userEventService;

  @ModelAttribute("filterValues")
  public static List<UserEventFilterEnum> getFilter() {
    return Arrays.asList(UserEventFilterEnum.values());
  }

  @RequestMapping(value="/notifications", method = RequestMethod.POST)
  public RedirectView resetNotifications(
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    User currentUser = tmpl.getCurrentUser();

    userEventService.resetUnreadReplies(currentUser);

    RedirectView view = new RedirectView("/notifications");

    view.setExposeModelAttributes(false);

    return view;
  }

  /**
   * Показывает уведомления для текущего пользоваетля
   *
   * @param request    запрос
   * @param response   ответ
   * @param offset     смещение
   * @return вьюшку
   */
  @RequestMapping(value="/notifications", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showNotifications(
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value = "filter", defaultValue="all") String filter,
    @RequestParam(value = "offset", defaultValue = "0") int offset
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    Map<String, Object> params = new HashMap<>();
    UserEventFilterEnum eventFilter = UserEventFilterEnum.fromNameOrDefault(filter);
    params.put("filter", eventFilter.getName());

    User currentUser = tmpl.getCurrentUser();
    String nick = currentUser.getNick();

    params.put("nick", nick);
    if (eventFilter != UserEventFilterEnum.ALL) {
      params.put("addition_query", "&filter=" + eventFilter.getName());
    } else {
      params.put("addition_query", "");
    }

    if (offset < 0) {
      offset = 0;
    }

    boolean firstPage = offset == 0;
    int topics = tmpl.getProf().getTopics();

    if (topics > 200) {
      topics = 200;
    }

    params.put("firstPage", firstPage);
    params.put("topics", topics);
    params.put("offset", offset);

    params.put("disable_event_header", true);

    /* define timestamps for caching */
    long time = System.currentTimeMillis();
    int delay = firstPage ? 90 : 60 * 60;
    response.setDateHeader("Expires", time + 1000 * delay);
    params.put("unreadCount", currentUser.getUnreadEvents());
    params.put("isMyNotifications", true);

    response.addHeader("Cache-Control", "no-cache");
    List<UserEvent> list = userEventService.getRepliesForUser(currentUser, true, topics, offset, eventFilter);
    List<PreparedUserEvent> prepared = userEventService.prepare(list, false, request.isSecure());

    params.put("enableReset", true);

    params.put("topicsList", prepared);
    params.put("hasMore", list.size() == topics);

    return new ModelAndView("show-replies", params);
  }

  @RequestMapping(value = "/show-replies.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showReplies(
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value = "nick", required = false) String nick,
    @RequestParam(value = "offset", defaultValue = "0") int offset
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    boolean feedRequested = request.getParameterMap().containsKey("output");

    if (nick == null) {
      if (tmpl.isSessionAuthorized()) {
        return new ModelAndView(new RedirectView("/notifications"));
      }
      throw new AccessViolationException("not authorized");
    } else {
      User.checkNick(nick);
      if (!tmpl.isSessionAuthorized() && !feedRequested) {
        throw new AccessViolationException("not authorized");
      }
      if (tmpl.isSessionAuthorized() && nick.equals(tmpl.getCurrentUser().getNick()) && !feedRequested) {
        return new ModelAndView(new RedirectView("/notifications"));
      }
      if (!feedRequested && !tmpl.isModeratorSession()) {
        throw new AccessViolationException("нельзя смотреть чужие уведомления");
      }
    }

    Map<String, Object> params = new HashMap<>();
    params.put("nick", nick);

    if (offset < 0) {
      offset = 0;
    }

    boolean firstPage = offset == 0;
    int topics = tmpl.getProf().getTopics();
    if (feedRequested) {
      topics = 50;
    }

    if (topics > 200) {
      topics = 200;
    }

    params.put("firstPage", firstPage);
    params.put("topics", topics);
    params.put("offset", offset);

    /* define timestamps for caching */
    long time = System.currentTimeMillis();
    int delay = firstPage ? 90 : 60 * 60;
    response.setDateHeader("Expires", time + 1000 * delay);

    User user = userDao.getUser(nick);

    boolean showPrivate = tmpl.isModeratorSession();

    User currentUser = tmpl.getCurrentUser();
    params.put("currentUser", currentUser);

    if (currentUser != null && currentUser.getId() == user.getId()) {
      showPrivate = true;
      params.put("unreadCount", user.getUnreadEvents());
      response.addHeader("Cache-Control", "no-cache");
    }

    List<UserEvent> list = userEventService.getRepliesForUser(user, showPrivate, topics, offset, UserEventFilterEnum.ALL);
    List<PreparedUserEvent> prepared = userEventService.prepare(list, feedRequested, request.isSecure());

    params.put("isMyNotifications", false);
    params.put("topicsList", prepared);
    params.put("hasMore", list.size() == topics);

    ModelAndView result = new ModelAndView("show-replies", params);

    if (feedRequested) {
      result.addObject("feed-type", "rss");
      if ("atom".equals(request.getParameter("output"))) {
        result.addObject("feed-type", "atom");
      }
      result.setView(feedView);
    }
    return result;
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
