/*
 * Copyright 1998-2015 Linux.org.ru
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
package ru.org.linux.search

import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConverters._
import scala.concurrent.duration.Deadline

@Component
class SearchQueueListener @Autowired() (
  indexService: ElasticsearchIndexService
) extends StrictLogging {
  private var mappingsSet = false
  
  def handleMessage(msgUpdate: SearchQueueSender.UpdateMessage):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    logger.info(s"Indexing ${msgUpdate.getMsgid}")

    indexService.reindexMessage(msgUpdate.getMsgid, msgUpdate.isWithComments)
  }

  def handleMessage(msgUpdate: SearchQueueSender.UpdateComments):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    logger.info(s"Indexing comments ${msgUpdate.getMsgids}")

    indexService.reindexComments(msgUpdate.getMsgids.asScala.map(x ⇒ x.toInt).toSeq)
  }

  def handleMessage(msgUpdate: SearchQueueSender.UpdateMonth):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    val month = msgUpdate.getMonth
    val year = msgUpdate.getYear

    logger.info(s"Indexing month $year/$month")

    val startTime = Deadline.now

    indexService.reindexMonth(year, month)

    val endTime = Deadline.now - startTime

    logger.info(s"Reindex month $year/$month done, ${endTime.toMillis} millis")
  }

  private def createIndex():Unit = {
    indexService.createIndexIfNeeded()

    mappingsSet = true
  }
}