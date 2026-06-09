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
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given

@Component
class ScoreUpdater(userDao: UserDao, springDB: SpringDB) extends StrictLogging:

  @Scheduled(cron = "1 0 1 */2 * *")
  def updateScore(): Unit =
    logger.info("Updating score")
    springDB.localTx { userDao.updateScore() }

  @Scheduled(cron = "1 15 * * * *")
  def updateMaxScore(): Unit = springDB.localTx { userDao.updateMaxScore() }

  @Scheduled(cron = "0 1 * * * *")
  def blockLowScoreUsers(): Unit = userDao.blockLowScoreUsers()

  @Scheduled(cron = "0 30 * * * *")
  def deleteInactivated(): Unit =
    logger.info("Deleting non-activated accounts")
    val (deleted, deletedBlocked) = springDB.localTx { userDao.deleteInactivatedAccounts() }
    logger.info(s"Deleted $deleted non-activated; $deletedBlocked blocked accounts")
