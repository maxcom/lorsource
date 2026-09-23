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
import org.opensearch.client.opensearch.core.GetRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.opensearch.client.opensearch.indices.RefreshRequest
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.opensearch.testcontainers.OpenSearchContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import org.springframework.stereotype.{Repository, Service}
import org.springframework.security.crypto.password.PasswordEncoder
import ru.org.linux.auth.PasswordEncoderImpl
import org.springframework.test.context.ContextConfiguration
import munit.FunSuite
import ru.org.linux.PekkoConfiguration
import ru.org.linux.test.SpringTestSupport
import ru.org.linux.auth.FloodProtector
import ru.org.linux.search.OpenSearchIndexService.MessageIndex
import ru.org.linux.spring.SiteConfig

import ru.org.linux.topic.TopicDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.util.StringUtil

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration],
  classOf[PekkoConfiguration]))
class OpenSearchIndexServiceIntegrationTest extends FunSuite with SpringTestSupport:

  @Autowired
  var indexCreationService: OpenSearchIndexCreationService = scala.compiletime.uninitialized

  @Autowired
  var elastic: OpenSearchClient = scala.compiletime.uninitialized

  @Autowired
  var indexService: OpenSearchIndexService = scala.compiletime.uninitialized

  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  test("OpenSearchIndexCreationService create index"):
    indexCreationService.createIndexIfNeeded()

    val exists = elastic.indices().exists(ExistsRequest.of(_.index(MessageIndex))).value()

    assertEquals(exists, true)

  test("OpenSearchIndexService stores escaped title"):
    indexCreationService.createIndexIfNeeded()

    // заголовок со спецсимволами (& < > " ') — без экранирования при индексации
    // проверка провалится
    val payload = """<img src=x onerror=alert(1)> & "quotes" 'apostrophes'"""
    val originalTitle = topicDao.getById(1920001).title

    try
      springDB.localTx {
        topicDao.updateTitle(1920001, payload)
      }

      // чтение идёт через StringUtil.makeTitle (см. Topic), как и при индексации,
      // поэтому ожидание считаем от прочитанного значения, а не от payload
      // (makeTitle превращает "quotes" в «quotes»)
      val indexedTitle = topicDao.getById(1920001).title

      indexService.reindexMessage(1920001, withComments = false)
      elastic.indices().refresh(RefreshRequest.of(r => r.index("*")))

      val doc = elastic
        .get(GetRequest.of(_.index(MessageIndex).id("1920001")), classOf[MessageIndexDocument])
        .source()

      assertEquals(doc.title, Some(StringUtil.escapeHtml(indexedTitle)))
      assertEquals(doc.topicTitle, StringUtil.escapeHtml(indexedTitle))
    finally
      springDB.localTx {
        topicDao.updateTitle(1920001, originalTitle)
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
  def passwordEncoder: PasswordEncoder = new PasswordEncoderImpl

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