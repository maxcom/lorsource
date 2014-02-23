package ru.org.linux.topic

import org.springframework.stereotype.Service
import com.typesafe.scalalogging.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.tag._
import org.springframework.scala.transaction.support.TransactionManagement
import scala.collection.JavaConversions._

import TopicTagService._
import com.google.common.collect.{ImmutableListMultimap, ImmutableList}
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.tag.TagRef

@Service
class TopicTagService @Autowired() (
                                     val transactionManager:PlatformTransactionManager,
                                     tagService:TagModificationService,
                                     topicTagDao:TopicTagDao
  ) extends Logging with TransactionManagement {

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

      val modified = !newTags.isEmpty || !deleteTags.isEmpty

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

    for (tag <- newTags if !oldTags.contains(tag)) {
      val id = tagService.getOrCreateTag(tag)
      logger.trace("Увеличен счётчик для тега " + tag)
      topicTagDao.increaseCounterById(id, 1)
    }

    for (tag <- oldTags if !newTags.contains(tag)) {
      val id = tagService.getOrCreateTag(tag)
      logger.trace("Уменьшен счётчик для тега " + tag)
      topicTagDao.decreaseCounterById(id, 1)
    }
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTags(msgId:Int):java.util.List[String] = topicTagDao.getTags(msgId).map(_.name)

  def getTagRefs(topic:Topic):java.util.List[TagRef] =
    topicTagDao.getTags(topic.getId).map(tag => tagRef(tag))

  def getTagRefs(topics:java.util.List[Topic]):ImmutableListMultimap[Integer, TagRef] = {
    val builder = ImmutableListMultimap.builder[Integer,TagRef]()

    val tags = topicTagDao.getTags(topics)

    for ((msgid, tag) <- tags) {
      builder.put(msgid, tagRef(tag))
    }

    builder.build()
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTagsForTitle(msgId:Int):ImmutableList[String] = {
    val tags = topicTagDao.getTags(msgId).map(_.name).take(MAX_TAGS_IN_TITLE)
    ImmutableList.copyOf(tags.toIterable)
  }
}

object TopicTagService {
  val MAX_TAGS_IN_TITLE = 3

  // TODO: move to TagService
  def tagRef(tag: TagInfo) = new TagRef(tag.name,
    if (TagName.isGoodTag(tag.name)) {
      Some(TagTopicListController.tagListUrl(tag.name))
    } else {
      None
    })

  // TODO: move to TagService
  def tagRef(name: String) = new TagRef(name,
    if (TagName.isGoodTag(name)) {
      Some(TagTopicListController.tagListUrl(name))
    } else {
      None
    })

  // TODO: move to TagService
  def namesToRefs(tags:java.util.List[String]):java.util.List[TagRef] = tags.map(tagRef)
}
