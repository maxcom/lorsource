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
package ru.org.linux.spring

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.auth.IPBlockDao
import ru.org.linux.comment.DeleteService
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.user.UserErrorException

import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}

@Controller
class DelIPController(searchQueueSender: SearchQueueSender, commentDeleteService: DeleteService,
                      ipBlockDao: IPBlockDao) extends StrictLogging {
  /**
   * Контроллер удаление топиков и сообщений по ip и времени
   *
   * @param reason причина удаления
   * @param ip     ip по которому удаляем
   * @param time   время за которое удаляем (hour, day, 3day, 5day)
   * @return возвращаем страничку с результатом выполнения
   */
  @RequestMapping(value = Array("/delip.jsp"), method = Array(RequestMethod.POST))
  def delIp(@RequestParam("reason") reason: String, @RequestParam("ip") ip: String, @RequestParam("time") time: String,
            @RequestParam(value = "ban_time", required = false) banTime: String,
            @RequestParam(value = "ban_mode", required = false) banMode: String): ModelAndView = ModeratorOnly { implicit currentUser =>
    val params = new mutable.HashMap[String, AnyRef]

    val delFrom = time match {
      case "hour" => Instant.now.minus(1, ChronoUnit.HOURS)
      case "day" => Instant.now.minus(1, ChronoUnit.DAYS)
      case "3day" => Instant.now.minus(3, ChronoUnit.DAYS)
      case "5day" => Instant.now.minus(5, ChronoUnit.DAYS)
      case _ => throw new UserErrorException("Invalid count")
    }

    val ts = new Timestamp(delFrom.toEpochMilli)

    params.put("message", s"Удаляем темы и сообщения после $ts с IP $ip<br>")

    val moderator = currentUser.user

    if (banTime != null) {
      val banTo = banTime match {
        case "hour" => Some(OffsetDateTime.now.plusHours(1))
        case "day" => Some(OffsetDateTime.now.plusDays(1))
        case "month" => Some(OffsetDateTime.now.plusMonths(1))
        case "3month" => Some(OffsetDateTime.now.plusMonths(3))
        case "6month" => Some(OffsetDateTime.now.plusMonths(6))
        case "unlim" => None
        case "remove" => Some(OffsetDateTime.now)
        case _ => throw new UserErrorException("Invalid count")
      }

      var allowPosting = false
      var captchaRequired = false

      banMode match {
        case "anonymous_and_captcha" =>
          allowPosting = true
          captchaRequired = true
        case "anonymous_only" =>
          allowPosting = true
          captchaRequired = false
        case _ =>
          allowPosting = false
          captchaRequired = false
      }

      ipBlockDao.blockIP(ip, moderator.getId, reason, banTo.orNull, allowPosting, captchaRequired)
    }

    val deleteResult = commentDeleteService.deleteByIPAddress(ip, ts, reason)

    params.put("topics", Int.box(deleteResult.getDeletedTopicIds.size))
    params.put("comments", Int.box(deleteResult.getDeletedCommentIds.size))
    params.put("skipped", deleteResult.getSkippedComments)

    for (topicId <- deleteResult.getDeletedTopicIds.asScala) {
      searchQueueSender.updateMessage(topicId, true)
    }

    searchQueueSender.updateComment(deleteResult.getDeletedCommentIds)

    logger.info("Deleted {} from {} by moderator {}: topic={}; comments={}", ip, time,
      currentUser.user.getNick, deleteResult.getDeletedTopicIds.size, deleteResult.getDeletedCommentIds.size)

    new ModelAndView("delip", params.asJava)
  }
}