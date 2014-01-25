package ru.org.linux.search

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import ru.org.linux.topic.Topic
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders._
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.index.query.FilterBuilders._
import ru.org.linux.tag.TagRef
import scala.beans.BeanProperty
import ru.org.linux.util.StringUtil
import org.springframework.web.util.UriComponentsBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.ElasticSearchException

@Service
class MoreLikeThisService @Autowired() (
  client:Client
) extends Logging {
  // TODO async - return ListenableFuture
  def search(topic:Topic, tags:java.util.List[TagRef]):java.util.List[MoreLikeThisTopic] = {
    // TODO boost tags
    // see http://stackoverflow.com/questions/15300650/elasticsearch-more-like-this-api-vs-more-like-this-query

    // TODO message body

    val mltQuery = boolQuery()

    mltQuery.should(titleQuery(topic))

    if (!tags.isEmpty) {
      mltQuery.should(tagsQuery(tags.map(_.name).toSeq))
    }

    val rootFilter = boolFilter()
    rootFilter.must(termFilter("is_comment", "false"))
    rootFilter.mustNot(idsFilter(SearchQueueListener.MESSAGES_TYPE).addIds(topic.getId.toString))

    val rootQuery = filteredQuery(mltQuery, rootFilter)

    val query = client
      .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
      .setTypes(SearchQueueListener.MESSAGES_TYPE)
      .setQuery(rootQuery)
      .addFields("title", "postdate", "section", "group")

    resultsOrNothing(query.execute())
  }

  def resultsOrNothing(featureResult:ListenableActionFuture[SearchResponse]):java.util.List[MoreLikeThisTopic] = {
    try {
      val result = featureResult.actionGet(MoreLikeThisService.TimeoutMillis)

      result.getHits.map(processHit).toSeq
    } catch {
      case ex:ElasticSearchException =>
        logger.warn("Unable to find simular topics", ex)
        Seq()
    }
  }

  def processHit(hit: SearchHit): MoreLikeThisTopic = {
    val section = SearchResultsService.section(hit)
    val group = SearchResultsService.group(hit)

    val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
    val link = builder.buildAndExpand(section, group, new Integer(hit.getId)).toUriString

    val postdate = SearchResultsService.postdate(hit)

    val title = hit.getFields.get("title").getValue[String]

    MoreLikeThisTopic(
      title = StringUtil.processTitle(StringUtil.escapeHtml(title)),
      link = link,
      year = postdate.year().get()
    )
  }

  private def titleQuery(topic:Topic) = moreLikeThisFieldQuery("title")
    .likeText(topic.getTitleUnescaped)
    .minTermFreq(0)
    .minDocFreq(0)

  private def tagsQuery(tags:Seq[String]) = {
    val root = boolQuery()

    tags foreach { tag =>
      root.should(termQuery("tag", tag))
    }

    root
  }
}

object MoreLikeThisService {
  val TimeoutMillis = 500
}

case class MoreLikeThisTopic(
  @BeanProperty title:String,
  @BeanProperty link:String,
  @BeanProperty year:Int
)
