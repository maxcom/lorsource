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

package ru.org.linux.tag

import munit.FunSuite
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, times, verify, when}
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.ShardStatistics
import org.opensearch.client.opensearch._types.aggregations.{Aggregate, SignificantStringTermsBucket}
import org.opensearch.client.opensearch.core.{CountRequest, CountResponse, SearchRequest, SearchResponse}
import org.opensearch.client.opensearch.core.search.{Hit, HitsMetadata, TotalHits, TotalHitsRelation}
import ru.org.linux.group.GroupService
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}

import java.util.concurrent.CompletableFuture
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class TagServiceTest extends FunSuite:
  private val system = ActorSystem("TagServiceTest")

  override def afterAll(): Unit = Await.result(system.terminate(), 10.seconds)

  private def makeService(client: OpenSearchAsyncClient): TagService =
    new TagService(mock(classOf[TagDao]), client, system, mock(classOf[SectionService]), mock(classOf[GroupService]))

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

  private def makeBucket(key: String): SignificantStringTermsBucket =
    SignificantStringTermsBucket.of(b => b.key(key).docCount(10L).bgCount(100L).score(1.0))

  private def makeSearchResponse(aggName: String, keys: Seq[String]): SearchResponse[Void] =
    val agg = Aggregate.of(a =>
      a.sigsterms(s => s.docCount(100L).bgCount(200L).buckets(b => b.array(keys.map(makeBucket).asJava))))

    new SearchResponse.Builder[Void]()
      .took(1L)
      .timedOut(false)
      .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
      .hits(
        new HitsMetadata.Builder[Void]()
          .total(new TotalHits.Builder().value(0L).relation(TotalHitsRelation.Eq).build())
          .hits(java.util.List.of[Hit[Void]]())
          .build())
      .aggregations(aggName, agg)
      .build()

  private def makeCountResponse(count: Long): CountResponse =
    new CountResponse.Builder()
      .count(count)
      .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
      .build()

  private def stubSearch(client: OpenSearchAsyncClient, aggName: String, keys: String*): Unit =
    when(client.search(any[SearchRequest](), any[Class[Void]]())).thenReturn(
      CompletableFuture.completedFuture(makeSearchResponse(aggName, keys.toSeq)))

  private def stubSearchFailure(client: OpenSearchAsyncClient): Unit =
    when(client.search(any[SearchRequest](), any[Class[Void]]())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("OpenSearch is down")))

  private def stubCount(client: OpenSearchAsyncClient, count: Long): Unit =
    when(client.count(any[CountRequest]())).thenReturn(CompletableFuture.completedFuture(makeCountResponse(count)))

  private def stubCountFailure(client: OpenSearchAsyncClient): Unit =
    when(client.count(any[CountRequest]())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("OpenSearch is down")))

  test("getRelatedTags: результат кешируется, повторный вызов не ходит в OpenSearch"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, "related", "related1", "related2")
    val service = makeService(client)

    val first = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)
    assertEquals(first.map(_.name), Seq("related1", "related2"))

    val second = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)
    assert(second eq first)

    verify(client, times(1)).search(any[SearchRequest](), any[Class[Void]]())

  test("getRelatedTags: при сбое OpenSearch отдаётся stale значение"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, "related", "related1")
    val service = makeService(client)

    val original = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)

    // имитируем истечение свежего TTL: остаётся только stale копия
    service.relatedTagsCache.invalidate("foo")
    stubSearchFailure(client)

    val served = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)
    assert(served eq original)

  test("getRelatedTags: при сбое без кеша исключение пробрасывается"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearchFailure(client)
    val service = makeService(client)

    intercept[RuntimeException]:
      Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)

  test("countTagTopics: результат кешируется, повторный вызов не ходит в OpenSearch"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubCount(client, 42L)
    val service = makeService(client)

    val first = Await.result(service.countTagTopics("foo", None, 500.millis.fromNow), 10.seconds)
    assertEquals(first, Some(42L))

    val second = Await.result(service.countTagTopics("foo", None, 500.millis.fromNow), 10.seconds)
    assert(second eq first)

    verify(client, times(1)).count(any[CountRequest]())

  test("countTagTopics: при сбое OpenSearch отдаётся stale значение"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubCount(client, 42L)
    val service = makeService(client)

    Await.result(service.countTagTopics("foo", None, 500.millis.fromNow), 10.seconds)

    // имитируем истечение свежего TTL: остаётся только stale копия
    service.tagTopicCountCache.invalidate(TagService.TagTopicCount("foo", None))
    stubCountFailure(client)

    val served = Await.result(service.countTagTopics("foo", None, 500.millis.fromNow), 10.seconds)
    assertEquals(served, Some(42L))

  test("countTagTopics: при сбое без кеша возвращается None"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubCountFailure(client)
    val service = makeService(client)

    val result = Await.result(service.countTagTopics("foo", None, 500.millis.fromNow), 10.seconds)
    assertEquals(result, None)

  test("getActiveTopTags: результат кешируется, повторный вызов не ходит в OpenSearch"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, "active", "active1", "active2")
    val service = makeService(client)

    val first = Await.result(service.getActiveTopTags(makeSection, None, None, 500.millis.fromNow), 10.seconds)
    assertEquals(first.map(_.name), Seq("active1", "active2"))

    val second = Await.result(service.getActiveTopTags(makeSection, None, None, 500.millis.fromNow), 10.seconds)
    assert(second eq first)

    verify(client, times(1)).search(any[SearchRequest](), any[Class[Void]]())

  test("getActiveTopTags: при сбое OpenSearch отдаётся stale значение"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, "active", "active1")
    val service = makeService(client)

    val original = Await.result(service.getActiveTopTags(makeSection, None, None, 500.millis.fromNow), 10.seconds)

    // имитируем истечение свежего TTL: остаётся только stale копия
    service.activeTopTagsCache.invalidate(TagService.ActiveTopTags(makeSection, None, None))
    stubSearchFailure(client)

    val served = Await.result(service.getActiveTopTags(makeSection, None, None, 500.millis.fromNow), 10.seconds)
    assert(served eq original)

  test("getActiveTopTags: при сбое без кеша возвращается пустой список"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearchFailure(client)
    val service = makeService(client)

    val result = Await.result(service.getActiveTopTags(makeSection, None, None, 500.millis.fromNow), 10.seconds)
    assertEquals(result, Seq.empty[TagRef])

  test("пять сбоев подряд открывают breaker: дальше stale отдаётся без запроса к OpenSearch"):
    val client = mock(classOf[OpenSearchAsyncClient])
    stubSearch(client, "related", "related1")
    val service = makeService(client)

    val original = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)

    // имитируем истечение свежего TTL: остаётся только stale копия
    service.relatedTagsCache.invalidate("foo")
    stubSearchFailure(client)

    // maxFailures=5: пять неудач подряд открывают breaker; каждый вызов отдаёт stale
    for _ <- 1 to 5 do
      Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)

    // при открытом breaker клиент не дёргается вовсе
    val served = Await.result(service.getRelatedTags("foo", 500.millis.fromNow), 10.seconds)
    assert(served eq original)

    verify(client, times(6)).search(any[SearchRequest](), any[Class[Void]]())

end TagServiceTest
