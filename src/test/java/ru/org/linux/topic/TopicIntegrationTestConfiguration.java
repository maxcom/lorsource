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

package ru.org.linux.topic;

import com.sksamuel.elastic4s.TcpClient;
import com.sksamuel.elastic4s.TcpClient$;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import play.api.libs.ws.StandaloneWSClient;
import ru.org.linux.email.EmailService;
import ru.org.linux.exception.ExceptionResolver;
import ru.org.linux.realtime.RealtimeWebsocketHandler;
import ru.org.linux.search.MoreLikeThisService;
import ru.org.linux.search.SearchQueueListener;
import ru.org.linux.search.SearchQueueSender;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:database.xml")
@ComponentScan(
        basePackages = "ru.org.linux",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        value = {Controller.class,Configuration.class}
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = {
                                SearchQueueListener.class,
                                SearchQueueSender.class,
                                TopicListService.class,
                                MoreLikeThisService.class,
                                EmailService.class,
                                ExceptionResolver.class,
                                RealtimeWebsocketHandler.class
                        }
                )
        }
)

public class TopicIntegrationTestConfiguration {
  @Bean
  public TopicController topicController() {
    return new TopicController();
  }

  @Bean
  public Properties properties() throws IOException {
    Properties properties = new Properties();

    properties.load(new FileInputStream("src/main/webapp/WEB-INF/config.properties.dist"));

    return properties;
  }

  @Bean
  public MoreLikeThisService moreLikeThisService() {
    return mock(MoreLikeThisService.class);
  }

  @Bean
  public TcpClient elasticsearch() {
    Client mockClient = mock(Client.class);

    Mockito.when(mockClient.prepareSearch(Matchers.anyString())).thenThrow(new ElasticsearchException("no ES here"));

    return TcpClient$.MODULE$.fromClient(mockClient);
  }

  @Bean
  public SearchQueueSender searchQueueSender() {
    return mock(SearchQueueSender.class);
  }

  @Bean
  public StandaloneWSClient httpClient() {
    return mock(StandaloneWSClient.class);
  }
}
