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
package ru.org.linux.user

import org.joda.time.DateTimeZone
import org.springframework.stereotype.Service
import ru.org.linux.site.DateFormats
import ru.org.linux.spring.dao.UserAgentDao
import ru.org.linux.user.UserLogDao.*
import ru.org.linux.util.StringUtil.escapeHtml

import java.time.Instant
import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala}

object UserLogPrepareService {
  private val OptionDescription: Map[String, String] =
    Map(OPTION_BONUS -> "Изменение score",
        OPTION_NEW_EMAIL -> "Новый email",
        OPTION_NEW_USERPIC -> "Новая фотография",
        OPTION_OLD_EMAIL -> "Старый email",
        OPTION_OLD_INFO -> "Старый текст информации",
        OPTION_OLD_USERPIC -> "Старая фотография",
        OPTION_REASON -> "Причина",
        OPTION_UNTIL -> "Срок действия")
}

@Service
class UserLogPrepareService(userService: UserService, userAgentDao: UserAgentDao) {
  def prepare(items: collection.Seq[UserLogItem], timezone: DateTimeZone): Seq[PreparedUserLogItem] = {
    items.view.map((item: UserLogItem) => {
      val options = for ((rawKey, rawValue) <- item.getOptions.asScala) yield {
        val key = UserLogPrepareService.OptionDescription.getOrElse(rawKey, escapeHtml(rawKey))

        val value = rawKey match {
          case OPTION_OLD_USERPIC | OPTION_NEW_USERPIC =>
            s"<a href=\"/photos/${escapeHtml(rawValue)}\">${escapeHtml(rawValue)}</a>"
          case OPTION_IP =>
            s"<a href=\"/sameip.jsp?ip=${escapeHtml(rawValue)}\">${escapeHtml(rawValue)}</a>"
          case OPTION_INVITED_BY =>
            val user = userService.getUserCached(rawValue.toInt)
            s"<a href=\"/people/${user.getNick}/profile\">${user.getNick}</a>"
          case OPTION_USER_AGENT =>
            val id = rawValue.toInt
            val ip = item.getOptions.getOrDefault(OPTION_IP, "")

            if (id != 0) {
              s"<a href=\"/sameip.jsp?ua=$id&ip=$ip&mask=0\">${userAgentDao.getUserAgentById(id).orElse(escapeHtml("<не найден>"))}</a>"
            } else {
              escapeHtml("<нет>")
            }
          case OPTION_UNTIL =>
            val until = Instant.parse(rawValue);

            DateFormats.getDefault(timezone).print(until.toEpochMilli)
          case _ =>
            escapeHtml(rawValue)
        }

        key -> value
      }

      new PreparedUserLogItem(item, userService.getUserCached(item.getActionUser), options.asJava)
    }).toVector
  }
}