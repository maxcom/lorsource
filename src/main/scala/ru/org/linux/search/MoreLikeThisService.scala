package ru.org.linux.search

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import ru.org.linux.topic.Topic
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.elasticsearch.index.query.QueryBuilders._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.elasticsearch.index.query.FilterBuilders._
import ru.org.linux.tag.TagRef
import scala.beans.BeanProperty
import ru.org.linux.util.StringUtil
import org.springframework.web.util.UriComponentsBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.ElasticsearchException
import scala.concurrent.{TimeoutException, Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import ru.org.linux.section.SectionService
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

@Service
class MoreLikeThisService @Autowired() (
  client:Client,
  sectionService:SectionService
) extends StrictLogging {
  import MoreLikeThisService._

  type Result = java.util.List[java.util.List[MoreLikeThisTopic]]

  private val cache = CacheBuilder
    .newBuilder()
    .maximumSize(CacheSize)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build[Integer, Result]()

  def search(topic:Topic, tags:java.util.List[TagRef], plainText:String):Future[Result] = {
    val cachedValue = cache.getIfPresent(topic.getId)
    if (cachedValue != null) {
      Future.successful(cachedValue)
    } else {
      val promise = Promise[SearchResponse]()

      try {
        val query = makeQuery(topic, plainText, tags)

        query.execute().addListener(new ActionListener[SearchResponse]() {
          override def onFailure(e: Throwable): Unit = promise.failure(e)
          override def onResponse(response: SearchResponse): Unit = promise.success(response)
        })

        val result:Future[Result] = promise.future.map(result => if (result.getHits.nonEmpty) {
          val half = result.getHits.size / 2 + result.getHits.size % 2

          result.getHits.map(processHit).grouped(half).map(_.toSeq.asJava).toSeq.asJava
        } else Seq())

        result.onSuccess {
          case v => cache.put(topic.getId, v)
        }

        result
      } catch {
        case ex: ElasticsearchException => Future.failed(ex)
      }
    }
  }

  def makeQuery(topic: Topic, plainText: String, tags: java.util.List[TagRef]):SearchRequestBuilder = {
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

    client
      .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
      .setTypes(SearchQueueListener.MESSAGES_TYPE)
      .setQuery(rootQuery)
      .addFields("title", "postdate", "section", "group")
  }

  def resultsOrNothing(featureResult:Future[Result]):Result = {
    try {
      Await.result(featureResult, Timeout)
    } catch {
      case ex:ElasticsearchException =>
        logger.warn("Unable to find similar topics", ex)
        Seq()
      case ex:TimeoutException =>
        logger.warn("Similar topics lookup timed out", ex)
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
  val CacheSize = 2000
}

case class MoreLikeThisTopic(
  @BeanProperty title:String,
  @BeanProperty link:String,
  @BeanProperty year:Int,
  @BeanProperty section:String
)
