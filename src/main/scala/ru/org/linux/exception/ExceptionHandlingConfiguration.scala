package ru.org.linux.exception

import akka.actor.ActorSystem
import org.springframework.context.annotation.{Bean, Configuration}
import ru.org.linux.spring.SiteConfig

@Configuration
class ExceptionHandlingConfiguration {
  @Bean(name=Array("exceptionMailingActor"))
  def exceptionMailingActor(siteConfig: SiteConfig, actorSystem: ActorSystem) = {
    actorSystem.actorOf(ExceptionMailingActor.props(siteConfig))
  }
}
