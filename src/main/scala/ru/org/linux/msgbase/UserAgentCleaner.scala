/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.msgbase

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig

/** Периодическое удаление неиспользуемых записей словаря user_agents.
  *
  * Критерии описаны в doc/data-retention.md: раз в день удаляются записи, на которые нет упоминаний в БД — проверяются
  * не только внешние ключи (topics.ua_id, comments.ua_id), но и другие использования, такие как упоминания id в журнале
  * действий учётной записи (user_log, ключ info->'user_agent').
  *
  * Запускается после DeletedCommentCleaner (6:00) и DeletedTopicCleaner (6:30), чтобы ссылки, освобождённые при
  * окончательном удалении сообщений, подчищались в тот же день.
  *
  * При выключенном флаге `cleanUserAgents` (по умолчанию) вместо удаления кандидаты только логгируются (dry-run). При
  * ошибке удаления исключение пробрасывается наружу — его обработает обработчик ошибок планировщика (лог + письмо
  * администратору); оставшиеся кандидаты будут удалены следующим запуском.
  */
@Component
class UserAgentCleaner(siteConfig: SiteConfig, userAgentDao: UserAgentDao) extends StrictLogging:

  @Scheduled(cron = "0 0 7 * * *")
  def cleanUserAgents(): Unit =
    val ids = userAgentDao.getUnreferencedUserAgentIds

    if ids.isEmpty then
      logger.info("UserAgentCleaner: no candidates")
    else if siteConfig.cleanUserAgents then
      var deleted = 0
      ids
        .grouped(UserAgentCleaner.BatchSize)
        .foreach { batch =>
          val purged = userAgentDao.purgeUnreferencedUserAgents(batch)
          deleted += purged
          logger.info(s"UserAgentCleaner: purged $purged of ${batch.size}: ${batch.mkString(", ")}")
        }
      logger.info(s"UserAgentCleaner: deleted $deleted of ${ids.size} candidates")
    else
      logger.info(s"UserAgentCleaner: would delete ${ids.size} user agents")
      ids
        .grouped(UserAgentCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"UserAgentCleaner candidates: ${batch.mkString(", ")}")
        }

object UserAgentCleaner:
  val BatchSize = 500
