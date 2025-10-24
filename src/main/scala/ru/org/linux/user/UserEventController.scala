/*
 * Copyright 1998-2024 Linux.org.ru
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

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, MaybeAuthorized}
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.site.BadInputException
import ru.org.linux.user.UserEvent.NoReaction
import ru.org.linux.util.StringUtil

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Controller
class UserEventController(feedView: UserEventFeedView, userService: UserService, userEventService: UserEventService,
                          prepareService: UserEventPrepareService, apiController: UserEventApiController) {
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
      val eventFilter = UserEventFilterEnum.fromNameOrDefault(filter)
      val nick = currentUser.user.getNick

      val params = mutable.Map[String, Any]()

      params.put("filterValues", userEventService.getEventTypes(currentUser.user).asJava)

      params.put("filter", eventFilter.getName)
      params.put("nick", nick)

      if (eventFilter != UserEventFilterEnum.ALL) {
        params.put("addition_query", s"&filter=${eventFilter.getName}")
      } else {
        params.put("addition_query", "")
      }

      params.put("link", "/notifications")

      val offset = if (offsetRaw < 0) {
        0
      } else {
        offsetRaw
      }

      val firstPage = offset == 0

      val topics = currentUser.profile.topics

      params.put("firstPage", firstPage)
      params.put("topics", topics)
      params.put("offset", offset)
      params.put("disable_event_header", true)
      params.put("unreadCount", currentUser.user.getUnreadEvents)
      params.put("isMyNotifications", true)

      response.addHeader("Cache-Control", "no-cache")

      val list = userEventService.getUserEvents(currentUser.user, showPrivate = true,
        OldEventsCleaner.MaxEventsPerUser, 0, eventFilter
      ).filterNot(r => r.eventType == UserEventFilterEnum.REACTION && r.reaction == NoReaction)

      val prepared = prepareService.prepareGrouped(list, !currentUser.profile.oldTracker)

      if (list.nonEmpty) {
        params.put("enableReset", true)
        params.put("topId", prepared.view.map(_.lastId).max)
      }

      val sliced = prepared.slice(offset, offset + topics).take(topics)

      params.put("topicsList", sliced.asJava)
      params.put("hasMore", sliced.size == topics)

      if (!currentUser.profile.oldTracker) {
        new ModelAndView("show-replies-new", params.asJava)
      } else {
        new ModelAndView("show-replies", params.asJava)
      }
  }

  @RequestMapping(value = Array("/show-replies.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("!output", "!nick"))
  def showNotificationsOld(): ModelAndView = AuthUtil.AuthorizedOnly { _ =>
    new ModelAndView(new RedirectView("/notifications"))
  }

  @RequestMapping(value = Array("/show-replies.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD), params = Array("!output"))
  def showNotificationsForModerator(@RequestParam(value = "filter", defaultValue = "all") filter: String,
                                    @RequestParam(value = "nick") nick: String,
                                    @RequestParam(value = "offset", defaultValue = "0") offsetRaw: Int): ModelAndView =
    AuthUtil.AuthorizedOnly { currentUser =>
      if (!StringUtil.checkLoginName(nick)) {
        throw new BadInputException("некорректное имя пользователя")
      }

      if (currentUser.user.getNick == nick) {
        return new ModelAndView(new RedirectView("/notifications"))
      }

      if (!currentUser.moderator) {
        throw new AccessViolationException("нельзя смотреть чужие уведомления")
      }

      val eventFilter = UserEventFilterEnum.fromNameOrDefault(filter)

      val params = mutable.Map[String, Any]()

      params.put("filter", eventFilter.getName)
      if (eventFilter != UserEventFilterEnum.ALL) {
        params.put("addition_query", s"&filter=${eventFilter.getName}")
      } else {
        params.put("addition_query", "")
      }

      params.put("nick", nick)
      params.put("link", s"/show-replies.jsp?nick=$nick")

      val offset = if (offsetRaw < 0) {
        0
      } else {
        offsetRaw
      }

      val firstPage = offset == 0

      val topics = currentUser.profile.topics

      params.put("firstPage", firstPage)
      params.put("topics", topics)
      params.put("offset", offset)

      val user = userService.getUser(nick)
      params.put("filterValues", userEventService.getEventTypes(user).asJava)

      val list = userEventService.getUserEvents(user, showPrivate = true, topics, offset, eventFilter)
      val prepared = prepareService.prepareSimple(list, withText = false)

      params.put("isMyNotifications", false)
      params.put("topicsList", prepared.asJava)
      params.put("hasMore", list.size == topics)

      new ModelAndView("show-replies", params.asJava)
    }

  @RequestMapping(value = Array("/show-replies.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD), params = Array("output"))
  def repliesFeed(@RequestParam(value = "output") output: String, response: HttpServletResponse,
                  @RequestParam(value = "filter", defaultValue = "all") filter: String,
                  @RequestParam(value = "nick") nick: String): ModelAndView = MaybeAuthorized { currentUserOpt =>
    if (!StringUtil.checkLoginName(nick)) {
      throw new BadInputException("некорректное имя пользователя")
    }

    val viewByOwner = currentUserOpt.userOpt.exists(_.getNick == nick)

    val eventFilter = UserEventFilterEnum.fromNameOrDefault(filter)

    val params = mutable.Map[String, Any]()

    params.put("nick", nick)
    params.put("link", s"/show-replies.jsp?nick=$nick")

    val topics = 200

    val time = System.currentTimeMillis
    response.setDateHeader("Expires", time + 1000 * 90)

    val user = userService.getUser(nick)

    val list = userEventService.getUserEvents(user, viewByOwner, topics, 0, eventFilter)
    val prepared = prepareService.prepareSimple(list, withText = true)
    params.put("topicsList", prepared.asJava)

    val result = new ModelAndView(feedView, params.asJava)

    result.addObject("feed-type", "rss")

    if ("atom" == output) {
      result.addObject("feed-type", "atom")
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