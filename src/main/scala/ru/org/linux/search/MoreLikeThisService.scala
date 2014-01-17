package ru.org.linux.search

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import ru.org.linux.topic.{TopicDao, Topic}
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders._
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.index.query.FilterBuilders._

@Service
class MoreLikeThisService @Autowired() (
  client:Client,
  topicDao:TopicDao
) extends Logging {
  // TODO async - return ListenableFuture
  // TODO timeout
  def search(topic:Topic):java.util.List[Topic] = {
    // TODO boost tags
    // see http://stackoverflow.com/questions/15300650/elasticsearch-more-like-this-api-vs-more-like-this-query

    // TODO unescape title
    val titleQuery = moreLikeThisFieldQuery("title")
      .likeText(topic.getTitleUnescaped)
      .minTermFreq(0)
      .minDocFreq(0)

    // TODO tags
    // TODO message body

    // TODO filter out same topic

    val mltQuery = boolQuery()

    mltQuery.should(titleQuery)

    val rootQuery = filteredQuery(mltQuery, termFilter("is_comment", "false"))

    val query = client
      .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
      .setTypes(SearchQueueListener.MESSAGES_TYPE)
      .setQuery(rootQuery)

    val result = query.execute().actionGet()

    // TODO filter out ids with MessageNotFoundException
    result.getHits.map(hit => topicDao.getById(hit.getId.toInt)).toSeq
  }
}
