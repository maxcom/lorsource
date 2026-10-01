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
  *
  * Отдельно, ежечасно и независимо от флага `cleanInactiveUsers`, удаляются неотактивированные аккаунты — см.
  * [[deleteInactivated]].
  */
@Component
class InactiveUserCleaner(siteConfig: SiteConfig, userDao: UserDao) extends StrictLogging:

  @Scheduled(cron = "0 30 5 * * *")
  def cleanInactiveUsers(): Unit =
    deleteCandidates(userDao.getDeletableBlockedUserIds, "blocked")
    deleteCandidates(userDao.getDeletableInactiveUserIds, "inactive")

  /** Ежечасное удаление неотактивированных аккаунтов: незаблокированных, зарегистрированных более 12 часов назад, и
    * заблокированных — более 30 дней назад. Работает независимо от флага `cleanInactiveUsers`.
    */
  @Scheduled(cron = "0 30 * * * *")
  def deleteInactivated(): Unit =
    logger.info("Deleting non-activated accounts")
    val (deleted, deletedBlocked) = userDao.deleteInactivatedAccounts()
    logger.info(s"Deleted $deleted non-activated; $deletedBlocked blocked accounts")

  private def deleteCandidates(ids: Seq[Int], category: String): Unit =
    if ids.isEmpty then
      logger.info(s"InactiveUserCleaner: no $category candidates")
    else if siteConfig.cleanInactiveUsers then
      val deleted = userDao.deleteUsers(ids)
      logger.info(s"InactiveUserCleaner: deleted $deleted of ${ids.size} $category candidates")
    else
      logger.info(s"InactiveUserCleaner: would delete ${ids.size} $category users")
      ids
        .grouped(UserDao.DeleteBatchSize)
        .foreach { batch =>
          logger.info(s"InactiveUserCleaner $category candidates: ${batch.mkString(", ")}")
        }
