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

package ru.org.linux.topic

import org.apache.pekko.actor.typed.ActorRef
import com.sksamuel.elastic4s.ElasticClient
import org.mockito.Mockito
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration, FilterType, ImportResource}
import org.springframework.stereotype.Controller
import ru.org.linux.auth.{BlackListUpdater, IPBlockDao}
import ru.org.linux.comment.{CommentPrepareService, CommentReadService}
import ru.org.linux.edithistory.EditHistoryService
import ru.org.linux.email.EmailService
import ru.org.linux.exception.ExceptionResolver
import ru.org.linux.markup.MessageTextService
import ru.org.linux.msgbase.MsgbaseDao
import ru.org.linux.realtime.{RealtimeEventHub, RealtimeWebsocketHandler}
import ru.org.linux.search.{MoreLikeThisService, SearchQueueListener, SearchQueueSender}
import ru.org.linux.section.SectionService
import ru.org.linux.user.{IgnoreListDao, MemoriesDao}
import ru.org.linux.warning.WarningService
import sttp.client3.*

import java.io.FileInputStream
import java.util.Properties

@Configuration
@ImportResource(Array("classpath:database.xml"))
@ComponentScan(
  basePackages = Array("ru.org.linux"),
  excludeFilters = Array(
    new ComponentScan.Filter(
      `type` = FilterType.ANNOTATION,
      classes = Array(classOf[Controller], classOf[Configuration])
    ),
    new ComponentScan.Filter(
      `type` = FilterType.ASSIGNABLE_TYPE,
      classes = Array(
        classOf[SearchQueueListener],
        classOf[SearchQueueSender],
        classOf[TopicListService],
        classOf[MoreLikeThisService],
        classOf[EmailService],
        classOf[ExceptionResolver],
        classOf[RealtimeWebsocketHandler],
        classOf[BlackListUpdater]
      )
    )
  )
)
class TopicIntegrationTestConfiguration {
  @Bean
  def topicController(
    sectionService: SectionService,
    messageDao: TopicDao,
    prepareService: CommentPrepareService,
    topicPrepareService: TopicPrepareService,
    commentService: CommentReadService,
    ignoreListDao: IgnoreListDao,
    ipBlockDao: IPBlockDao,
    editHistoryService: EditHistoryService,
    memoriesDao: MemoriesDao,
    permissionService: TopicPermissionService,
    moreLikeThisService: MoreLikeThisService,
    topicTagService: TopicTagService,
    msgbaseDao: MsgbaseDao,
    textService: MessageTextService,
    warningService: WarningService
  ): TopicController = {
    new TopicController(
      sectionService,
      messageDao,
      prepareService,
      topicPrepareService,
      commentService,
      ignoreListDao,
      ipBlockDao,
      editHistoryService,
      memoriesDao,
      permissionService,
      moreLikeThisService,
      topicTagService,
      msgbaseDao,
      textService,
      warningService
    )
  }

  @Bean
  def properties: Properties = {
    val props = new Properties()
    props.load(new FileInputStream("src/main/webapp/WEB-INF/config.properties.dist"))
    props
  }

  @Bean
  def moreLikeThisService: MoreLikeThisService = {
    Mockito.mock(classOf[MoreLikeThisService])
  }

  @Bean
  def searchQueueSender: SearchQueueSender = {
    Mockito.mock(classOf[SearchQueueSender])
  }

  @Bean
  def syncClient: SttpBackend[Identity, Any] = {
    Mockito.mock(classOf[SttpBackend[Identity, Any]])
  }

  @Bean
  def elasticClient: ElasticClient = {
    Mockito.mock(classOf[ElasticClient])
  }

  @Bean
  def openSearchClient: OpenSearchClient = {
    Mockito.mock(classOf[OpenSearchClient])
  }

  @Bean(Array("realtimeHubWS"))
  def realtimeHub: ActorRef[RealtimeEventHub.Protocol] = {
    Mockito.mock(classOf[ActorRef[RealtimeEventHub.Protocol]])
  }
}
