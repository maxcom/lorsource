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
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.site.{BadInputException, Template}
import ru.org.linux.spring.StatUpdater
import ru.org.linux.util.StringUtil

import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Controller
class UserEventController(feedView: UserEventFeedView, userService: UserService, userEventService: UserEventService,
                          prepareService: UserEventPrepareService, apiController: UserEventApiController) {

  @ModelAttribute("filterValues")
  def filterValues(@RequestAttribute reactionsEnabled: Boolean): util.List[UserEventFilterEnum] =
    UserEventFilterEnum.values.toSeq.filter(r => reactionsEnabled || r != UserEventFilterEnum.REACTION).asJava

  @RequestMapping(value = Array("/notifications"), method = Array(RequestMethod.POST))
  def resetNotifications(@RequestParam topId: Int): RedirectView = {
    apiController.resetNotifications(topId)

    val view = new RedirectView("/notifications")
    view.setExposeModelAttributes(false)
    view
  }

  /**
   * Показывает уведомления для текущего пользователя
   *
   * @param response ответ
   * @param offsetRaw   смещение
   * @return вьюшку
   */
  @RequestMapping(value = Array("/notifications"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def showNotifications(response: HttpServletResponse,
                        @RequestParam(value = "filter", defaultValue = "all") filter: String,
                        @RequestParam(value = "offset", defaultValue = "0") offsetRaw: Int): ModelAndView =
    AuthorizedOnly { currentUser =>
      val tmpl = Template.getTemplate

      val eventFilter = UserEventFilterEnum.fromNameOrDefault(filter)
      val nick = currentUser.user.getNick

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
      params.put("unreadCount", currentUser.user.getUnreadEvents)
      params.put("isMyNotifications", true)

      response.addHeader("Cache-Control", "no-cache")

      val list = userEventService.getUserEvents(currentUser.user, showPrivate = true,
        StatUpdater.MAX_EVENTS, 0, eventFilter)

      val prepared = prepareService.prepareGrouped(list)

      if (list.nonEmpty) {
        params.put("enableReset", true)
        params.put("topId", prepared.view.map(_.lastId).max)
      }

      val sliced = prepared.slice(offset, offset + topics).take(topics)

      params.put("topicsList", sliced.asJava)
      params.put("hasMore", sliced.size == topics)

      if (!tmpl.getProf.isOldTracker) {
        new ModelAndView("show-replies-new", params.asJava)
      } else {
        new ModelAndView("show-replies", params.asJava)
      }
  }

  @RequestMapping(value = Array("/show-replies.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def showReplies(request: HttpServletRequest, response: HttpServletResponse,
                  @RequestParam(value = "nick", required = false) nick: String,
                  @RequestParam(value = "offset", defaultValue = "0") offsetRaw: Int): ModelAndView =
    AuthUtil.AuthorizedOpt { currentUserOpt =>
      val feedRequested = request.getParameterMap.containsKey("output")

      if (nick == null) {
        if (currentUserOpt.isDefined) {
          return new ModelAndView(new RedirectView("/notifications"))
        } else {
          throw new AccessViolationException("not authorized")
        }
      } else {
        if (!StringUtil.checkLoginName(nick)) {
          throw new BadInputException("некорректное имя пользователя")
        }

        if (currentUserOpt.isEmpty && !feedRequested) {
          throw new AccessViolationException("not authorized")
        }

        val viewByOwner = currentUserOpt.exists(_.user.getNick == nick)
        if (viewByOwner && !feedRequested) {
          return new ModelAndView(new RedirectView("/notifications"))
        }

        val viewByModerator = currentUserOpt.exists(_.moderator)
        if (!feedRequested && !viewByModerator) {
          throw new AccessViolationException("нельзя смотреть чужие уведомления")
        }

        val params = mutable.Map[String, Any]()

        params.put("nick", nick)

        val offset = if (offsetRaw < 0) {
          0
        } else {
          offsetRaw
        }

        val firstPage = offset == 0

        val tmpl = Template.getTemplate

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
        val showPrivate = viewByModerator || viewByOwner

        if (viewByOwner) {
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