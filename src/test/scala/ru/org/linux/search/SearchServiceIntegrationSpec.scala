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

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.*
import org.specs2.mutable.{After, SpecificationWithJUnit}
import org.specs2.specification.Scope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.org.linux.PekkoConfiguration

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[PekkoConfiguration]))
@DirtiesContext
class SearchServiceIntegrationSpec extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  sequential

  @Autowired
  var indexCreationService: OpenSearchIndexCreationService = _

  @Autowired
  var elastic: ElasticClient = _

  @Autowired
  var service: SearchService = _

  @Autowired
  var indexService: OpenSearchIndexService = _

  trait IndexFixture extends Scope with After {
    elastic execute { deleteIndex("*") } await

    indexCreationService.createIndexIfNeeded()

    override def after: Unit = elastic execute { deleteIndex("*") } await
  }

  "SearchService" should {
    "make valid default search" in new IndexFixture {
      val response = service.performSearch(new SearchRequest(), null)

      response.totalHits must be equalTo 0
    }

    "prepare some results" in new IndexFixture {
      indexService.reindexMessage(1920001, withComments = false)
      elastic execute {
        refreshIndex("*")
      } await

      val response = service.performSearch(new SearchRequest(), null)

      val prepared: Array[SearchItem] = response.hits.hits.map(service.prepare)

      prepared must not be empty
    }
  }
}
