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

/** Периодическое удаление неактивных пользователей.
  *
  * Удаляются две категории пользователей без активности (топики, комментарии, реакции и прочая активность):
  *   - заблокированные более 3 лет назад. Дата блокировки берётся из `ban_info.bandate`, при отсутствии — из
  *     `lastlogin`, затем из `regdate`; если ни одна дата не известна, пользователь удаляется сразу;
  *   - не заблокированные, не заходившие на сайт более 10 лет. Используется `lastlogin`, при отсутствии — `regdate`;
  *     если ни одна дата не известна, пользователь удаляется сразу.
  *
  * При выключенном флаге `cleanInactiveUsers` только логгируются кандидаты на удаление. При ошибке удаления исключение
  * пробрасывается наружу — его обработает обработчик ошибок планировщика (лог + письмо администратору).
  */
@Component
class InactiveUserCleaner(siteConfig: SiteConfig, userDao: UserDao) extends StrictLogging:

  @Scheduled(cron = "0 30 5 * * *")
  def cleanInactiveUsers(): Unit =
    deleteCandidates(userDao.getDeletableBlockedUserIds, "blocked")
    deleteCandidates(userDao.getDeletableInactiveUserIds, "inactive")

  private def deleteCandidates(ids: Seq[Int], category: String): Unit =
    if ids.isEmpty then
      logger.info(s"InactiveUserCleaner: no $category candidates")
    else if siteConfig.cleanInactiveUsers then
      var deleted = 0
      ids
        .grouped(InactiveUserCleaner.BatchSize)
        .foreach { batch =>
          deleted += userDao.deleteUsers(batch)
        }
      logger.info(s"InactiveUserCleaner: deleted $deleted of ${ids.size} $category candidates")
    else
      logger.info(s"InactiveUserCleaner: would delete ${ids.size} $category users")
      ids
        .grouped(InactiveUserCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"InactiveUserCleaner $category candidates: ${batch.mkString(", ")}")
        }

object InactiveUserCleaner:
  val BatchSize = 500
