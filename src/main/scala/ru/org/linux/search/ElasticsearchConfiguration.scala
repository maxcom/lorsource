/*
 * Copyright 1998-2018 Linux.org.ru
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

import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import org.elasticsearch.analysis.common.CommonAnalysisPlugin
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.transport.Netty4Plugin
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.spring.SiteConfig

@Configuration
class ElasticsearchConfiguration(config: SiteConfig) {
  @Bean(destroyMethod = "close")
  def elasticsearch: TcpClient = {
    config.getElasticsearch match {
      case "embedded" ⇒
        ElasticsearchConfiguration.createEmbedded("elasticsearch", "target/elasticsearch-data").tcp()
      case address ⇒
        TcpClient.transport(ElasticsearchClientUri(address, 9300))
    }
  }
}

object ElasticsearchConfiguration {
  def createEmbedded(name: String, homePath: String): InternalLocalNode = {
    val settings = LocalNode.requiredSettings(name, homePath).foldLeft(Settings.builder) {
      case (builder, (key, value)) => builder.put(key, value)
    }.build()

    // https://discuss.elastic.co/t/unknown-filter-type-stemmer/109567/5

    val plugins = List(classOf[Netty4Plugin], classOf[CommonAnalysisPlugin])

    val mergedSettings = Settings.builder().put(settings)
      .put("http.type", "netty4")
      .put("http.enabled", "true")
      .put("node.max_local_storage_nodes", "10")
      .build()

    new InternalLocalNode(mergedSettings, plugins)
  }
}