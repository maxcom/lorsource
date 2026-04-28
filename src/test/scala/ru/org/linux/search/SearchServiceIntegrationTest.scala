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
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.org.linux.PekkoConfiguration
import ru.org.linux.topic.TopicTagService

import java.time.ZoneId
import scala.jdk.CollectionConverters.ListHasAsScala

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[PekkoConfiguration]))
@DirtiesContext
class SearchServiceIntegrationTest extends FunSuite:
  new TestContextManager(this.getClass).prepareTestInstance(this)

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

  private val indexFixture = FunFixture[Unit](
    setup = { test =>
      elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))
      indexCreationService.createIndexIfNeeded()
    },
    teardown = { _ =>
      elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))
    }
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