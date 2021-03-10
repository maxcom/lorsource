/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.telegram

import akka.actor.{Actor, ActorLogging, Status, Timers}
import akka.pattern.PipeToSupport
import play.api.libs.json.Json
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSResponse}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.telegram.TelegramBotActor.Check
import ru.org.linux.topic.Topic

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt

class TelegramBotActor(dao: TelegramPostsDao, wsClient: StandaloneWSClient, config: SiteConfig)
  extends Actor with Timers with ActorLogging with PipeToSupport {

  timers.startTimerAtFixedRate(Check, Check, 10.minutes)

  import context.dispatcher

  override def receive: Receive = {
    case Check =>
      dao.hotTopic match {
        case Some(topic) =>
          log.info(s"Posting topic ${topic.getLink}")

          if (config.getTelegramToken.equals("false")) {
            log.info("Posting disabled")
          } else {
            wsClient
              .url(s"https://api.telegram.org/bot${config.getTelegramToken}/sendMessage")
              .addQueryStringParameters("chat_id" -> "@best_of_lor")
              .addQueryStringParameters("text" -> (config.getSecureUrlWithoutSlash + topic.getLink))
              .get()
              .pipeTo(self)

            context.become(posting(topic))
          }
        case None =>
          log.info("No hot topics :-(")
      }
  }

  private def posting(topic: Topic): Receive = {
    case r: StandaloneWSResponse =>
      if (r.status>=200 && r.status < 300) {
        val telegramId = (Json.parse(r.body) \ "result" \ "message_id").as[Int]

        log.info(s"Post success! telegramId = $telegramId")
        dao.storePost(topic, telegramId)
        context.become(receive)
      } else {
        log.error(s"Failed to post: status=${r.status} body=${r.body}")
        context.become(receive)
      }
    case Status.Failure(ex) =>
      log.error(ex, "Posting failed")
      context.become(receive)
  }
}

object TelegramBotActor {
  case object Check
}
