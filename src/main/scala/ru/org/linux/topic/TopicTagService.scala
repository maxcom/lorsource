/*
 * Copyright 1998-2019 Linux.org.ru
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
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.tag.TagService._
import ru.org.linux.tag.{TagRef, _}
import ru.org.linux.topic.TopicTagService._

import scala.collection.JavaConverters._
import scala.collection.Map

@Service
class TopicTagService(val transactionManager: PlatformTransactionManager, tagService: TagService,
                      topicTagDao: TopicTagDao) extends StrictLogging with TransactionManagement {

  def reCalculateAllCounters(): Unit = topicTagDao.reCalculateAllCounters()

  /**
   * Обновить список тегов сообщения по идентификационному номеру сообщения.
   *
   * @param msgId   идентификационный номер сообщения
   * @param tagList новый список тегов.
   * @return true если были произведены изменения
   */
  def updateTags(msgId: Int, oldTags: java.util.List[String], tagList: java.util.List[String]): Boolean = {
    transactional() { _ =>
      logger.debug(s"Обновление списка тегов [${tagList.toString}] для топика msgId=$msgId")

      val oldTags = getTags(msgId)

      val newTags = tagList.asScala.filterNot(oldTags.contains)

      for (tag <- newTags) {
        val id = tagService.getOrCreateTag(tag)

        logger.trace(s"Добавлен тег '$tag' к топику msgId=$msgId")
        topicTagDao.addTag(msgId, id)
      }

      val deleteTags = oldTags.asScala.filterNot(tagList.contains)

      for (tag <- deleteTags) {
        val id = tagService.getOrCreateTag(tag)
        logger.trace(s"Удалён тег '$tag' к топику msgId=$msgId")
        topicTagDao.deleteTag(msgId, id)
      }

      logger.trace(s"Завершено: обновление списка тегов для топика msgId=$msgId")

      newTags.nonEmpty || deleteTags.nonEmpty
    }
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   */
  def getTags(topic:Topic): java.util.List[String] = topicTagDao.getTags(topic.getId).map(_.name).asJava

  private def getTags(msgId:Int): java.util.List[String] = topicTagDao.getTags(msgId).map(_.name).asJava

  def getTagRefs(topic: Topic): java.util.List[TagRef] =
    topicTagDao.getTags(topic.getId).map(tagRef).asJava

  def getTagRefs(topics:java.util.List[Topic]): ImmutableListMultimap[Integer, TagRef] = {
    val builder = ImmutableListMultimap.builder[Integer,TagRef]()

    val tags = topicTagDao.getTags(topics.asScala.map(_.getId))

    for ((msgid, tag) <- tags) {
      builder.put(msgid, tagRef(tag))
    }

    builder.build()
  }

  def tagRefs(topics: scala.collection.Seq[Int]): Map[Int, scala.collection.Seq[TagInfo]] =
    topicTagDao.getTags(topics).groupBy(_._1).mapValues(_.map(_._2)).toMap

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTagsForTitle(msgId:Int): ImmutableList[String] = {
    val tags = topicTagDao.getTags(msgId).map(_.name).take(MaxTagsInTitle)
    ImmutableList.copyOf(tags.asJava)
  }
}

object TopicTagService {
  val MaxTagsInTitle = 3
}
