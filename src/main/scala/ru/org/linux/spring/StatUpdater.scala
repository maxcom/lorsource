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
package ru.org.linux.spring

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.TopicDao
import scalikejdbc.*

@Component
class StatUpdater(topicDao: TopicDao, springDB: SpringDB) extends StrictLogging:

  @Scheduled(fixedDelay = 600000, initialDelay = 300000)
  def updateStats(): Unit =
    logger.debug("Updating statistics")
    springDB.run:
      sql"SELECT stat_update()".update.apply()
      sql"SELECT update_monthly_stats()".update.apply()
    topicDao.recalcAllWarningsCountInTx()

  @Scheduled(fixedDelay = 3600000, initialDelay = 300000)
  def updateGroupStats(): Unit =
    logger.debug("Updating group statistics")
    springDB.run:
      sql"SELECT stat_update2()".update.apply()
