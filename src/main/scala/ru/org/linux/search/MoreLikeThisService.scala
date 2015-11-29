/*
 * Copyright 1998-2015 Linux.org.ru
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
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, RichSearchHit, TermsQueryDefinition}
import com.typesafe.scalalogging.StrictLogging
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.util.CharArraySet
import org.elasticsearch.ElasticsearchException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.search.ElasticsearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndexTypes}
import ru.org.linux.section.SectionService
import ru.org.linux.tag.TagRef
import ru.org.linux.topic.Topic
import ru.org.linux.util.StringUtil

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}

@Service
class MoreLikeThisService @Autowired() (
  elastic:ElasticClient,
  sectionService:SectionService,
  scheduler:Scheduler
) extends StrictLogging {
  import ru.org.linux.search.MoreLikeThisService._

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

  def searchSimilar(topic:Topic, tags:java.util.List[TagRef]):Future[Result] = {
    val cachedValue = Option(cache.getIfPresent(topic.getId))

    cachedValue.map(Future.successful).getOrElse {
      breaker.withCircuitBreaker {
        try {
          val searchResult = elastic execute makeQuery(topic, tags)

          val result: Future[Result] = searchResult.map(result => if (result.hits.nonEmpty) {
            val half = result.hits.length / 2 + result.hits.length % 2

            result.hits.map(processHit).grouped(half).map(_.toVector.asJava).toVector.asJava
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
  }

  private def makeQuery(topic: Topic, tags: Seq[TagRef]) = {
    val tagsQ = if (tags.nonEmpty) {
      Seq(tagsQuery(tags.map(_.name)))
    } else Seq.empty

    val queries = Seq(titleQuery(topic), textQuery(topic.getId)) ++ tagsQ

    val rootFilters = Seq(termQuery("is_comment", "false"), termQuery(COLUMN_TOPIC_AWAITS_COMMIT, "false"))

    search in MessageIndexTypes query {
      bool { should(queries:_*) filter rootFilters minimumShouldMatch 1 not idsQuery(topic.getId.toString) }
    } fields("title", "postdate", "section", "group")
  }

  def resultsOrNothing(topic: Topic, featureResult: Future[Result], deadline: Deadline): Result = {
    Option(cache.getIfPresent(topic.getId)).getOrElse {
      try {
        Await.result(featureResult, deadline.timeLeft)
      } catch {
        case ex: CircuitBreakerOpenException ⇒
          logger.debug(s"Similar topics circuit breaker is open")
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq())
        case ex: ElasticsearchException ⇒
          logger.warn("Unable to find similar topics", ex)
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq())
        case ex: TimeoutException ⇒
          logger.warn(s"Similar topics lookup timed out (${ex.getMessage})")
          Option(cache.getIfPresent(topic.getId)).getOrElse(Seq())
      }
    }
  }

  private def processHit(hit: RichSearchHit): MoreLikeThisTopic = {
    val section = SearchResultsService.section(hit)
    val group = SearchResultsService.group(hit)

    val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
    val link = builder.buildAndExpand(section, group, new Integer(hit.id)).toUriString

    val postdate = SearchResultsService.postdate(hit)

    val title = hit.field("title").value[String]

    MoreLikeThisTopic(
      title = StringUtil.processTitle(StringUtil.escapeHtml(title)),
      link = link,
      year = postdate.year().get(),
      sectionService.getSectionByName(section).getTitle
    )
  }

  private def titleQuery(topic:Topic) =
    moreLikeThisQuery("title") like
      topic.getTitleUnescaped minTermFreq 1 minDocFreq 2 stopWords(StopWords: _*) maxDocFreq 5000

  private def textQuery(id:Int) =
    moreLikeThisQuery("message") minTermFreq 1 stopWords
      (StopWords: _*) minWordLength 3 maxDocFreq 100000 ids id.toString

  private def tagsQuery(tags:Seq[String]) = TermsQueryDefinition("tag", tags)
}

object MoreLikeThisService {
  val CacheSize = 10000

  val StopWords = {
    val stop = RussianAnalyzer.getDefaultStopSet.asScala.map(arr ⇒ new String(arr.asInstanceOf[Array[Char]]))
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
