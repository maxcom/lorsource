package ru.org.linux.tag

import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.topic.TagTopicListController
import scala.collection.JavaConversions._
import org.springframework.stereotype.Service

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

  def getRelatedTags(tagId: Int): java.util.List[TagRef] =
    namesToRefs(tagDao.relatedTags(tagId)).sorted
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
}
