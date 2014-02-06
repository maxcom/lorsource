package ru.org.linux.search

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import ru.org.linux.topic.Topic
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.elasticsearch.index.query.QueryBuilders._
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.index.query.FilterBuilders._
import ru.org.linux.tag.TagRef
import scala.beans.BeanProperty
import ru.org.linux.util.StringUtil
import org.springframework.web.util.UriComponentsBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.ElasticSearchException
import scala.concurrent.{TimeoutException, Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import ru.org.linux.section.SectionService

@Service
class MoreLikeThisService @Autowired() (
  client:Client,
  sectionService:SectionService
) extends Logging {
  def search(topic:Topic, tags:java.util.List[TagRef], plainText:String):Future[java.util.List[java.util.List[MoreLikeThisTopic]]] = {
    // TODO boost tags
    // see http://stackoverflow.com/questions/15300650/elasticsearch-more-like-this-api-vs-more-like-this-query

    val mltQuery = boolQuery()

    mltQuery.should(titleQuery(topic))
    mltQuery.should(textQuery(plainText))

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

    val promise = Promise[SearchResponse]()

    try {
      query.execute().addListener(new ActionListener[SearchResponse]() {
        override def onFailure(e: Throwable): Unit = promise.failure(e)
        override def onResponse(response: SearchResponse): Unit = promise.success(response)
      })

      promise.future.map(result => if (result.getHits.nonEmpty) {
        val half = result.getHits.size/2 + result.getHits.size%2

        result.getHits.map(processHit).grouped(half).map(_.toSeq.asJava).toSeq.asJava
      } else Seq())
    } catch {
      case ex:ElasticSearchException => Future.failed(ex)
    }
  }

  def resultsOrNothing(featureResult:Future[java.util.List[java.util.List[MoreLikeThisTopic]]]):java.util.List[java.util.List[MoreLikeThisTopic]] = {
    try {
      Await.result(featureResult, MoreLikeThisService.Timeout)
    } catch {
      case ex:ElasticSearchException =>
        logger.warn("Unable to find simular topics", ex)
        Seq()
      case ex:TimeoutException =>
        logger.warn("Simular topics lookup timed out", ex)
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
      year = postdate.year().get(),
      sectionService.getSectionByName(section).getTitle
    )
  }

  private def titleQuery(topic:Topic) = moreLikeThisFieldQuery("title")
    .likeText(topic.getTitleUnescaped)
    .minTermFreq(0)
    .minDocFreq(0)
    .maxDocFreq(20000)

  private def textQuery(plainText:String) = moreLikeThisFieldQuery("message")
    .likeText(plainText)
    .maxDocFreq(50000)
    .minTermFreq(1)

  private def tagsQuery(tags:Seq[String]) = {
    val root = boolQuery()

    tags foreach { tag =>
      root.should(termQuery("tag", tag))
    }

    root
  }
}

object MoreLikeThisService {
  val Timeout = 500.milliseconds
}

case class MoreLikeThisTopic(
  @BeanProperty title:String,
  @BeanProperty link:String,
  @BeanProperty year:Int,
  @BeanProperty section:String
)
