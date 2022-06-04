package ru.org.linux

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.web.firewall.StrictHttpFirewall

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

@Configuration
class SpringSecurityConfiguration {
  private val allowed: Pattern = Pattern.compile("[\\p{IsAssigned}&&[^\\p{IsControl}]]*")

  @Bean
  def httpFirewall: StrictHttpFirewall = {
    val firewall = new StrictHttpFirewall

    firewall.setAllowedHeaderValues { header =>
      val parsed = new String(header.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)

      allowed.matcher(parsed).matches
    }

    firewall
  }
}
