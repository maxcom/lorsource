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

package ru.org.linux.topic

import com.google.common.collect.{ImmutableList, ImmutableListMultimap}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.tag.TagService._
import ru.org.linux.tag.{TagRef, _}
import ru.org.linux.topic.TopicTagService._

import scala.collection.JavaConversions._

@Service
class TopicTagService @Autowired() (
                                     val transactionManager:PlatformTransactionManager,
                                     tagService:TagModificationService,
                                     topicTagDao:TopicTagDao
  ) extends StrictLogging with TransactionManagement {

  tagService.getActionHandlers.add(new ITagActionHandler() {
    override def replaceTag(oldTagId: Int, newTagId: Int, newTagName: String):Unit = {
      val tagCount = topicTagDao.getCountReplacedTags(oldTagId, newTagId)
      topicTagDao.replaceTag(oldTagId, newTagId)
      topicTagDao.increaseCounterById(newTagId, tagCount)

      logger.debug(s"Счётчик использование тега '$newTagName' увеличен на $tagCount")
    }

    override def deleteTag(tagId: Int, tagName: String):Unit = {
      topicTagDao.deleteTag(tagId)
      logger.debug("Удалено использование тега '{}' в топиках", tagName)
    }
  })

  def reCalculateAllCounters():Unit = {
    topicTagDao.reCalculateAllCounters()
  }

  /**
   * Обновить список тегов сообщения по идентификационному номеру сообщения.
   *
   * @param msgId   идентификационный номер сообщения
   * @param tagList новый список тегов.
   * @return true если были произведены изменения
   */
  def updateTags(msgId:Int, oldTags:java.util.List[String], tagList:java.util.List[String]):Boolean = {
    transactional() { _ =>
      logger.debug("Обновление списка тегов [" + tagList.toString + "] для топика msgId=" + msgId)

      val oldTags = getTags(msgId)

      val newTags = tagList.filter(!oldTags.contains(_))

      for (tag <- newTags) {
        val id = tagService.getOrCreateTag(tag)

        logger.trace("Добавлен тег '" + tag + "' к топику msgId=" + msgId)
        topicTagDao.addTag(msgId, id)
      }

      val deleteTags = oldTags.filter(!tagList.contains(_))

      for (tag <- deleteTags) {
        val id = tagService.getOrCreateTag(tag)
        logger.trace("Удалён тег '" + tag + "' к топику msgId=" + msgId)
        topicTagDao.deleteTag(msgId, id)
      }

      logger.trace("Завершено: обновление списка тегов для топика msgId=" + msgId)

      val modified = newTags.nonEmpty || deleteTags.nonEmpty

      if (modified) {
        updateCounters(oldTags, newTags)
      }

      modified
    }
  }

  /**
   * Обновить счётчики по тегам.
   *
   * @param oldTags список старых тегов
   * @param newTags список новых тегов
   */
  private[this] def updateCounters(oldTags:Seq[String], newTags:Seq[String]):Unit = {
    logger.debug(
            "Обновление счётчиков тегов; старые теги [{}]; новые теги [{}]",
            oldTags,
            newTags
    )
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   */
  def getTags(topic:Topic):java.util.List[String] = topicTagDao.getTags(topic.getId).map(_.name)

  private def getTags(msgId:Int):java.util.List[String] = topicTagDao.getTags(msgId).map(_.name)

  def getTagRefs(topic:Topic):java.util.List[TagRef] =
    topicTagDao.getTags(topic.getId).map(tag => tagRef(tag))

  def getTagRefs(topics:java.util.List[Topic]):ImmutableListMultimap[Integer, TagRef] = {
    val builder = ImmutableListMultimap.builder[Integer,TagRef]()

    val tags = topicTagDao.getTags(topics.map(_.getId))

    for ((msgid, tag) <- tags) {
      builder.put(msgid, tagRef(tag))
    }

    builder.build()
  }

  def tagRefs(topics:Seq[Int]) = topicTagDao.getTags(topics).groupBy(_._1).mapValues(_.map(_._2))

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTagsForTitle(msgId:Int):ImmutableList[String] = {
    val tags = topicTagDao.getTags(msgId).map(_.name).take(MaxTagsInTitle)
    ImmutableList.copyOf(tags.toIterable)
  }
}

object TopicTagService {
  val MaxTagsInTitle = 3
}
