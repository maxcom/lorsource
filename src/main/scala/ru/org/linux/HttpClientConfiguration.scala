/*
 * Copyright 1998-2019 Linux.org.ru
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
package ru.org.linux

import akka.actor.ActorSystem
import org.springframework.context.annotation.{Bean, Configuration}
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

@Configuration
class HttpClientConfiguration {
  @Bean
  def httpClient(actorSystem: ActorSystem): StandaloneWSClient = {
    implicit val system: ActorSystem = actorSystem

    StandaloneAhcWSClient()
  }
}
