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

import org.apache.commons.httpclient.URI
import org.apache.hc.core5.http.HttpHost
import org.mockito.Mockito
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.opensearch.testcontainers.OpenSearchContainer
import org.specs2.mutable.SpecificationWithJUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import org.springframework.stereotype.{Repository, Service}
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.org.linux.PekkoConfiguration
import ru.org.linux.auth.FloodProtector
import ru.org.linux.search.OpenSearchIndexService.MessageIndex
import ru.org.linux.spring.SiteConfig

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[PekkoConfiguration]))
class OpenSearchIndexServiceIntegrationSpec extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  var indexCreationService: OpenSearchIndexCreationService = scala.compiletime.uninitialized

  @Autowired
  var elastic: OpenSearchClient = scala.compiletime.uninitialized

  "OpenSearchIndexCreationService" should {
    "create index" in {
      indexCreationService.createIndexIfNeeded()

      val exists = elastic.indices().exists(ExistsRequest.of(_.index(MessageIndex))).value()

      exists must beTrue
    }
  }
}

@Configuration
@ImportResource(Array("classpath:common.xml", "classpath:database.xml"))
@ComponentScan(
  basePackages = Array("ru.org.linux"),
  lazyInit = true,
  useDefaultFilters = false,
  excludeFilters = Array(
    new ComponentScan.Filter(`type` = FilterType.ASSIGNABLE_TYPE, value = Array(classOf[SiteConfig]))
  ),
  includeFilters = Array(
    new ComponentScan.Filter(`type` = FilterType.ANNOTATION, value = Array(classOf[Service], classOf[Repository]))))
class SearchIntegrationTestConfiguration {
  @Bean
  def openSearchContainer: OpenSearchContainer[Nothing] = {
    val container = new OpenSearchContainer("opensearchproject/opensearch:3.5.0")
    container.start()
    container
  }

  @Bean(destroyMethod = "close")
  def clientTransport(container: OpenSearchContainer[Nothing]): OpenSearchTransport = {
    val url = new URI(container.getHttpHostAddress, true)
    val transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost(url.getScheme, url.getHost, url.getPort)).build()

    transport
  }

  @Bean
  def client(transport: OpenSearchTransport): OpenSearchClient = new OpenSearchClient(transport)

  @Bean
  def asyncClient(transport: OpenSearchTransport): OpenSearchAsyncClient = new OpenSearchAsyncClient(transport)

  @Bean
  def floodProtector: FloodProtector = Mockito.mock(classOf[FloodProtector])
}
