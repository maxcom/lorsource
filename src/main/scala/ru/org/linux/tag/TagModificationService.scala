/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.base.Strings
import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.topic.TopicTagDao
import ru.org.linux.user.UserTagDao

@Service
class TagModificationService(val transactionManager: PlatformTransactionManager, tagDao: TagDao,
                             tagService: TagService, userTagDao: UserTagDao,
                             topicTagDao: TopicTagDao) extends StrictLogging with TransactionManagement {
  /**
    * Изменить название существующего тега.
    *
    * @param oldTagName старое название тега
    * @param tagName    новое название тега
    */
  def change(oldTagName: String, tagName: String): Unit = {
    val oldTagId = tagService.getTagId(oldTagName)

    tagDao.changeTag(oldTagId, tagName)
  }

  /**
    * Удалить тег по названию. Заменить все использования удаляемого тега
    * новым тегом (если имя нового тега не null).
    *
    * @param tagName    название тега
    * @param newTagName новое название тега
    */
  def delete(tagName: String, newTagName: String): Unit = {
    transactional() { _ =>
      val oldTagId = tagService.getTagId(tagName)

      if (!Strings.isNullOrEmpty(newTagName)) {
        assume(newTagName != tagName, "Заменяемый тег не должен быть равен удаляемому")

        val newTagId = tagService.getOrCreateTag(newTagName)

        val tagCount = topicTagDao.getCountReplacedTags(oldTagId, newTagId)
        topicTagDao.replaceTag(oldTagId, newTagId)
        topicTagDao.increaseCounterById(newTagId, tagCount)

        logger.debug(s"Счётчик использование тега '$newTagName' увеличен на $tagCount")

        userTagDao.replaceTag(oldTagId, newTagId)

        logger.debug("Удаляемый тег '{}' заменён тегом '{}'", tagName, newTagName)
      }

      userTagDao.deleteTags(oldTagId)
      topicTagDao.deleteTag(oldTagId)

      tagDao.deleteTag(oldTagId)
    }
  }
}