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

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.DeleteIndexRequest
import org.opensearch.client.opensearch.indices.RefreshRequest
import org.specs2.mutable.{After, SpecificationWithJUnit}
import org.specs2.specification.Scope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.org.linux.PekkoConfiguration
import ru.org.linux.topic.TopicTagService

import scala.jdk.CollectionConverters.ListHasAsScala

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[PekkoConfiguration]))
@DirtiesContext
class SearchServiceIntegrationSpec extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  sequential

  @Autowired
  var indexCreationService: OpenSearchIndexCreationService = _

  @Autowired
  var elastic: OpenSearchClient = _

  @Autowired
  var service: SearchService = _

  @Autowired
  var indexService: OpenSearchIndexService = _

  @Autowired
  var topicTagService: TopicTagService = _

  trait IndexFixture extends Scope with After {
    elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))

    indexCreationService.createIndexIfNeeded()

    override def after: Unit = elastic.indices().delete(DeleteIndexRequest.of(d => d.index("*")))
  }

  "SearchService" should {
    "make valid default search" in new IndexFixture {
      val response = service.performSearch(new SearchServiceRequest(), null)

      response.totalHits must be equalTo 0
    }

    "prepare some results" in new IndexFixture {
      topicTagService.updateTags(1920001, Seq("lor"))
      indexService.reindexMessage(1920001, withComments = false)
      elastic.indices().refresh(RefreshRequest.of(r => r.index("*")))

      val response = service.performSearch(new SearchServiceRequest(), null)

      response.hits must not be empty
      response.hits.head.tags.asScala.map(_.name) must containTheSameElementsAs(Seq("lor"))
    }
  }
}
