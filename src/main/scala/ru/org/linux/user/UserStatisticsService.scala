/*
 * Copyright 1998-2026 Linux.org.ru
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

import org.apache.pekko.actor.ActorSystem
import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.{CountRequest, SearchRequest, SearchResponse}
import org.opensearch.client.opensearch._types.aggregations.{CalendarInterval, DateHistogramAggregation, StatsAggregation, TermsAggregation}
import org.opensearch.client.opensearch._types.query_dsl.{BoolQuery, Query, RangeQuery, TermQuery}
import org.springframework.stereotype.Service
import ru.org.linux.search.OpenSearchIndexService.MessageIndex
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.user.UserStatisticsService.*
import ru.org.linux.util.RichFuture.RichFuture

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import java.util.Date
import scala.beans.BeanProperty
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

@Service
class UserStatisticsService(userDao: UserDao, ignoreListDao: IgnoreListDao, sectionService: SectionService,
                            elastic: OpenSearchAsyncClient, actorSystem: ActorSystem) extends StrictLogging {
  private given ActorSystem = actorSystem

  def getStats(user: User): Future[UserStats] = {
    val deadline = SearchTimeout.fromNow

    val commentCountFuture = countComments(user).map(Some.apply).withTimeout(deadline.timeLeft).recover { ex =>
      logger.warn("Unable to count comments", ex)
      None
    }

    val topicsFuture = topicStats(user).map(Some.apply).withTimeout(deadline.timeLeft).recover { ex =>
      logger.warn("Unable to count topics", ex)
      None
    }

    val ignoreCount = ignoreListDao.getIgnoreCount(user)
    val (firstComment, lastComment) = userDao.getFirstAndLastCommentDate(user)

    (commentCountFuture, topicsFuture).mapN { (commentCount, topicStat) =>
      val topicsBySection = topicStat.map(_.sectionCount).getOrElse(Seq()).map(
        e => PreparedUsersSectionStatEntry(sectionService.getSectionByName(e._1), e._2)
      ).sortBy(_.section.id)

      UserStats(
        ignoreCount = ignoreCount,
        commentCount = commentCount.getOrElse(0L),
        incomplete = commentCount.isEmpty || topicStat.isEmpty,
        firstComment = firstComment,
        lastComment = lastComment,
        firstTopic = topicStat.flatMap(_.firstTopic).map(Date.from).orNull,
        lastTopic = topicStat.flatMap(_.lastTopic).map(Date.from).orNull,
        topicsBySection = topicsBySection.asJava)
    }
  }

  def getYearStats(user: User, timezone: ZoneId): Future[Map[Long, Long]] = {
    val rootQuery = Query.of(q => q.bool(BoolQuery.of(b => b
      .filter(
        Query.of(qq => qq.term(TermQuery.of(t => t.field("author").value(FieldValue.of(user.nick))))),
        Query.of(qq => qq.range(RangeQuery.of(r => r.field("postdate").gt(JsonData.of("now-1y/M")))))
      )
    )))

    val request = new SearchRequest.Builder()
      .index(MessageIndex)
      .size(0)
      .query(rootQuery)
      .aggregations("days", a => a
        .dateHistogram(DateHistogramAggregation.of(d => d
          .field("postdate")
          .timeZone(timezone.getId)
          .calendarInterval(CalendarInterval.Day)
          .minDocCount(1)
        ))
      )
      .build()

    elastic.search(request, classOf[Void])
      .asScala
      .map { response =>
        val buckets = response.aggregations.get("days").dateHistogram().buckets.array().asScala
        buckets.map(b => b.key/1000 -> b.docCount).toMap
      }
  }

  private def timeoutHandler(response: SearchResponse[Void]): Future[SearchResponse[Void]] = {
    if (response.timedOut) {
      Future.failed(new RuntimeException("ES Request timed out"))
    } else {
      Future.successful(response)
    }
  }

  private def countComments(user: User): Future[Long] = {
    val rootQuery = Query.of(q => q.bool(BoolQuery.of(b => b
      .filter(
        Query.of(qq => qq.term(TermQuery.of(t => t.field("author").value(FieldValue.of(user.nick))))),
        Query.of(qq => qq.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of(true)))))
      )
    )))

    val request = new CountRequest.Builder()
      .index(MessageIndex)
      .query(rootQuery)
      .build()

    elastic.count(request)
      .asScala
      .map(_.count())
  }

  private def topicStats(user: User): Future[TopicStats] = {
    val rootQuery = Query.of(q => q.bool(BoolQuery.of(b => b
      .filter(
        Query.of(qq => qq.term(TermQuery.of(t => t.field("author").value(FieldValue.of(user.nick))))),
        Query.of(qq => qq.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of(false)))))
      )
    )))

    val request = new SearchRequest.Builder()
    .index(MessageIndex)
    .size(0)
    .timeout(formatTimeout(SearchTimeout))
      .query(rootQuery)
      .aggregations("topic_stats", a => a.stats(StatsAggregation.of(s => s.field("postdate"))))
      .aggregations("sections", a => a.terms(TermsAggregation.of(t => t.field("section").size(1000))))
      .build()

    elastic.search(request, classOf[Void])
      .asScala
      .flatMap(timeoutHandler)
      .map { response =>
        val topicStatsResult = Option(response.aggregations.get("topic_stats").stats())
        val sectionsResult = response.aggregations.get("sections").sterms()

        val (firstTopic, lastTopic) = if (topicStatsResult.exists(_.count() > 0)) {
          (Some(Instant.ofEpochMilli(topicStatsResult.get.min().toLong)), Some(Instant.ofEpochMilli(topicStatsResult.get.max().toLong)))
        } else {
          (None, None)
        }

        val sections = sectionsResult.buckets.array().asScala.map { bucket =>
          (bucket.key(), bucket.docCount())
        }

        TopicStats(firstTopic, lastTopic, sections.toSeq)
      }
  }
}

object UserStatisticsService:
  private val SearchTimeout: FiniteDuration = 5.seconds

  private def formatTimeout(timeout: FiniteDuration): String = s"${timeout.toSeconds}s"

  private case class TopicStats(firstTopic: Option[Instant], lastTopic: Option[Instant], sectionCount: Seq[(String, Long)])

case class UserStats (
  @BeanProperty ignoreCount: Int,
  @BeanProperty commentCount: Long,
  @BeanProperty incomplete: Boolean,
  @BeanProperty firstComment: Timestamp,
  @BeanProperty lastComment: Timestamp,
  @BeanProperty firstTopic: Date,
  @BeanProperty lastTopic: Date,
  @BeanProperty topicsBySection: java.util.List[PreparedUsersSectionStatEntry])

case class PreparedUsersSectionStatEntry(
  @BeanProperty section: Section,
  @BeanProperty count: Long)
