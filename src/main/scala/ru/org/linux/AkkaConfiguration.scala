package ru.org.linux

import akka.actor.ActorSystem
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class AkkaConfiguration {
  @Bean
  def actorSystem = ActorSystem("lor")

  @Bean
  def scheduler(actorSystem: ActorSystem) = actorSystem.scheduler
}
