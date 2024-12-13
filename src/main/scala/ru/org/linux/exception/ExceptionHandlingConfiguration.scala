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

package ru.org.linux.exception

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import ru.org.linux.spring.SiteConfig

import java.io.{PrintWriter, StringWriter}
import java.util.concurrent.Executors

@Configuration
class ExceptionHandlingConfiguration extends StrictLogging {
  @Bean(name=Array("exceptionMailingActor"))
  def exceptionMailingActor(siteConfig: SiteConfig, actorSystem: ActorSystem): ActorRef = {
    actorSystem.actorOf(ExceptionMailingActor.props(siteConfig))
  }

  @Bean
  def taskScheduler(exceptionMailingActor: ActorRef): TaskScheduler = {
    val scheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor)

    scheduler.setErrorHandler(ex => {
      val text = new StringBuilder("Periodic task failed\n\n")

      val exceptionStackTrace = new StringWriter
      ex.printStackTrace(new PrintWriter(exceptionStackTrace))
      text.append(exceptionStackTrace.toString)

      logger.warn("Periodic task failed", ex)

      exceptionMailingActor ! ExceptionMailingActor.Report(ex.getClass, text.toString())
    })

    scheduler
  }
}
