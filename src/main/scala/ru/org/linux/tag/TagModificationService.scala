/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.tag

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.topic.TopicTagDao
import ru.org.linux.user.UserTagDao

import scala.collection.mutable

@Service
class TagModificationService(val transactionManager: PlatformTransactionManager, tagDao: TagDao,
                             tagService: TagService, userTagDao: UserTagDao, topicTagDao: TopicTagDao,
                             searchQueueSender: SearchQueueSender) extends StrictLogging with TransactionManagement {
  /**
    * Изменить название существующего тега.
    *
    * @param oldTagName старое название тега
    * @param tagName    новое название тега
    */
  def change(oldTagName: String, tagName: String): Unit = transactional() { _ =>
    val oldTagId = tagService.getTagId(oldTagName)

    tagDao.deleteTagSynonym(tagName)
    tagDao.changeTag(oldTagId, tagName)

    topicTagDao.processTopicsByTag(oldTagId, searchQueueSender.updateMessage(_, true))
  }

  /**
    * Удалить тег по названию.
    *
    * @param tagName    название тега
    */
  def delete(tagName: String): Unit = {
    val toUpdate = transactional() { _ =>
      val oldTagId = tagService.getTagId(tagName)

      val toUpdate = mutable.Buffer[Int]()
      topicTagDao.processTopicsByTag(oldTagId, toUpdate += _)

      userTagDao.deleteTags(oldTagId)
      topicTagDao.deleteTag(oldTagId)

      tagDao.deleteTag(oldTagId)

      toUpdate
    }

    toUpdate.foreach(searchQueueSender.updateMessage(_, true))
  }

  /**
    * Удалить тег по названию. Заменить все использования удаляемого тега
    * новым тегом.
    *
    * @param tagName    название тега
    * @param newTagName новое название тега
    */
  def merge(tagName: String, newTagName: String, createSynonym: Boolean): Unit = {
    val newTagId = transactional() { _ =>
      val oldTagId = tagService.getTagId(tagName)

      assume(newTagName != tagName, "Заменяемый тег не должен быть равен удаляемому")

      val newTagId = tagService.getOrCreateTag(newTagName)

      val tagCount = topicTagDao.getCountReplacedTags(oldTagId, newTagId)
      topicTagDao.replaceTag(oldTagId, newTagId)
      topicTagDao.increaseCounterById(newTagId, tagCount)

      userTagDao.replaceTag(oldTagId, newTagId)
      tagDao.updateTagSynonym(oldTagId, newTagId)

      logger.info("Удаляемый тег '{}' заменён тегом '{}'", tagName, newTagName)

      userTagDao.deleteTags(oldTagId)
      topicTagDao.deleteTag(oldTagId)

      tagDao.deleteTag(oldTagId)

      if (createSynonym) {
        tagDao.createTagSynonym(tagName, newTagId)
      }

      newTagId
    }

    topicTagDao.processTopicsByTag(newTagId, searchQueueSender.updateMessage(_, true))
  }
}