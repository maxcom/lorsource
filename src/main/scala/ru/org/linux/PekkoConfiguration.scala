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

package ru.org.linux

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.springframework.context.annotation.{Bean, Configuration}

import scala.concurrent.Await
import scala.concurrent.duration.*

case class TerminatableActorSystem(system: ActorSystem) {
  def close(): Unit = {
    Await.result(system.terminate(), 5.minutes)
  }
}

@Configuration
class PekkoConfiguration {
  @Bean
  def terminatableActorSystem: TerminatableActorSystem = TerminatableActorSystem(ActorSystem("lor"))

  @Bean
  def actorSystem(pekko: TerminatableActorSystem): ActorSystem = pekko.system

  @Bean
  def scheduler(actorSystem: ActorSystem): Scheduler = actorSystem.scheduler
}
