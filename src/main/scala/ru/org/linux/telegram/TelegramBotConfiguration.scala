/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.telegram

import akka.actor.{ActorRef, ActorSystem, Props}
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicTagDao
import sttp.client3.SttpBackend

import scala.concurrent.Future

@Configuration
class TelegramBotConfiguration {
  @Bean(name=Array("telegramBot"))
  def telegramBotActor(actorSystem: ActorSystem, dao: TelegramPostsDao, httpClient: SttpBackend[Future, Any],
                       config: SiteConfig, topicTagDao: TopicTagDao): ActorRef = {
    actorSystem.actorOf(Props(new TelegramBotActor(dao, httpClient, config, topicTagDao)))
  }
}
