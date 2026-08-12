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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig

import scala.util.control.NonFatal

/** Периодическое удаление заблокированных пользователей без активности.
  *
  * Удаляются пользователи, заблокированные более 3 лет назад и не имеющие топиков, комментариев или реакций (а также
  * прочей активности). Дата блокировки берётся из `ban_info.bandate`, при отсутствии — из `lastlogin`, затем из
  * `regdate`; если ни одна дата не известна, пользователь удаляется сразу.
  *
  * При выключенном флаге `cleanOldBlockedUsers` только логгируются кандидаты на удаление.
  */
@Component
class BlockedUserCleaner(siteConfig: SiteConfig, userDao: UserDao) extends StrictLogging:

  @Scheduled(cron = "0 30 5 * * *")
  def cleanBlockedUsers(): Unit =
    val ids = userDao.getDeletableBlockedUserIds

    if ids.isEmpty then
      logger.info("BlockedUserCleaner: no candidates")
    else if siteConfig.cleanOldBlockedUsers then
      var deleted = 0
      ids
        .grouped(BlockedUserCleaner.BatchSize)
        .foreach { batch =>
          try
            deleted += userDao.deleteBlockedUsers(batch)
          catch
            case NonFatal(e) =>
              logger.error(s"BlockedUserCleaner: failed to delete ${batch.size} users", e)
        }
      logger.info(s"BlockedUserCleaner: deleted $deleted of ${ids.size} candidates")
    else
      logger.info(s"BlockedUserCleaner: would delete ${ids.size} blocked users")
      ids
        .grouped(BlockedUserCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"BlockedUserCleaner candidates: ${batch.mkString(", ")}")
        }

object BlockedUserCleaner:
  val BatchSize = 500
