package ru.org.linux

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.web.firewall.StrictHttpFirewall

@Configuration
class SpringSecurityConfiguration {
  @Bean
  def httpFirewall: StrictHttpFirewall = {
    val firewall = new StrictHttpFirewall

    firewall.setAllowedHeaderValues(_ => true )

    firewall
  }
}
