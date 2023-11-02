/*
 * Copyright 1998-2023 Linux.org.ru
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
import ru.org.linux.AkkaConfiguration

import scala.concurrent.duration.*

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[AkkaConfiguration]))
@DirtiesContext
class SearchResultServiceIntegrationSpec  extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  var indexService: ElasticsearchIndexService = _

  @Autowired
  var elastic: ElasticClient = _

  @Autowired
  var service: SearchResultsService = _

  trait IndexFixture extends Scope with After {
    implicit val Timeout: Duration = 30.seconds

    indexService.createIndexIfNeeded()
    indexService.reindexMessage(1920001, withComments = false)
    elastic execute {
      refreshIndex("*")
    } await

    override def after: Unit = elastic execute { deleteIndex("*") } await
  }

  "SearchResultsService" should {
    "prepare some results" in new IndexFixture {
      val response = new SearchViewer(new SearchRequest(), elastic).performSearch

      val prepared = response.hits.hits.map(service.prepare)

      prepared must not be empty
    }
  }
}
