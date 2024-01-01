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

package ru.org.linux.telegram

import akka.actor.{Actor, ActorLogging, Status, Timers}
import akka.pattern.PipeToSupport
import io.circe.parser.*
import ru.org.linux.spring.SiteConfig
import ru.org.linux.telegram.TelegramBotActor.Check
import ru.org.linux.topic.{Topic, TopicTagDao}
import sttp.client3.*

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class TelegramBotActor(dao: TelegramPostsDao, httpClient: SttpBackend[Future, Any], config: SiteConfig,
                       topicTagDao: TopicTagDao)
  extends Actor with Timers with ActorLogging with PipeToSupport {

  timers.startTimerAtFixedRate(Check, Check, 10.minutes)

  import context.dispatcher

  override def receive: Receive = {
    case Check =>
      dao.hotTopic match {
        case Some(topic) =>
          val tags = topicTagDao.getTags(topic.id)

          log.info(s"Posting topic ${topic.getLink}")

          if (config.getTelegramToken.equals("false")) {
            log.info("Posting disabled")
          } else {
            val text = s"${topic.getTitleUnescaped} ${tags.map("#" + _.name.filterNot(_ == ' ')).mkString(" ")}\n\n${config.getSecureUrlWithoutSlash + topic.getLink}"

            basicRequest
              .get(uri"https://api.telegram.org/bot${config.getTelegramToken}/sendMessage"
                .addParams("chat_id" -> "@best_of_lor", "text" -> text))
              .send(httpClient)
              .pipeTo(self)

            context.become(posting(topic))
          }
        case None =>
          log.info("No hot topics :-(")

          dao.topicToDelete match {
            case Some(toDelete) =>
              log.info("Deleting " + toDelete)

              basicRequest
                .get(uri"https://api.telegram.org/bot${config.getTelegramToken}/deleteMessage"
                  .addParams("chat_id" -> "@best_of_lor", "message_id" -> toDelete.toString))
                .send(httpClient)
                .pipeTo(self)

              context.become(deleting(toDelete))
            case None =>
          }
      }
  }

  private def posting(topic: Topic): Receive = {
    case r: Response[Either[String, String]] =>
      r.body match {
        case Right(body) =>
          val json = parse(body)

          val telegramId = json.flatMap(
            _.hcursor.downField("result").downField("message_id").as[Int]
          ).toTry.get

          log.info(s"Post success! telegramId = $telegramId")
          dao.storePost(topic, telegramId)
          context.become(receive)
        case Left(error) =>
          log.error(s"Failed to post: status=${r.code} body=$error")
          context.become(receive)
      }
    case Status.Failure(ex) =>
      log.error(ex, "Posting failed")
      context.become(receive)
  }

  private def deleting(telegramId: Int): Receive = {
    case r: Response[Either[String, String]] =>
      if (r.isSuccess) {
        log.info(s"Delete success! telegramId = $telegramId")
        dao.storeDeletion(telegramId)
        context.become(receive)
      } else {
        log.error(s"Failed to delete: status=${r.code} body=${r.body}")
        context.become(receive)
      }
    case Status.Failure(ex) =>
      log.error(ex, "Deleting failed")
      context.become(receive)
  }
}

object TelegramBotActor {
  case object Check
}
