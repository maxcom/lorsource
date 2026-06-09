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

package ru.org.linux.auth

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.httpclient.{URI, URIException}
import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.spring.SiteConfig

class HstsInterceptor(config: SiteConfig) extends HandlerInterceptor:
  private val HCaptchaSources = "https://hcaptcha.com https://*.hcaptcha.com"
  private val PingAdminImageSource = "https://images.ping-admin.ru"

  private val secureOrigin = origin(config.getSecureURI, "SecureUrl")
  private val webSocketOrigin = Option(config.getWSUrl).filter(_.nonEmpty).map(parseOrigin)

  private val contentSecurityPolicy =
    val connectSources = Seq("'self'", HCaptchaSources) ++ webSocketOrigin

    s"default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; " +
      s"form-action 'self' $secureOrigin; manifest-src 'self'; " +
      s"script-src 'self' 'unsafe-inline' 'unsafe-eval' $HCaptchaSources; " +
      s"style-src 'self' 'unsafe-inline' $HCaptchaSources; " +
      s"img-src 'self' $PingAdminImageSource; font-src 'self'; " +
      s"connect-src ${connectSources.mkString(" ")}; frame-src 'self' $HCaptchaSources"

  private def parseOrigin(url: String): String =
    try
      origin(new URI(url, true, "UTF-8"), "WSUrl")
    catch
      case e: URIException =>
        throw new RuntimeException(s"Invalid WSUrl property: ${e.getMessage}")

  private def origin(uri: URI, propertyName: String): String =
    val scheme = Option(uri.getScheme).getOrElse {
      throw new RuntimeException(s"Invalid $propertyName property: missing scheme")
    }
    val host = Option(uri.getHost).getOrElse {
      throw new RuntimeException(s"Invalid $propertyName property: missing host")
    }

    if uri.getPort == -1 then
      s"$scheme://$host"
    else
      s"$scheme://$host:${uri.getPort}"

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) =
    if request.isSecure && config.enableHsts() then
      response.addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

    response.addHeader("X-Content-Type-Options", "nosniff")
    response.addHeader("X-Frame-Options", "SAMEORIGIN")
    response.setHeader("Content-Security-Policy", contentSecurityPolicy)

    true
