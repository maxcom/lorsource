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

package ru.org.linux.search

import com.google.common.cache.CacheBuilder
import com.typesafe.scalalogging.StrictLogging
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.pekko.actor.Scheduler
import org.apache.pekko.pattern.{CircuitBreaker, CircuitBreakerOpenException}
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.*
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.search.{Hit, SourceConfig, SourceFilter}
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.search.OpenSearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.SectionService
import ru.org.linux.tag.TagRef
import ru.org.linux.topic.Topic
import ru.org.linux.util.StringUtil

import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit
import scala.beans.BeanProperty
import scala.collection.Seq as MSeq
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, TimeoutException}
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.util.control.NonFatal

@Service
class MoreLikeThisService(
  client: OpenSearchAsyncClient,
  sectionService: SectionService,
  scheduler: Scheduler
) extends StrictLogging {
  import MoreLikeThisService.*

  private type Result = java.util.List[java.util.List[MoreLikeThisTopic]]

  private val cache = CacheBuilder
    .newBuilder()
    .maximumSize(CacheSize)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build[Integer, Result]()

  private val breaker = new CircuitBreaker(
    scheduler = scheduler,
    maxFailures = 5,
    callTimeout = 2.seconds,
    resetTimeout = 1.minute
  )

  breaker.onOpen { logger.warn("Similar topics lookup disabled") }
  breaker.onClose { logger.warn("Similar topics lookup enabled") }

  def searchSimilar(topic: Topic, tags: MSeq[TagRef]): Future[Result] = {
    val cachedValue = Option(cache.getIfPresent(topic.id))

    cachedValue.map(Future.successful).getOrElse {
      breaker.withCircuitBreaker {
        val request = makeQuery(topic, tags)

        val result: Future[Result] = client.search(request, classOf[MessageIndexDocument])
          .asScala
          .map { response =>
            val hits = response.hits.hits.asScala
            if (hits.nonEmpty) {
              val half = hits.size / 2 + hits.size % 2

              val grouped: Iterator[Vector[MoreLikeThisTopic]] = hits.map(processHit).grouped(half).map(_.toVector)
              val listOfLists: Result = grouped.map(_.asJava).toVector.asJava

              listOfLists
            } else {
              Seq().asJava
            }
          }

        result.foreach { v =>
          cache.put(topic.id, v)
        }

        result
      }
    }
  }

  private def makeQuery(topic: Topic, tags: MSeq[TagRef]): SearchRequest = {
    val tagsQ = if (tags.nonEmpty) {
      Seq(tagsQuery(tags.map(_.name)))
    } else Seq.empty

    val queries = Seq(titleQuery(topic), textQuery(topic.id)) ++ tagsQ

    val filterQueries = Seq(
      Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
      Query.of(q => q.term(TermQuery.of(t => t.field(COLUMN_TOPIC_AWAITS_COMMIT).value(FieldValue.of("false")))))
    )

    new SearchRequest.Builder()
      .index(MessageIndex)
      .query(Query.of(q => q
        .bool(BoolQuery.of(b => {
          queries.foreach(q => b.should(q))
          b.filter(filterQueries.asJava)
          b.minimumShouldMatch("1")
          b.mustNot(Query.of(q => q.ids(ids => ids.values(topic.id.toString))))
        })
      )))
      .source(new SourceConfig.Builder()
        .filter(new SourceFilter.Builder()
          .includes(java.util.List.of("title", "postdate", "section", "group"))
          .build())
        .build())
      .build()
  }

  def resultsOrNothing(topic: Topic, featureResult: Future[Result], deadline: Deadline): Result = {
    Option(cache.getIfPresent(topic.id)).getOrElse {
      try {
        Await.result(featureResult, deadline.timeLeft)
      } catch {
        case _: CircuitBreakerOpenException =>
          logger.debug("Similar topics circuit breaker is open")
          Option(cache.getIfPresent(topic.id)).getOrElse(Seq().asJava)
        case ex: TimeoutException =>
          logger.warn(s"Similar topics lookup timed out (${ex.getMessage})")
          Option(cache.getIfPresent(topic.id)).getOrElse(Seq().asJava)
        case NonFatal(ex) =>
          logger.warn("Unable to find similar topics", ex)
          Option(cache.getIfPresent(topic.id)).getOrElse(Seq().asJava)
      }
    }
  }

  private def processHit(hit: Hit[MessageIndexDocument]): MoreLikeThisTopic = {
    val source = hit.source
    val section = source.section
    val group = source.group

    val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
    val link = builder.buildAndExpand(section, group, Integer.parseInt(hit.id)).toUriString

    val postdate = Instant.parse(source.postdate)

    val title = source.title.getOrElse("")

    MoreLikeThisTopic(
      title = StringUtil.processTitle(StringUtil.escapeHtml(title)),
      link = link,
      year = postdate.atZone(ZoneId.systemDefault()).getYear,
      sectionService.getSectionByName(section).getTitle)
  }

  private def titleQuery(topic: Topic): Query = {
    Query.of(q => q
      .moreLikeThis(MoreLikeThisQuery.of(m => m
        .fields("title")
        .like(Like.of(l => l.text(topic.getTitleUnescaped)))
        .minTermFreq(1)
        .minDocFreq(2)
        .stopWords(StopWords.asJava)
        .maxDocFreq(5000)
      ))
    )
  }

  private def textQuery(id: Int): Query = {
    Query.of(q => q
      .moreLikeThis(MoreLikeThisQuery.of(m => m
        .fields("message")
        .like(Like.of(l => l.document(d => d.index(MessageIndex).id(id.toString))))
        .minTermFreq(1)
        .stopWords(StopWords.asJava)
        .minWordLength(3)
        .maxDocFreq(100000)
      ))
    )
  }

  private def tagsQuery(tags: MSeq[String]): Query = {
    Query.of(q => q
      .terms(TermsQuery.of(t => t
        .field("tag")
        .terms(ts => ts.value(tags.map(v => FieldValue.of(v)).asJava))
      ))
    )
  }
}

object MoreLikeThisService {
  private val CacheSize = 10000

  private val StopWords: Seq[String] = {
    val stop = RussianAnalyzer.getDefaultStopSet.asScala.map(arr => new String(arr.asInstanceOf[Array[Char]]))
    val analyzedStream = new RussianAnalyzer(CharArraySet.EMPTY_SET).tokenStream(null, stop.mkString(" "))

    analyzedStream.reset()

    val b = new ArrayBuffer[String](initialSize = stop.size)

    while (analyzedStream.incrementToken()) {
      b += analyzedStream.getAttribute(classOf[CharTermAttribute]).toString
    }

    b.toVector
  }

}

case class MoreLikeThisTopic(
  @BeanProperty title: String,
  @BeanProperty link: String,
  @BeanProperty year: Int,
  @BeanProperty section: String
)
