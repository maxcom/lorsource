/*
 * Copyright 1998-2026 Linux.org.ru
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

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.http.HttpMethod
import org.springframework.security.web.csrf.{CookieCsrfTokenRepository, CsrfFilter}
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.{AndRequestMatcher, NegatedRequestMatcher, OrRequestMatcher,
  RequestMatcher}
import ru.org.linux.csrf.{CSRFProtectionService, CsrfAccessDeniedHandler, EagerCsrfTokenRequestHandler}

import java.time.Duration

@Configuration
class SpringSecurityConfiguration:
  @Bean
  def httpFirewall: StrictHttpFirewall =
    val firewall = new StrictHttpFirewall

    firewall.setAllowedHeaderValues(_ => true)

    firewall

  @Bean
  def csrfTokenRepository: CookieCsrfTokenRepository =
    val repository = CookieCsrfTokenRepository.withHttpOnlyFalse()

    repository.setCookieName(CSRFProtectionService.CSRF_COOKIE)
    repository.setParameterName(CSRFProtectionService.CSRF_INPUT_NAME)
    repository.setHeaderName("X-CSRF-TOKEN")
    repository.setCookieCustomizer(builder => builder.maxAge(Duration.ofDays(744)))

    repository

  @Bean
  def csrfTokenRequestHandler: EagerCsrfTokenRequestHandler = EagerCsrfTokenRequestHandler()

  @Bean
  def csrfProtectionMatcher: RequestMatcher =
    val manuallyChecked = SpringSecurityConfiguration
      .ManuallyCheckedCsrfPaths
      .map: path =>
        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path)

    AndRequestMatcher(CsrfFilter.DEFAULT_CSRF_MATCHER, NegatedRequestMatcher(OrRequestMatcher(manuallyChecked*)))

  @Bean
  def csrfAccessDeniedHandler: CsrfAccessDeniedHandler = CsrfAccessDeniedHandler()

object SpringSecurityConfiguration:
  private val ManuallyCheckedCsrfPaths =
    List("/add.jsp", "/edit.jsp", "/add_comment.jsp", "/edit_comment", "/add_comment_ajax", "/addphoto.jsp")
