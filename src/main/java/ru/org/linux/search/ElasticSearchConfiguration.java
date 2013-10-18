package ru.org.linux.search;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration {
  @Bean(destroyMethod = "close")
  public Client elastic() {
    return new TransportClient()
            .addTransportAddress(new InetSocketTransportAddress("127.0.0.1", 9300));
  }
}
