/*
 * Copyright 1998-2017 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user

import java.sql.Timestamp
import java.util.Date

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.metrics.stats.Stats
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.search.ElasticsearchIndexService.MessageIndexTypes
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.user.UserStatisticsService._

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@Service
class UserStatisticsService(
  userDao: UserDao,
  ignoreListDao: IgnoreListDao,
  sectionService: SectionService,
  elastic: TcpClient
) extends StrictLogging {
  def getStats(user:User): UserStats = {
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

    UserStats(
      ignoreCount,
      commentCount.getOrElse(0L),
      commentCount.isEmpty || topicStat.isEmpty,
      firstComment,
      lastComment,
      topicStat.flatMap(_.firstTopic).map(_.toDate).orNull,
      topicStat.flatMap(_.lastTopic).map(_.toDate).orNull,
      topicsBySection.asJava
    )
  }

  private def timeoutHandler(response: RichSearchResponse): Future[RichSearchResponse] = {
    if (response.isTimedOut) {
      Future failed new RuntimeException("ES Request timed out")
    } else {
      Future successful response
    }
  }

  private def statSearch = search(MessageIndexTypes) size 0 timeout ElasticTimeout

  private def countComments(user: User): Future[Long] = {
    try {
      elastic execute {
        val root = boolQuery() filter (
              termQuery("author", user.getNick),
              termQuery("is_comment", true))

        statSearch query root
      } flatMap timeoutHandler map { _.totalHits }
    } catch {
      case ex: ElasticsearchException => Future.failed(ex)
    }
  }

  private def topicStats(user: User): Future[TopicStats] = {
    try {
      elastic execute {
        val root = new BoolQueryDefinition filter (
          termQuery("author", user.getNick),
          termQuery("is_comment", false))

        statSearch query root aggs(
          statsAggregation("topic_stats") field "postdate",
          termsAggregation("sections") field "section")
      } flatMap timeoutHandler map { response ⇒
        val topicStatsResult = response.aggregations.getAs[Stats]("topic_stats")
        val sectionsResult = response.aggregations.getAs[Terms]("sections")

        val (firstTopic, lastTopic) = if (topicStatsResult.getCount > 0) {
          (Some(new DateTime(topicStatsResult.getMin.toLong)), Some(new DateTime(topicStatsResult.getMax.toLong)))
        } else {
          (None, None)
        }

        val sections = sectionsResult.getBuckets.asScala.map { bucket =>
          (bucket.getKeyAsString, bucket.getDocCount)
        }

        TopicStats(firstTopic, lastTopic, sections)
      }
    } catch {
      case ex: ElasticsearchException => Future.failed(ex)
    }
  }
}

object UserStatisticsService {
  val ElasticTimeout = 1.second

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
