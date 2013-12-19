package ru.org.linux.topic

import org.springframework.stereotype.Service
import com.typesafe.scalalogging.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.tag.{ITagActionHandler, TagDao, TagService}

@Service
class TopicTagService2 @Autowired() (tagService:TagService, topicTagDao:TopicTagDao, tagDao:TagDao) extends Logging {
  tagService.getActionHandlers().add(new ITagActionHandler() {
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
}
