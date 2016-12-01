/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.exception

import akka.actor.ActorSystem
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.util.SiteConfig

@Configuration
class ExceptionHandlingConfiguration {
  @Bean(name=Array("exceptionMailingActor"))
  def exceptionMailingActor(siteConfig: SiteConfig, actorSystem: ActorSystem) = {
    actorSystem.actorOf(ExceptionMailingActor.props(siteConfig))
  }
}
