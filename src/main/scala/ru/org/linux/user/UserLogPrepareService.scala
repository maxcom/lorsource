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
import ru.org.linux.msgbase.UserAgentDao
import ru.org.linux.site.DateFormats
import ru.org.linux.user.UserLogDao.*
import ru.org.linux.util.StringUtil.escapeHtml

import java.time.Instant
import java.util.Date

object UserLogPrepareService {
  private val OptionDescription: Map[String, String] =
    Map(OptionBonus -> "Изменение score",
        OptionNewEmail -> "Новый email",
        OptionNewUserpic -> "Новая фотография",
        OptionOldEmail -> "Старый email",
        OptionOldInfo -> "Старый текст информации",
        OptionOldUserpic -> "Старая фотография",
        OptionReason -> "Причина",
        OptionUntil -> "Срок действия")
}

@Service
class UserLogPrepareService(userService: UserService, userAgentDao: UserAgentDao) {
  def prepare(items: collection.Seq[UserLogItem], timezone: DateTimeZone): Seq[PreparedUserLogItem] = {
    items.view.map((item: UserLogItem) => {
      val options = for ((rawKey, rawValue) <- item.options) yield {
        val key = UserLogPrepareService.OptionDescription.getOrElse(rawKey, escapeHtml(rawKey))

        val value = rawKey match {
          case OptionOldUserpic | OptionNewUserpic =>
            s"<a href=\"/photos/${escapeHtml(rawValue)}\">${escapeHtml(rawValue)}</a>"
          case OptionIp =>
            s"<a href=\"/sameip.jsp?ip=${escapeHtml(rawValue)}\">${escapeHtml(rawValue)}</a>"
          case OptionInvitedBy =>
            val user = userService.getUserCached(rawValue.toInt)
            s"<a href=\"/people/${user.nick}/profile\">${user.nick}</a>"
          case OptionUserAgent =>
            val id = rawValue.toInt
            val ip = item.options.getOrElse(OptionIp, "")

            if (id != 0) {
              s"<a href=\"/sameip.jsp?ua=$id&ip=$ip&mask=0\">${userAgentDao.getUserAgentById(id).orElse(escapeHtml("<не найден>"))}</a>"
            } else {
              escapeHtml("<нет>")
            }
          case OptionUntil =>
            val until = Instant.parse(rawValue)

            DateFormats.formatDefault(timezone, Date.from(until))
          case _ =>
            escapeHtml(rawValue)
        }

        key -> value
      }

      PreparedUserLogItem(item, userService.getUserCached(item.actionUser), options)
    }).toVector
  }
}
