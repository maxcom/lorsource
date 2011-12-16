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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.User;
import ru.org.linux.spring.dao.RepliesDao;
import ru.org.linux.spring.dao.UserDao;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ShowRepliesController {
  @Autowired
  private ReplyFeedView feedView;
  @Autowired
  private UserDao userDao;
  @Autowired
  private RepliesDao repliesDao;

  /**
   * Показывает уведомления для текущего пользоваетля
   * @param request запрос
   * @param response ответ
   * @param offset смещение
   * @param forceReset принудительная отсчистка уведомлений
   * @return вьюшку
   * @throws Exception возможны исключительные ситуации :-(
   */
  @RequestMapping("/notifications")
  public ModelAndView showNotifications(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "forceReset", defaultValue = "false") boolean forceReset
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    User currentUser = tmpl.getCurrentUser();
    String nick = currentUser.getNick();

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("nick", nick);
    params.put("forceReset", forceReset);

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
    List<RepliesListItem> list = repliesDao.getRepliesForUser(currentUser, true, topics, offset, false, request.isSecure());
    if ("POST".equalsIgnoreCase(request.getMethod())) {
      userDao.resetUnreadReplies(currentUser);
      tmpl.updateCurrentUser(userDao);
    } else {
      params.put("enableReset", true);
    }
    params.put("topicsList", list);
    params.put("hasMore", list.size()==topics);

    return new ModelAndView("show-replies", params);
  }

  @RequestMapping(value="/show-replies.jsp", method= RequestMethod.GET)
  public ModelAndView showReplies(
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value = "nick", required=false) String nick,
    @RequestParam(value = "offset", defaultValue = "0") int offset
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    boolean feedRequested = request.getParameterMap().containsKey("output");

    if (nick==null) {
      if(tmpl.isSessionAuthorized()) {
        return new ModelAndView(new RedirectView("/notifications"));
      }
      if (!tmpl.isSessionAuthorized()) {
        throw new AccessViolationException("not authorized");
      }
    } else {
      User.checkNick(nick);
      if (!tmpl.isSessionAuthorized() && !feedRequested) {
        throw new AccessViolationException("not authorized");
      }
      if(tmpl.isSessionAuthorized() && nick.equals(tmpl.getCurrentUser().getNick()) && !feedRequested) {
        return new ModelAndView(new RedirectView("/notifications"));
      }
      if(!feedRequested && !tmpl.isModeratorSession()) {
        throw new AccessViolationException("нельзя смотреть чужие уведомления");
      }
    }

    Map<String, Object> params = new HashMap<String, Object>();
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

    List<RepliesListItem> list = repliesDao.getRepliesForUser(user, showPrivate, topics, offset, feedRequested, request.isSecure());

    params.put("isMyNotifications", false);
    params.put("topicsList", list);
    params.put("hasMore", list.size()==topics);

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
}
