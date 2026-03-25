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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{Topic, TopicTagDao}
import sttp.client3.*
import sttp.shared.Identity
import io.circe.parser.*

@Component
class TelegramPoster(
    dao: TelegramPostsDao,
    httpClient: SttpBackend[Identity, Any],
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

  private def post(topic: Topic, text: String): Unit =
    logger.debug(s"Posting topic ${topic.getLink}")

    val response = basicRequest
      .get(
        uri"https://api.telegram.org/bot${config.getTelegramToken}/sendMessage".addParams(
          "chat_id" -> "@best_of_lor",
          "text" -> text))
      .send(httpClient)

    response.body match
      case Right(body) =>
        val json = parse(body)

        val telegramId = json.flatMap(_.hcursor.downField("result").downField("message_id").as[Int]).toTry.get

        logger.info(s"Post success! ${topic.getLink} telegramId = $telegramId")

        dao.storePost(topic, telegramId)
      case Left(error) =>
        logger.error(s"Post failed! ${topic.getLink} status=${response.code} body=$error")

  private def delete(telegramId: Int, topicId: Int): Unit =
    logger.info("Deleting " + topicId)

    val response = basicRequest
      .get(
        uri"https://api.telegram.org/bot${config.getTelegramToken}/deleteMessage".addParams(
          "chat_id" -> "@best_of_lor",
          "message_id" -> topicId.toString))
      .send(httpClient)

    if response.isSuccess then
      logger.info(s"Delete success! telegramId = $telegramId")
      dao.storeDeletion(telegramId)
    else
      logger.error(s"Failed to delete: status=${response.code} body=${response.body}")
