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

import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import org.apache.commons.httpclient.URI
import org.apache.hc.core5.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.spring.SiteConfig

@Configuration
class OpenSearchConfiguration(config: SiteConfig) {
  @Bean(destroyMethod = "close")
  def legacyClient: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties(config.getElasticsearch), new RequestConfigCallback() {
      override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
        requestConfigBuilder.setSocketTimeout(120000)
      }
    }))
  }

  @Bean(destroyMethod = "close")
  def clientTransport: OpenSearchTransport = {
    val url = new URI(config.getElasticsearch, true)
    val transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost(url.getScheme, url.getHost, url.getPort)).build()

    transport
  }

  @Bean
  def client(transport: OpenSearchTransport): OpenSearchClient = new OpenSearchClient(transport)
}
