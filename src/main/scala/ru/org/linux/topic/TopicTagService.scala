package ru.org.linux.topic

import org.springframework.stereotype.Service
import com.typesafe.scalalogging.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.tag.{ITagActionHandler, TagDao, TagService}
import org.springframework.scala.transaction.support.TransactionManagement
import scala.collection.JavaConversions._

import TopicTagService._
import org.springframework.validation.Errors
import com.google.common.collect.ImmutableList
import org.springframework.transaction.PlatformTransactionManager

@Service
class TopicTagService @Autowired() (
                                     val transactionManager:PlatformTransactionManager,
                                     tagService:TagService,
                                     topicTagDao:TopicTagDao,
                                     tagDao:TagDao
  ) extends Logging with TransactionManagement {

  tagService.getActionHandlers.add(new ITagActionHandler() {
    override def replaceTag(oldTagId: Int, oldTagName: String, newTagId: Int, newTagName: String):Unit = {
      val tagCount = topicTagDao.getCountReplacedTags(oldTagId, newTagId)
      topicTagDao.replaceTag(oldTagId, newTagId)
      tagDao.increaseCounterById(newTagId, tagCount)

      logger.debug(s"Счётчик использование тега '$newTagName' увеличен на $tagCount")
    }

    override def deleteTag(tagId: Int, tagName: String):Unit = {
      topicTagDao.deleteTag(tagId)
      logger.debug("Удалено использование тега '{}' в топиках", tagName)
    }

    override def reCalculateAllCounters():Unit = {
      topicTagDao.reCalculateAllCounters()
    }
  })

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
      tagDao.increaseCounterById(id, 1)
    }

    for (tag <- oldTags if !newTags.contains(tag)) {
      val id = tagService.getOrCreateTag(tag)
      logger.trace("Уменьшен счётчик для тега " + tag)
      tagDao.decreaseCounterById(id, 1)
    }
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  def getTags(msgId:Int):java.util.List[String] = topicTagDao.getTags(msgId).map(_.name)

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

  /**
   * Разбор строки тегов. Error при ошибках
   *
   * @param tags   список тегов через запятую
   * @param errors класс для ошибок валидации (параметр 'tags')
   * @return список тегов
   */
  def parseTags(tags:String, errors:Errors):Seq[String] = {
    // Теги разделяютчя пайпом или запятой
    val tagsArr = tags.replaceAll("\\|", ",").split(",")

    import scala.collection.breakOut

    val tagSet:Set[String] = (for (aTagsArr <- tagsArr if !aTagsArr.isEmpty) yield {
      val tag = aTagsArr.toLowerCase

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (tag.length() > TagService.MAX_TAG_LENGTH) {
        errors.rejectValue("tags", null, "Слишком длиный тег: '" + tag + "\' (максимум " + TagService.MAX_TAG_LENGTH + " символов)")
      } else if (!TagService.isGoodTag(tag)) {
        errors.rejectValue("tags", null, "Некорректный тег: '" + tag + '\'')
      }

      tag
    })(breakOut)

    if (tagSet.size > MAX_TAGS_PER_TOPIC) {
      errors.rejectValue("tags", null, "Слишком много тегов (максимум " + MAX_TAGS_PER_TOPIC + ')')
    }

    tagSet.toVector
  }
}

object TopicTagService {
  val MAX_TAGS_PER_TOPIC = 5
  val MAX_TAGS_IN_TITLE = 3
}
