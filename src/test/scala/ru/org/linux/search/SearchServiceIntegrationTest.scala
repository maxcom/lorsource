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
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.{DeleteIndexRequest, RefreshRequest}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.PekkoConfiguration
import ru.org.linux.test.SpringTestSupport
import ru.org.linux.topic.TopicTagService
import ru.org.linux.util.StringUtil

import java.time.ZoneId
import scala.jdk.CollectionConverters.ListHasAsScala

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration], classOf[PekkoConfiguration]))
@DirtiesContext
class SearchServiceIntegrationTest extends FunSuite with SpringTestSupport:

  @Autowired
  var indexCreationService: OpenSearchIndexCreationService = scala.compiletime.uninitialized

  @Autowired
  var elastic: OpenSearchClient = scala.compiletime.uninitialized

  @Autowired
  var service: SearchService = scala.compiletime.uninitialized

  @Autowired
  var indexService: OpenSearchIndexService = scala.compiletime.uninitialized

  @Autowired
  var topicTagService: TopicTagService = scala.compiletime.uninitialized

  private val indexFixture =
    FunFixture[Unit](
      setup =
        _ =>
          elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))
          indexCreationService.createIndexIfNeeded()
      ,
      teardown = _ => elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))
    )

  indexFixture.test("SearchService make valid default search"): _ =>
    val response = service.performSearch(new SearchServiceRequest(), ZoneId.systemDefault())
    assertEquals(response.totalHits.toInt, 0)

  indexFixture.test("SearchService prepare some results"): _ =>
    topicTagService.updateTags(1920001, Seq("lor"))
    indexService.reindexMessage(1920001, withComments = false)
    elastic.indices().refresh(RefreshRequest.of(r => r.index("*")))

    val response = service.performSearch(new SearchServiceRequest(), ZoneId.systemDefault())

    assert(response.hits.nonEmpty)
    assertEquals(response.hits.head.tags.asScala.map(_.name).toSeq, Seq("lor"))

  indexFixture.test("SearchService renders escaped title with highlight as-is"): _ =>
    // title/topic_title хранятся в индексе уже заэкранированными (см. OpenSearchIndexService),
    // search.jsp выводит их без c:out (должна проходить подсветка <em>) — рендер pass-through
    val doc = MessageIndexDocument(
      section = "forum",
      topicAuthor = "maxcom",
      topicId = 90001,
      author = "maxcom",
      group = "linux-org-ru",
      title = Some(StringUtil.escapeHtml("<img src=x onerror=alert(1)> uniquezzztitle")),
      topicTitle = StringUtil.escapeHtml("<img src=x onerror=alert(1)> uniquezzztitle"),
      message = "plain text",
      postdate = java.time.Instant.now().toString,
      tags = Seq.empty,
      isComment = false,
      topicAwaitsCommit = false
    )

    elastic.index(
      org
        .opensearch
        .client
        .opensearch
        .core
        .IndexRequest
        .of(i => i.index(OpenSearchIndexService.MessageIndex).id("90001").document(doc)))
    elastic.indices().refresh(RefreshRequest.of(r => r.index("*")))

    val query = new SearchServiceRequest()
    query.q = "uniquezzztitle"

    val response = service.performSearch(query, ZoneId.systemDefault())

    assert(response.hits.nonEmpty, "document should be found")

    val title = response.hits.head.title

    // заэкранированный payload проходит в литеральном виде, сырого HTML-тега нет
    assert(title.contains("&lt;img src=x onerror=alert(1)&gt;"), title)
    assert(!title.contains("<img"), title)
    // подсветка сохранена (теги подсветки не удаляются и не экранируются)
    assert(title.contains("<em class=\"search-hl\">"), title)
    assert(title.contains("uniquezzztitle"), title)
