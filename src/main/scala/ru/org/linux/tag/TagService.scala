package ru.org.linux.tag

import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.topic.TopicTagService
import scala.collection.JavaConversions._
import org.springframework.stereotype.Service

@Service
class TagService @Autowired () (tagDao:TagDao) {
  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String) = tagDao.getTagId(tag).getOrElse(throw new TagNotFoundException)

  @throws(classOf[TagNotFoundException])
  def getTagInfo(tag: String, skipZero: Boolean): TagInfo = {
    val tagId = tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException())

    tagDao.getTagInfo(tagId)
  }

  def getRelatedTags(tagId: Int): java.util.List[TagRef] =
    TopicTagService.namesToRefs(tagDao.relatedTags(tagId)).sorted
}
