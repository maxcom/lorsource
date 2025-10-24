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

import org.apache.pekko.actor.typed.ActorRef
import io.circe.Json
import io.circe.syntax.*
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, MaybeAuthorized}
import ru.org.linux.realtime.RealtimeEventHub

@Controller
class UserEventApiController(userEventService: UserEventService,
                             @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef[RealtimeEventHub.Protocol]) {
  @ResponseBody
  @RequestMapping(value = Array("/notifications-count"), method = Array(RequestMethod.GET))
  def getEventsCount(response: HttpServletResponse): Json = AuthorizedOnly { currentUser =>
    response.setHeader("Cache-control", "no-cache")
    currentUser.user.getUnreadEvents.asJson
  }

  @RequestMapping(value = Array("/notifications-reset"), method = Array(RequestMethod.POST))
  @ResponseBody
  def resetNotifications(@RequestParam topId: Int): Json = AuthorizedOnly { currentUser =>
    userEventService.resetUnreadReplies(currentUser.user, topId)
    RealtimeEventHub.notifyEvents(realtimeHubWS, Set(currentUser.user.getId))

    "ok".asJson
  }

  @ResponseBody
  @RequestMapping(value = Array("/yandex-tableau"), method = Array(RequestMethod.GET),
    produces = Array("application/json"))
  def getYandexWidget(response: HttpServletResponse): Json = MaybeAuthorized { session =>
    session.opt match {
      case None =>
        Map.empty[String, Int].asJson
      case Some(currentUser) =>
        response.setHeader("Cache-control", "no-cache")
        Map("notifications" -> currentUser.user.getUnreadEvents).asJson
    }
  }
}