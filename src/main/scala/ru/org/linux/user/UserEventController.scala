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
package ru.org.linux.user

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.site.Template
import ru.org.linux.spring.StatUpdater

import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

@Controller
class UserEventController(feedView: UserEventFeedView, userService: UserService, userEventService: UserEventService,
                          prepareService: UserEventPrepareService, apiController: UserEventApiController) {

  @ModelAttribute("filterValues")
  def filterValues: util.List[UserEventFilterEnum] = UserEventFilterEnum.values.toSeq.asJava

  @RequestMapping(value = Array("/notifications"), method = Array(RequestMethod.POST))
  def resetNotifications(request: HttpServletRequest, @RequestParam topId: Int): RedirectView = {
    apiController.resetNotifications(request, topId)

    val view = new RedirectView("/notifications")
    view.setExposeModelAttributes(false)
    view
  }

  /**
   * Показывает уведомления для текущего пользоваетля
   *
   * @param request  запрос
   * @param response ответ
   * @param offsetRaw   смещение
   * @return вьюшку
   */
  @RequestMapping(value = Array("/notifications"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def showNotifications(request: HttpServletRequest, response: HttpServletResponse,
                        @RequestParam(value = "filter", defaultValue = "all") filter: String,
                        @RequestParam(value = "offset", defaultValue = "0") offsetRaw: Int): ModelAndView = {
    val tmpl = Template.getTemplate(request)

    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("not authorized")
    }

    val eventFilter = UserEventFilterEnum.fromNameOrDefault(filter)
    val currentUser = tmpl.getCurrentUser
    val nick = currentUser.getNick

    val params = mutable.Map[String, Any]()

    params.put("filter", eventFilter.getName)
    params.put("nick", nick)

    if (eventFilter != UserEventFilterEnum.ALL) {
      params.put("addition_query", s"&filter=${eventFilter.getName}")
    } else {
      params.put("addition_query", "")
    }

    val offset = if (offsetRaw < 0) {
      0
    } else {
      offsetRaw
    }

    val firstPage = offset == 0

    val topics = tmpl.getProf.getTopics

    params.put("firstPage", firstPage)
    params.put("topics", topics)
    params.put("offset", offset)
    params.put("disable_event_header", true)
    params.put("unreadCount", currentUser.getUnreadEvents)
    params.put("isMyNotifications", true)

    response.addHeader("Cache-Control", "no-cache")

    if (currentUser!=null && currentUser.getScore >= 500) {
      val list = userEventService.getUserEvents(currentUser, showPrivate = true, StatUpdater.MAX_EVENTS, 0, eventFilter)

      val prepared = prepareService.prepareGrouped(list)

      if (list.nonEmpty) {
        params.put("enableReset", true)
        params.put("topId", prepared.head.lastId)
      }

      val sliced = prepared.slice(offset, offset + topics).take(topics)

      params.put("topicsList", sliced.asJava)
      params.put("hasMore", sliced.size == topics)
    } else {
      val list = userEventService.getUserEvents(currentUser, showPrivate = true, topics, offset, eventFilter)

      val prepared = prepareService.prepare(list, withText = false)

      if (list.nonEmpty) {
        params.put("enableReset", true)
        params.put("topId", prepared.head.lastId)
      }

      params.put("topicsList", prepared.asJava)
      params.put("hasMore", list.size == topics)
    }

    if (!tmpl.getProf.isOldTracker) {
      new ModelAndView("show-replies-new", params.asJava)
    } else {
      new ModelAndView("show-replies", params.asJava)
    }

  }

  @RequestMapping(value = Array("/show-replies.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def showReplies(request: HttpServletRequest, response: HttpServletResponse,
                  @RequestParam(value = "nick", required = false) nick: String,
                  @RequestParam(value = "offset", defaultValue = "0") offsetRaw: Int): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    val feedRequested = request.getParameterMap.containsKey("output")

    if (nick == null) {
      if (tmpl.isSessionAuthorized) {
        return new ModelAndView(new RedirectView("/notifications"))
      } else {
        throw new AccessViolationException("not authorized")
      }
    } else {
      User.checkNick(nick)
      if (!tmpl.isSessionAuthorized && !feedRequested) {
        throw new AccessViolationException("not authorized")
      }

      if (tmpl.isSessionAuthorized && nick == tmpl.getCurrentUser.getNick && !feedRequested) {
        return new ModelAndView(new RedirectView("/notifications"))
      }

      if (!feedRequested && !tmpl.isModeratorSession) {
        throw new AccessViolationException("нельзя смотреть чужие уведомления")
      }
    }

    val params = mutable.Map[String, Any]()

    params.put("nick", nick)

    val offset = if (offsetRaw < 0) {
      0
    } else {
      offsetRaw
    }

    val firstPage = offset == 0

    val topics = if (feedRequested) {
      200
    } else {
      tmpl.getProf.getTopics
    }

    params.put("firstPage", firstPage)
    params.put("topics", topics)
    params.put("offset", offset)

    val time = System.currentTimeMillis
    val delay = if (firstPage) 90 else 60 * 60
    response.setDateHeader("Expires", time + 1000 * delay)

    val user = userService.getUser(nick)
    var showPrivate = tmpl.isModeratorSession
    val currentUser = tmpl.getCurrentUser

    params.put("currentUser", currentUser)

    if (currentUser != null && currentUser.getId == user.getId) {
      showPrivate = true
      params.put("unreadCount", user.getUnreadEvents)
      response.addHeader("Cache-Control", "no-cache")
    }

    val list = userEventService.getUserEvents(user, showPrivate, topics, offset, UserEventFilterEnum.ALL)
    val prepared = prepareService.prepare(list, feedRequested)
    params.put("isMyNotifications", false)
    params.put("topicsList", prepared.asJava)
    params.put("hasMore", list.size == topics)

    val result = new ModelAndView("show-replies", params.asJava)

    if (feedRequested) {
      result.addObject("feed-type", "rss")
      if ("atom" == request.getParameter("output")) result.addObject("feed-type", "atom")
      result.setView(feedView)
    }

    result
  }

  @ExceptionHandler(Array(classOf[UserNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleUserNotFound: ModelAndView = {
    val mav = new ModelAndView("errors/good-penguin")

    mav.addObject("msgTitle", "Ошибка: пользователя не существует")
    mav.addObject("msgHeader", "Пользователя не существует")
    mav.addObject("msgMessage", "")

    mav
  }
}