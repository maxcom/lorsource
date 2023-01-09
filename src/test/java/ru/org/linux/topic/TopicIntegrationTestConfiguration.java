/*
 * Copyright 1998-2022 Linux.org.ru
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

import akka.actor.ActorRef;
import com.sksamuel.elastic4s.ElasticClient;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import play.api.libs.ws.StandaloneWSClient;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.TorBlockUpdater;
import ru.org.linux.comment.CommentPrepareService;
import ru.org.linux.comment.CommentReadService;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.email.EmailService;
import ru.org.linux.exception.ExceptionResolver;
import ru.org.linux.group.GroupDao;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.realtime.RealtimeWebsocketHandler;
import ru.org.linux.search.MoreLikeThisService;
import ru.org.linux.search.SearchQueueListener;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.SectionService;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.MemoriesDao;
import sttp.client3.SttpBackend;

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
                                RealtimeWebsocketHandler.class,
                                TorBlockUpdater.class
                        }
                )
        }
)

public class TopicIntegrationTestConfiguration {
  @Bean
  public TopicController topicController(SectionService sectionService, TopicDao messageDao, CommentPrepareService prepareService,
                                         TopicPrepareService topicPrepareService, CommentReadService commentService,
                                         IgnoreListDao ignoreListDao, SiteConfig siteConfig, IPBlockDao ipBlockDao,
                                         EditHistoryService editHistoryService, MemoriesDao memoriesDao,
                                         TopicPermissionService permissionService, MoreLikeThisService moreLikeThisService,
                                         TopicTagService topicTagService, MsgbaseDao msgbaseDao, MessageTextService textService,
                                         GroupDao groupDao) {
    return new TopicController(sectionService, messageDao, prepareService, topicPrepareService, commentService,
            ignoreListDao, siteConfig, ipBlockDao, editHistoryService, memoriesDao,  permissionService,
            moreLikeThisService, topicTagService, msgbaseDao, textService, groupDao);
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
  public SearchQueueSender searchQueueSender() {
    return mock(SearchQueueSender.class);
  }

  @Bean
  public StandaloneWSClient httpClient() {
    return mock(StandaloneWSClient.class);
  }

  @Bean
  public SttpBackend<Object, Object> syncClient() { return mock(SttpBackend.class); }

  @Bean
  public ElasticClient elasticClient() {
    return mock(ElasticClient.class);
  }

  @Bean("realtimeHubWS")
  public ActorRef realtimeHub() {
    return mock(ActorRef.class);
  }
}
