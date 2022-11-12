/*
 * Copyright 1998-2022 Linux.org.ru
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

import java.util.concurrent.TimeUnit
import akka.actor.Scheduler
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException}
import com.google.common.cache.CacheBuilder
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.requests.common.DocumentRef
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.search.ElasticsearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.SectionService
import ru.org.linux.tag.TagRef
import ru.org.linux.topic.Topic
import ru.org.linux.util.StringUtil

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.control.NonFatal
import scala.collection.Seq as MSeq

@Service
class MoreLikeThisService(
  elastic: ElasticClient,
  sectionService: SectionService,
  scheduler: Scheduler
) extends StrictLogging {
  import ru.org.linux.search.MoreLikeThisService.*

  type Result = java.util.List[java.util.List[MoreLikeThisTopic]]

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

  breaker.onOpen { logger.warn("Similar topics circuit breaker is open, lookup disabled") }
  breaker.onClose { logger.warn("Similar topics circuit breaker is close, lookup enabled") }

  def searchSimilar(topic: Topic, tags: java.util.List[TagRef]): Future[Result] = {
    val cachedValue = Option(cache.getIfPresent(topic.getId))

    cachedValue.map(Future.successful).getOrElse {
      breaker.withCircuitBreaker {
        val searchResult = elastic execute makeQuery(topic, tags.asScala)

        val result: Future[Result] = searchResult.map(_.result).map(result => if (result.hits.nonEmpty) {
          val half = result.hits.hits.length / 2 + result.hits.hits.length % 2

          result.hits.hits.map(processHit).grouped(half).map(_.toVector.asJava).toVector.asJava
        } else Seq().asJava)

        result.foreach {
          v => cache.put(topic.getId, v)
        }

        result
      }
    }
  }

  private def makeQuery(topic: Topic, tags: MSeq[TagRef]) = {
    val tagsQ = if (tags.nonEmpty) {
      Seq(tagsQuery(tags.map(_.name)))
    } else Seq.empty

    val queries = Seq(titleQuery(topic), textQuery(topic.getId)) ++ tagsQ

    val rootFilters = Seq(termQuery("is_comment", "false"), termQuery(COLUMN_TOPIC_AWAITS_COMMIT, "false"))

    search(MessageIndex) query {
      boolQuery.should(queries*).filter(rootFilters).minimumShouldMatch(1).not(idsQuery(topic.getId.toString))
    } fetchSource true sourceInclude("title", "postdate", "section", "group")
  }

  def resultsOrNothing(topic: Topic, featureResult: Future[Result], deadline: Deadline): Result = {
    Option(cache.getIfPresent(topic.getId)).getOrElse {
      try {
        Await.result(featureResult, deadline.timeLeft)
      } catch {
        case _: CircuitBreakerOpenException =>
          logger.debug(s"Similar topics circuit breaker is open")
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq().asJava)
        case ex: TimeoutException =>
          logger.warn(s"Similar topics lookup timed out (${ex.getMessage})")
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq().asJava)
        case NonFatal(ex) =>
          logger.warn("Unable to find similar topics", ex)
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq().asJava)
      }
    }
  }

  private def processHit(hit: SearchHit): MoreLikeThisTopic = {
    val section = SearchResultsService.section(hit)
    val group = SearchResultsService.group(hit)

    val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
    val link = builder.buildAndExpand(section, group, Integer.parseInt(hit.id)).toUriString

    val postdate = SearchResultsService.postdate(hit)

    val title = hit.sourceAsMap("title").asInstanceOf[String]

    MoreLikeThisTopic(
      title = StringUtil.processTitle(StringUtil.escapeHtml(title)),
      link = link,
      year = postdate.year().get(),
      sectionService.getSectionByName(section).getTitle
    )
  }

  private def titleQuery(topic:Topic) = {
    moreLikeThisQuery("title")
      .likeTexts(topic.getTitleUnescaped)
      .minTermFreq(1)
      .minDocFreq(2)
      .stopWords(StopWords)
      .maxDocFreq(5000)
  }

  private def textQuery(id: Int) = {
    moreLikeThisQuery("message")
      .likeDocs(Seq(DocumentRef(MessageIndex, id.toString)))
      .minTermFreq(1)
      .stopWords(StopWords)
      .minWordLength(3)
      .maxDocFreq(100000)
  }

  private def tagsQuery(tags: MSeq[String]) = termsQuery("tag", tags)
}

object MoreLikeThisService {
  val CacheSize = 10000

  val StopWords: Seq[String] = {
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
  @BeanProperty title:String,
  @BeanProperty link:String,
  @BeanProperty year:Int,
  @BeanProperty section:String
)
