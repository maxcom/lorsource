package ru.org.linux.search

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.elasticsearch.search.SearchHit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.search.SearchQueueListener.{COLUMN_TOPIC_AWAITS_COMMIT, MESSAGES_INDEX, MESSAGES_TYPE}
import ru.org.linux.section.SectionService
import ru.org.linux.tag.TagRef
import ru.org.linux.topic.Topic
import ru.org.linux.util.StringUtil

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}

@Service
class MoreLikeThisService @Autowired() (
  client:Client,
  sectionService:SectionService
) extends StrictLogging {
  import ru.org.linux.search.MoreLikeThisService._

  type Result = java.util.List[java.util.List[MoreLikeThisTopic]]

  private val elastic = ElasticClient.fromClient(client)

  private val cache = CacheBuilder
    .newBuilder()
    .maximumSize(CacheSize)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build[Integer, Result]()

  def searchSimilar(topic:Topic, tags:java.util.List[TagRef], plainText:String):Future[Result] = {
    val cachedValue = cache.getIfPresent(topic.getId)
    if (cachedValue != null) {
      Future.successful(cachedValue)
    } else {
      try {
        val searchResult = elastic execute makeQuery(topic, plainText, tags)

        val result:Future[Result] = searchResult.map(result => if (result.getHits.nonEmpty) {
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

  private def makeQuery(topic: Topic, plainText: String, tags: Seq[TagRef]) = {
    val tagsQ = if (tags.nonEmpty) {
      Seq(tagsQuery(tags.map(_.name)))
    } else Seq.empty

    val queries = Seq(titleQuery(topic), textQuery(plainText)) ++ tagsQ

    val rootFilter = bool {
      must(
        termFilter("is_comment", "false"),
        termFilter(COLUMN_TOPIC_AWAITS_COMMIT, "false")
      ) not idsFilter(topic.getId.toString)
    }

    search in MESSAGES_INDEX -> MESSAGES_TYPE query {
      filteredQuery filter rootFilter query bool { should(queries:_*) }
    } fields("title", "postdate", "section", "group")
  }

  def resultsOrNothing(featureResult:Future[Result], deadline:Deadline):Result = {
    try {
      Await.result(featureResult, deadline.timeLeft)
    } catch {
      case ex:ElasticsearchException =>
        logger.warn("Unable to find similar topics", ex)
        Seq()
      case ex:TimeoutException =>
        logger.warn(s"Similar topics lookup timed out (${ex.getMessage})")
        Seq()
    }
  }

  private def processHit(hit: SearchHit): MoreLikeThisTopic = {
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

  private def titleQuery(topic:Topic) =
    morelikeThisQuery("title") likeText topic.getTitleUnescaped minTermFreq 0 minDocFreq 0 maxDocFreq 20000

  private def textQuery(plainText:String) =
    morelikeThisQuery("message") likeText plainText maxDocFreq 50000 minTermFreq 1

  private def tagsQuery(tags:Seq[String]) = termsQuery("tag", tags:_*)
}

object MoreLikeThisService {
  val CacheSize = 2000
}

case class MoreLikeThisTopic(
  @BeanProperty title:String,
  @BeanProperty link:String,
  @BeanProperty year:Int,
  @BeanProperty section:String
)
