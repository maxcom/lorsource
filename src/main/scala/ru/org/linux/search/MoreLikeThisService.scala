package ru.org.linux.search

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import ru.org.linux.topic.{TopicDao, Topic}
import scala.collection.JavaConversions._

@Service
class MoreLikeThisService @Autowired() (
  client:Client,
  topicDao:TopicDao
) {
  // TODO async - return ListenableFuture
  // TODO timeout
  def search(topic:Topic):Seq[Topic] = {
    // TODO filter out comments
    // TODO boost tags
    // see http://stackoverflow.com/questions/15300650/elasticsearch-more-like-this-api-vs-more-like-this-query

    val query = client.prepareMoreLikeThis(
      SearchQueueListener.MESSAGES_INDEX,
      SearchQueueListener.MESSAGES_TYPE,
      topic.getId.toString
    ).setField("title", "message", "tag")

    val result = query.execute().actionGet()

    result.getHits.map(hit => topicDao.getById(hit.getId.toInt)).toSeq
  }
}
