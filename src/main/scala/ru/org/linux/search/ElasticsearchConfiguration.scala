/*
 * Copyright 1998-2015 Linux.org.ru
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

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.node.NodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.spring.SiteConfig

@Configuration
class ElasticsearchConfiguration {
  @Autowired
  var config:SiteConfig = _

  @Bean(destroyMethod = "close")
  def elasticsearch: ElasticClient = {
    config.getElasticsearch match {
      case "embedded" ⇒
        val builder = NodeBuilder.nodeBuilder().local(true)

        builder.settings().put("path.home", "target/elasticsearch-data")

        ElasticClient.fromNode(builder.node())
      case address ⇒
        ElasticClient.transport(ElasticsearchClientUri(address, 9300))
    }
  }
}

