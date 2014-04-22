package ru.org.linux.search

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.springframework.context.annotation.{Configuration, Bean}
import org.springframework.beans.factory.annotation.Autowired
import ru.org.linux.spring.SiteConfig
import org.elasticsearch.node.NodeBuilder

@Configuration
class ElasticsearchConfiguration {
  @Autowired
  var config:SiteConfig = _

  @Bean(destroyMethod = "close")
  def elasticsearch: Client = {
    config.getElasticsearch match {
      case "embedded" =>
        val builder = NodeBuilder.nodeBuilder().local(true)

        builder.settings().put("path.data", "target/elasticsearch-data")

        builder.node().client();

      case address =>
        new TransportClient().addTransportAddress(new InetSocketTransportAddress(address, 9300))
    }
  }
}

