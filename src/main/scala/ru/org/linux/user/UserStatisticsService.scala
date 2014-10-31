package ru.org.linux.user

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Timestamp
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import ru.org.linux.section.{Section, SectionService}
import org.elasticsearch.client.Client
import ru.org.linux.search.SearchQueueListener
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import scala.concurrent._
import org.elasticsearch.action.ActionListener
import scala.concurrent.duration._
import UserStatisticsService._
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.ElasticsearchException
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import QueryBuilders._
import FilterBuilders._
import org.elasticsearch.search.aggregations.AggregationBuilders._
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.util.{Try, Success, Failure}
import java.util.Date
import org.elasticsearch.search.aggregations.metrics.stats.Stats

@Service
class UserStatisticsService @Autowired() (
  userDao: UserDao,
  ignoreListDao: IgnoreListDao,
  sectionService: SectionService,
  elastic: Client
) extends StrictLogging {
  def getStats(user:User) : UserStats = {
    val commentCountFuture = countComments(user)
    val topicsFuture = topicStats(user)

    val ignoreCount = ignoreListDao.getIgnoreStat(user)
    val (firstComment, lastComment) = userDao.getFirstAndLastCommentDate(user)

    try {
      Await.ready(Future.sequence(Seq(commentCountFuture, topicsFuture)), ElasticTimeout)
    } catch {
      case _:TimeoutException =>
        logger.warn("Stat lookup timed out")
    }

    val commentCount = extractValue(commentCountFuture.value) {
        logger.warn("Unable to count comments", _)
    }

    val topicStat = extractValue(topicsFuture.value) {
        logger.warn("Unable to count topics", _)
    }

    val topicsBySection = topicStat.map(_.sectionCount).getOrElse(Seq()).map(
      e => PreparedUsersSectionStatEntry(sectionService.getSectionByName(e._1), e._2)
    ).sortBy(_.section.getId)

    new UserStats(
      ignoreCount,
      commentCount.getOrElse(0L),
      commentCount.isEmpty || topicStat.isEmpty,
      firstComment,
      lastComment,
      topicStat.flatMap(_.firstTopic).map(_.toDate).orNull,
      topicStat.flatMap(_.lastTopic).map(_.toDate).orNull,
      topicsBySection
    )
  }

  private def countComments(user:User):Future[Long] = {
    val filter = boolFilter()
    filter.must(termFilter("author", user.getNick))
    filter.must(termFilter("is_comment", true))

    val root = filteredQuery(matchAllQuery(), filter)

    try {
      elastic
        .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
        .setSearchType(SearchType.COUNT)
        .setQuery(root)
        .setTimeout(TimeValue.timeValueMillis(ElasticTimeout.toMillis))
        .scalaExecute(failOnTimeout = true)
        .map(_.getHits.getTotalHits)
    } catch {
      case ex:ElasticsearchException => Future.failed(ex)
    }
  }

  private def topicStats(user:User):Future[TopicStats] = {
    val filter = boolFilter()

    filter.must(termFilter("author", user.getNick))
    filter.must(termFilter("is_comment", false))

    val root = filteredQuery(matchAllQuery(), filter)

    val topicStatsAgg = stats("topic_stats").field("postdate")
    val sectionsAgg = terms("sections").field("section")

    try {
      elastic
        .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
        .setSearchType(SearchType.COUNT)
        .setQuery(root)
        .addAggregation(topicStatsAgg)
        .addAggregation(sectionsAgg)
        .setTimeout(TimeValue.timeValueMillis(ElasticTimeout.toMillis))
        .scalaExecute(failOnTimeout = true)
        .map { response =>
          val topicStatsResult:Stats = response.getAggregations.get("topic_stats")
          val sectionsResult:Terms = response.getAggregations.get("sections")

          val (firstTopic, lastTopic) = if (topicStatsResult.getCount>0) {
            (Some(new DateTime(topicStatsResult.getMin.toLong)), Some(new DateTime(topicStatsResult.getMax.toLong)))
          } else {
            (None, None)
          }

          val sections = sectionsResult.getBuckets.map { bucket =>
            (bucket.getKeyAsText.string(), bucket.getDocCount)
          }.toSeq

          TopicStats(firstTopic, lastTopic, sections)
        }
    } catch {
      case ex:ElasticsearchException => Future.failed(ex)
    }
  }
}

object UserStatisticsService {
  val ElasticTimeout = 1.second

  implicit class RichSearchRequestBuilder(request : SearchRequestBuilder) {
    def scalaExecute(failOnTimeout : Boolean = false):Future[SearchResponse] = {

      try {
        val promise = Promise[SearchResponse]()

        request.execute(new ActionListener[SearchResponse] {
          override def onFailure(e: Throwable): Unit = promise.failure(e)

          override def onResponse(response: SearchResponse): Unit = {
            if (response.isTimedOut && failOnTimeout) {
              promise.failure(new RuntimeException("ES Request timed out"))
            } else {
              promise.success(response)
            }
          }
        })

        promise.future
      } catch {
        case ex:ElasticsearchException => Future.failed(ex)
      }
    }
  }

  private def extractValue[T](value:Option[Try[T]])(f:(Throwable => Unit)):Option[T] = {
    value flatMap {
      case Failure(ex) =>
        f(ex)
        None
      case Success(count) =>
        Some(count)
    }
  }

  case class TopicStats(firstTopic:Option[DateTime], lastTopic:Option[DateTime], sectionCount:Seq[(String, Long)])
}

case class UserStats (
  @BeanProperty ignoreCount: Int,
  @BeanProperty commentCount: Long,
  @BeanProperty incomplete: Boolean,
  @BeanProperty firstComment: Timestamp,
  @BeanProperty lastComment: Timestamp,
  @BeanProperty firstTopic: Date,
  @BeanProperty lastTopic: Date,
  @BeanProperty topicsBySection: java.util.List[PreparedUsersSectionStatEntry]
)

case class PreparedUsersSectionStatEntry (
  @BeanProperty section: Section,
  @BeanProperty count: Long
)
