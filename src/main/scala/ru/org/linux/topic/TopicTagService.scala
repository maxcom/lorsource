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

package ru.org.linux.topic

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Service
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.tag.*
import ru.org.linux.tag.TagService.*
import ru.org.linux.topic.TopicTagService.*

import scala.collection.Map

@Service
class TopicTagService(springDB: SpringDB, tagService: TagService,
                      topicTagDao: TopicTagDao) extends StrictLogging {

  /**
   * Обновить список тегов сообщения по идентификационному номеру сообщения.
   *
   * @param msgId   идентификационный номер сообщения
   * @param tagList новый список тегов.
   * @return true если были произведены изменения
   */
  def updateTags(msgId: Int, tagList: Seq[String]): Boolean = {
    springDB.localTx {
      logger.debug(s"Обновление списка тегов [${tagList.toString}] для топика msgId=$msgId")

      val oldTags = getTags(msgId)

      val addTags = tagList.filterNot(oldTags.contains)
      val deleteTags = oldTags.filterNot(tagList.contains)

      val addIds = addTags.map(tag => tag -> tagService.getOrCreateTag(tag))
      val deleteIds = deleteTags.map(tag => tag -> tagService.getOrCreateTag(tag))

      val lockIds = (addIds.map(_._2) ++ deleteIds.map(_._2)).distinct.sorted
      if lockIds.nonEmpty then
        topicTagDao.lockTagValues(lockIds)

      for ((tag, id) <- addIds) {
        logger.trace(s"Добавлен тег '$tag' к топику msgId=$msgId")
        topicTagDao.addTag(msgId, id)
      }

      for ((tag, id) <- deleteIds) {
        logger.trace(s"Удалён тег '$tag' к топику msgId=$msgId")
        topicTagDao.deleteTag(msgId, id)
      }

      logger.trace(s"Завершено: обновление списка тегов для топика msgId=$msgId")

      addTags.nonEmpty || deleteTags.nonEmpty
    }
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   */
  def getTags(topic: Topic): Seq[String] = topicTagDao.getTags(topic.id).map(_.name)

  private def getTags(msgId: Int): Seq[String] = topicTagDao.getTags(msgId).map(_.name)

  def getTagRefs(topic: Topic): Seq[TagRef] = topicTagDao.getTags(topic.id).map(t => tagRef(t))

  def tagRefs(topics: Seq[Int]): Map[Int, Seq[TagRef]] =
    topicTagDao.getTags(topics).groupMap(_._1)(p => tagRef(p._2))

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTagsForTitle(msgId: Int): Seq[String] = topicTagDao.getTags(msgId).map(_.name).take(MaxTagsInTitle)
}

object TopicTagService {
  val MaxTagsInTitle = 3
}
