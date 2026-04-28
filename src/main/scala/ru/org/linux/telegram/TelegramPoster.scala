/*
 * Copyright 1998-2026 Linux.org.ru
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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{Topic, TopicTagDao}
import sttp.client4.*
import io.circe.parser.*

import scala.util.{Failure, Success, Try}

@Component
class TelegramPoster(
    dao: TelegramPostsDao,
    @Qualifier("directBackend") httpClient: SyncBackend,
    @Qualifier("proxyBackend") proxyBackend: SyncBackend,
    config: SiteConfig,
    topicTagDao: TopicTagDao)
    extends StrictLogging:

  @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 1 * 60 * 1000)
  def check(): Unit =
    dao.hotTopic match
      case Some(topic) =>
        val tags = topicTagDao.getTags(topic.id)

        if config.getTelegramToken.equals("false") then
          logger.info("Posting disabled")
        else
          val text =
            s"${topic.getTitleUnescaped} ${tags.map("#" + _.name.filterNot(_ == ' ')).mkString(" ")}\n\n${config
                .getSecureUrlWithoutSlash + topic.getLink}"

          post(topic, text)
      case None =>
        logger.info("No hot topics :-(")

        dao.topicToDelete match
          case Some(toDelete) =>
            delete(toDelete, toDelete)
          case None =>

  private def sendWithRetry(request: Request[Either[String, String]]): (Response[Either[String, String]], String) =
    Try(request.send(httpClient)) match
      case Success(response) if response.body.isRight =>
        (response, "direct")
      case Success(response) =>
        logger.warn(s"Direct request failed with status ${response.code}, retrying via proxy")
        (request.send(proxyBackend), "proxy")
      case Failure(e) =>
        logger.warn(s"Direct request failed with exception: ${e.getMessage}, retrying via proxy")
        (request.send(proxyBackend), "proxy")

  private def post(topic: Topic, text: String): Unit =
    logger.debug(s"Posting topic ${topic.getLink}")

    val request = basicRequest
      .get(
        uri"https://api.telegram.org/bot${config.getTelegramToken}/sendMessage".addParams(
          "chat_id" -> "@best_of_lor",
          "text" -> text))

    val (response, via) = sendWithRetry(request)

    response.body match
      case Right(body) =>
        val json = parse(body)

        val telegramId = json.flatMap(_.hcursor.downField("result").downField("message_id").as[Int]).toTry.get

        logger.info(s"Post success via $via! ${topic.getLink} telegramId = $telegramId")

        dao.storePost(topic, telegramId)
      case Left(error) =>
        logger.error(s"Post failed via $via! ${topic.getLink} status=${response.code} body=$error")
        throw new TelegramBadStatusException(s"Post failed via $via! status=${response.code}")

  private def delete(telegramId: Int, topicId: Int): Unit =
    logger.info("Deleting " + topicId)

    val request = basicRequest
      .get(
        uri"https://api.telegram.org/bot${config.getTelegramToken}/deleteMessage".addParams(
          "chat_id" -> "@best_of_lor",
          "message_id" -> topicId.toString))

    val (response, via) = sendWithRetry(request)

    if response.body.isRight then
      logger.info(s"Delete success via $via! telegramId = $telegramId")
      dao.storeDeletion(telegramId)
    else
      logger.error(s"Failed to delete via $via: status=${response.code} body=${response.body}")
      throw new TelegramBadStatusException(s"Delete failed via $via! status=${response.code}")

class TelegramBadStatusException(message: String) extends RuntimeException(message)
