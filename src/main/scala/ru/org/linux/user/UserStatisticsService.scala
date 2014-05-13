package ru.org.linux.user

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Timestamp
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import ru.org.linux.section.{Section, SectionService}
import org.elasticsearch.client.Client
import ru.org.linux.search.SearchQueueListener
import org.elasticsearch.action.search.{SearchResponse, SearchType}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import scala.concurrent._
import org.elasticsearch.action.ActionListener
import scala.concurrent.duration._
import UserStatisticsService._
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.ElasticsearchException
import com.typesafe.scalalogging.slf4j.Logging

@Service
class UserStatisticsService @Autowired() (
  userDao: UserDao,
  ignoreListDao: IgnoreListDao,
  sectionService: SectionService,
  elastic: Client
) extends Logging {
  def getStats(user:User) : UserStats = {
    val commentCountFuture = countComments(user)

    val ignoreCount = ignoreListDao.getIgnoreStat(user)
    val (firstComment, lastComment) = userDao.getFirstAndLastCommentDate(user)
    val (firstTopic, lastTopic) = userDao.getFirstAndLastTopicDate(user)

    val topicsBySection = userDao.getSectionStats(user).map(
      e => PreparedUsersSectionStatEntry(sectionService.getSection(e.getSection), e.getCount)
    )

    val (commentCount, incomplete) = try {
      (Await.result(commentCountFuture, ElasticTimeout), false)
    } catch {
      case ex:ElasticsearchException =>
        logger.warn("Unable to count comments", ex)
        (0L, true)
      case ex:TimeoutException =>
        logger.warn("Comment count lookup timed out", ex)
        (0L, true)
    }

    new UserStats(
      ignoreCount,
      commentCount,
      incomplete,
      firstComment,
      lastComment,
      firstTopic,
      lastTopic,
      topicsBySection
    )
  }

  def countComments(user:User) = {
    import QueryBuilders._
    import FilterBuilders._

    val filter = boolFilter()
    filter.must(termFilter("author", user.getNick))
    filter.must(termFilter("is_comment", true))

    val root = filteredQuery(matchAllQuery(), filter)

    val promise = Promise[Long]()

    try {
      elastic
        .prepareSearch(SearchQueueListener.MESSAGES_INDEX)
        .setSearchType(SearchType.COUNT)
        .setQuery(root)
        .setTimeout(TimeValue.timeValueMillis(ElasticTimeout.toMillis))
        .execute(new ActionListener[SearchResponse] {
        override def onFailure(e: Throwable): Unit = promise.failure(e)

        override def onResponse(response: SearchResponse): Unit = {
          if (response.isTimedOut) {
            promise.failure(new RuntimeException("ES Request timed out"))
          } else {
            promise.success(response.getHits.getTotalHits)
          }
        }
      })

      promise.future
    } catch {
      case ex:ElasticsearchException => Future.failed(ex)
    }
  }
}

object UserStatisticsService {
  val ElasticTimeout = 1.second
}

case class UserStats (
  @BeanProperty ignoreCount: Int,
  @BeanProperty commentCount: Long,
  @BeanProperty incomplete: Boolean,
  @BeanProperty firstComment: Timestamp,
  @BeanProperty lastComment: Timestamp,
  @BeanProperty firstTopic: Timestamp,
  @BeanProperty lastTopic: Timestamp,
  @BeanProperty topicsBySection: java.util.List[PreparedUsersSectionStatEntry]
)

case class PreparedUsersSectionStatEntry (
  @BeanProperty section: Section,
  @BeanProperty count: Int
)