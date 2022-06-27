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

import akka.actor.ActorRef
import com.google.common.collect.ImmutableList
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.site.Template

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.jdk.CollectionConverters._

@Controller
class UserEventApiController(userEventService: UserEventService, realtimeHubWS: ActorRef) {
  @ResponseBody
  @RequestMapping(value = Array("/notifications-count"), method = Array(RequestMethod.GET))
  def getEventsCount(request: HttpServletRequest, response: HttpServletResponse): Int = {
    val tmpl = Template.getTemplate
    if (!tmpl.isSessionAuthorized) throw new AccessViolationException("not authorized")
    response.setHeader("Cache-control", "no-cache")
    AuthUtil.getCurrentUser.getUnreadEvents
  }

  @RequestMapping(value = Array("/notifications-reset"), method = Array(RequestMethod.POST))
  @ResponseBody
  def resetNotifications(request: HttpServletRequest, @RequestParam topId: Int): String = {
    val tmpl = Template.getTemplate
    if (!tmpl.isSessionAuthorized) throw new AccessViolationException("not authorized")
    val currentUser = AuthUtil.getCurrentUser
    userEventService.resetUnreadReplies(currentUser, topId)
    RealtimeEventHub.notifyEvents(realtimeHubWS, ImmutableList.of(currentUser.getId))
    "ok"
  }

  @ResponseBody
  @RequestMapping(value = Array("/yandex-tableau"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getYandexWidget(request: HttpServletRequest, response: HttpServletResponse): java.util.Map[String, Int] = {
    val tmpl = Template.getTemplate
    if (!tmpl.isSessionAuthorized) {
      Map.empty[String, Int].asJava
    } else {
      response.setHeader("Cache-control", "no-cache")
      Map("notifications" -> AuthUtil.getCurrentUser.getUnreadEvents).asJava
    }
  }
}