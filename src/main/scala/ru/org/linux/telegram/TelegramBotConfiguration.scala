/*
 * Copyright 1998-2021 Linux.org.ru
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
import play.api.libs.ws.StandaloneWSClient
import ru.org.linux.spring.SiteConfig

@Configuration
class TelegramBotConfiguration {
  @Bean(name=Array("telegramBot"))
  def telegramBotActor(actorSystem: ActorSystem, dao: TelegramPostsDao, wsClient: StandaloneWSClient,
                       config: SiteConfig): ActorRef = {
    actorSystem.actorOf(Props(new TelegramBotActor(dao, wsClient, config)))
  }
}
