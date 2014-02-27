package ru.org.linux.tag

import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.topic.TagTopicListController
import scala.collection.JavaConversions._
import org.springframework.stereotype.Service
import java.util

@Service
class TagService @Autowired () (tagDao:TagDao) {
  import TagService._

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String) = tagDao.getTagId(tag).getOrElse(throw new TagNotFoundException)

  @throws(classOf[TagNotFoundException])
  def getTagInfo(tag: String, skipZero: Boolean): TagInfo = {
    val tagId = tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException())

    tagDao.getTagInfo(tagId)
  }

  def getNewTags(tags:util.List[String]):util.List[String] =
    tags.filterNot(tag => tagDao.getTagId(tag, skipZero = true).isDefined)

  def getRelatedTags(tagId: Int): java.util.List[TagRef] =
    namesToRefs(tagDao.relatedTags(tagId)).sorted

  /**
   * Получить список популярных тегов по префиксу.
   *
   * @param prefix     префикс
   * @param count      количество тегов
   * @return список тегов по первому символу
   */
  def suggestTagsByPrefix(prefix: String, count: Int): util.List[String] =
    tagDao.getTopTagsByPrefix(prefix, 2, count)

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  def getFirstLetters: util.List[String] = tagDao.getFirstLetters

  /**
   * Получить список тегов по префиксу.
   *
   * @param prefix     префикс
   * @return список тегов по первому символу
   */
  def getTagsByPrefix(prefix: String, threshold: Int): util.Map[String, Integer] = {
    val result = (for (
      info <- tagDao.getTagsByPrefix(prefix, threshold)
    ) yield info.name -> (info.topicCount:java.lang.Integer)).toMap

    mapAsJavaMap(result)
  }

  /**
   * Получить список наиболее популярных тегов.
   *
   * @return список наиболее популярных тегов
   */
  def getTopTags: util.List[String] = tagDao.getTopTags
}

object TagService {
  def tagRef(tag: TagInfo) = new TagRef(tag.name,
    if (TagName.isGoodTag(tag.name)) {
      Some(TagTopicListController.tagListUrl(tag.name))
    } else {
      None
    })

  def tagRef(name: String) = new TagRef(name,
    if (TagName.isGoodTag(name)) {
      Some(TagTopicListController.tagListUrl(name))
    } else {
      None
    })

  def namesToRefs(tags:java.util.List[String]):java.util.List[TagRef] = tags.map(tagRef)

  def tagsToString(tags: util.Collection[String]): String = tags.mkString(",")
}
