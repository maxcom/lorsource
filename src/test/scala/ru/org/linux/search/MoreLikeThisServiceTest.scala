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

import munit.FunSuite
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, times, verify, when}
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.ShardStatistics
import org.opensearch.client.opensearch.core.{SearchRequest, SearchResponse}
import org.opensearch.client.opensearch.core.search.{Hit, HitsMetadata, TotalHits, TotalHitsRelation}
import ru.org.linux.reaction.Reactions
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}
import ru.org.linux.topic.Topic

import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.CompletableFuture
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*

class MoreLikeThisServiceTest extends FunSuite:
  private val system = ActorSystem("MoreLikeThisServiceTest")

  override def afterAll(): Unit = Await.result(system.terminate(), 10.seconds)

  private def makeTopic(id: Int): Topic =
    Topic(
      id = id,
      postscore = 0,
      sticky = false,
      linktext = null,
      url = null,
      rawTitle = "Тестовый заголовок",
      authorUserId = 1,
      groupId = 1,
      deleted = false,
      expired = false,
      commitby = 0,
      postdate = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
      commitDate = null,
      groupUrl = "forum-generic",
      lastModified = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
      sectionId = Section.Forum,
      commentCount = 0,
      commited = true,
      notop = false,
      userAgentId = 0,
      postIP = "127.0.0.1",
      resolved = false,
      minor = false,
      draft = false,
      allowAnonymous = false,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0
    )

  private def makeSection: Section =
    Section(
      name = "forum",
      imagepost = false,
      premoderated = true,
      id = Section.Forum,
      votepoll = false,
      scrollMode = SectionScrollModeEnum.SECTION,
      topicsRestriction = 0,
      imageAllowed = true
    )

  private def makeDocument(title: String): MessageIndexDocument =
    MessageIndexDocument(
      section = "forum",
      topicAuthor = "author",
      topicId = 1,
      author = "author",
      group = "forum-generic",
      title = Some(title),
      topicTitle = title,
      message = "message text",
      postdate = "2026-01-01T00:00:00Z",
      tags = Seq.empty,
      isComment = false,
      topicAwaitsCommit = false
    )

  private def makeHit(id: Int, title: String): Hit[MessageIndexDocument] =
    new Hit.Builder[MessageIndexDocument]().id(id.toString).source(makeDocument(title)).build()

  private def makeResponse(hits: Hit[MessageIndexDocument]*): SearchResponse[MessageIndexDocument] =
    new SearchResponse.Builder[MessageIndexDocument]()
      .took(1L)
      .timedOut(false)
      .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
      .hits(
        new HitsMetadata.Builder[MessageIndexDocument]()
          .total(new TotalHits.Builder().value(hits.size.toLong).relation(TotalHitsRelation.Eq).build())
          .hits(hits.asJava)
          .build())
      .build()

  private def stubSearch(client: OpenSearchAsyncClient, response: SearchResponse[MessageIndexDocument]): Unit =
    when(client.search(any[SearchRequest](), any[Class[MessageIndexDocument]]())).thenReturn(
      CompletableFuture.completedFuture(response))

  private def stubSearchFailure(client: OpenSearchAsyncClient, ex: RuntimeException): Unit =
    when(client.search(any[SearchRequest](), any[Class[MessageIndexDocument]]())).thenReturn(
      CompletableFuture.failedFuture(ex))

  private def makeService(client: OpenSearchAsyncClient): MoreLikeThisService =
    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSectionByName("forum")).thenReturn(makeSection)

    new MoreLikeThisService(client, sectionService, system.scheduler)

  test("successful search populates cache; repeat call does not hit OpenSearch"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, makeResponse(makeHit(1001, "Похожая тема"), makeHit(1002, "Ещё похожая тема")))

    val service = makeService(client)
    val topic = makeTopic(12345)

    val first = Await.result(service.searchSimilar(topic, Seq.empty), 10.seconds)
    assertEquals(first.size, 2)
    assertEquals(first.get(0).get(0).title, "Похожая тема")

    val second = Await.result(service.searchSimilar(topic, Seq.empty), 10.seconds)
    assert(second eq first)

    verify(client, times(1)).search(any[SearchRequest](), any[Class[MessageIndexDocument]]())

  test("stale result is served when fresh entry expired and search fails"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, makeResponse(makeHit(1001, "Похожая тема"), makeHit(1002, "Ещё похожая тема")))

    val service = makeService(client)
    val topic = makeTopic(12345)
    val original = Await.result(service.searchSimilar(topic, Seq.empty), 10.seconds)

    // имитируем истечение свежего TTL: остаётся только черствая копия
    service.cache.invalidate(topic.id)

    val served = service.resultsOrNothing(
      topic,
      Future.failed(new RuntimeException("OpenSearch is down")),
      500.millis.fromNow)

    assertEquals(served, original)

  test("nothing cached and search fails gives empty result"):
    val service = makeService(mock(classOf[OpenSearchAsyncClient]))
    val topic = makeTopic(12345)

    val served = service.resultsOrNothing(
      topic,
      Future.failed(new RuntimeException("OpenSearch is down")),
      500.millis.fromNow)

    assert(served.isEmpty)

  test("stale result is served instantly when the circuit breaker is open"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, makeResponse(makeHit(1001, "Похожая тема")))

    val service = makeService(client)
    val topic = makeTopic(12345)
    val original = Await.result(service.searchSimilar(topic, Seq.empty), 10.seconds)

    // имитируем истечение свежего TTL
    service.cache.invalidate(topic.id)
    stubSearchFailure(client, new RuntimeException("connection refused"))

    // maxFailures=5: после пяти неудач подряд breaker открывается
    for _ <- 1 to 5 do
      intercept[RuntimeException]:
        Await.result(service.searchSimilar(topic, Seq.empty), 10.seconds)

    val start = System.nanoTime()
    val served = service.resultsOrNothing(topic, service.searchSimilar(topic, Seq.empty), 500.millis.fromNow)
    val elapsedMs = (System.nanoTime() - start) / 1000000

    assertEquals(served, original)
    // открытый breaker даёт мгновенный фолбэк, без ожидания дедлайна
    assert(elapsedMs < 400)

end MoreLikeThisServiceTest
