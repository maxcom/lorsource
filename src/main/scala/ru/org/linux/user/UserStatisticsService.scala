/*
 * Copyright 1998-2019 Linux.org.ru
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
import java.util.concurrent.CompletionStage

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.DateHistogramInterval
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.ElasticsearchException
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.search.ElasticsearchIndexService.MessageIndex
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.user.UserStatisticsService._

import scala.beans.BeanProperty
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@Service
class UserStatisticsService(
  userDao: UserDao,
  ignoreListDao: IgnoreListDao,
  sectionService: SectionService,
  elastic: ElasticClient
) extends StrictLogging {
  def getStats(user: User): UserStats = {
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

  def getYearStats(user: User): CompletionStage[java.util.Map[Long, Long]] = {
    Future.successful(elastic).flatMap {
      _ execute {
        val root = boolQuery().filter(termQuery("author", user.getNick), rangeQuery("postdate").gt("now-1y/d"))

        search(MessageIndex) size 0 timeout 30.seconds query root aggs
          dateHistogramAgg("days", "postdate").interval(DateHistogramInterval.days(1))
      } map {
        _.result.aggregations.dateHistogram("days").buckets.map { bucket =>
          bucket.timestamp/1000 -> bucket.docCount
        }.toMap.asJava
      }
    }.toJava
  }

  private def timeoutHandler(response: SearchResponse): Future[SearchResponse] = {
    if (response.isTimedOut) {
      Future failed new RuntimeException("ES Request timed out")
    } else {
      Future successful response
    }
  }

  private def statSearch = search(MessageIndex) size 0 timeout ElasticTimeout

  private def countComments(user: User): Future[Long] = {
    try {
      elastic execute {
        val root = boolQuery() filter (
              termQuery("author", user.getNick),
              termQuery("is_comment", true))

        statSearch query root
      } map (_.result) flatMap timeoutHandler map { _.totalHits }
    } catch {
      case ex: ElasticsearchException => Future.failed(ex)
    }
  }

  private def topicStats(user: User): Future[TopicStats] = {
    try {
      elastic execute {
        val root = boolQuery().filter (
          termQuery("author", user.getNick),
          termQuery("is_comment", false))

        statSearch query root aggs(
          statsAggregation("topic_stats") field "postdate",
          termsAggregation("sections") field "section")
      } map(_.result) flatMap timeoutHandler map { response =>
        // workaround https://github.com/sksamuel/elastic4s/issues/1614
        val topicStatsResult = Try(response.aggregations.statsBucket("topic_stats")).toOption
        val sectionsResult = response.aggregations.terms("sections")

        val (firstTopic, lastTopic) = if (topicStatsResult.exists(_.count > 0)) {
          (Some(new DateTime(topicStatsResult.get.min.toLong)), Some(new DateTime(topicStatsResult.get.max.toLong)))
        } else {
          (None, None)
        }

        val sections = sectionsResult.buckets.map { bucket =>
          (bucket.key, bucket.docCount)
        }

        TopicStats(firstTopic, lastTopic, sections)
      }
    } catch {
      case ex: ElasticsearchException => Future.failed(ex)
    }
  }
}

object UserStatisticsService {
  val ElasticTimeout: FiniteDuration = 5.seconds

  private def extractValue[T](value:Option[Try[T]])(f: Throwable => Unit): Option[T] = {
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
