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

import munit.FunSuite
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import ru.org.linux.spring.SiteConfig

import java.util.Properties

class HstsInterceptorTest extends FunSuite:
  test("addsContentSecurityPolicyWithConfiguredOrigins"):
    val response = new MockHttpServletResponse

    interceptor("https://www.linux.org.ru/", Some("wss://www.linux.org.ru:8443/ws"), enableHsts = false).preHandle(
      new MockHttpServletRequest,
      response,
      null)

    assertEquals(
      response.getHeader("Content-Security-Policy"),
      "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; " +
        "form-action 'self' https://www.linux.org.ru; manifest-src 'self'; " +
        "script-src 'self' 'unsafe-inline' https://hcaptcha.com https://*.hcaptcha.com; " +
        "style-src 'self' 'unsafe-inline' https://hcaptcha.com https://*.hcaptcha.com; " +
        "img-src 'self' data: https://images.ping-admin.ru https://cdn.jsdelivr.net https://secure.gravatar.com; font-src 'self'; " +
        "connect-src 'self' https://hcaptcha.com https://*.hcaptcha.com wss://www.linux.org.ru:8443; " +
        "frame-src 'self' https://hcaptcha.com https://*.hcaptcha.com"
    )

  test("omitsWebSocketOriginWhenWsUrlIsMissing"):
    val response = new MockHttpServletResponse

    interceptor("https://www.linux.org.ru/", None, enableHsts = false).preHandle(
      new MockHttpServletRequest,
      response,
      null)

    assertEquals(
      response.getHeader("Content-Security-Policy"),
      "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'self'; " +
        "form-action 'self' https://www.linux.org.ru; manifest-src 'self'; " +
        "script-src 'self' 'unsafe-inline' https://hcaptcha.com https://*.hcaptcha.com; " +
        "style-src 'self' 'unsafe-inline' https://hcaptcha.com https://*.hcaptcha.com; " +
        "img-src 'self' data: https://images.ping-admin.ru https://cdn.jsdelivr.net https://secure.gravatar.com; font-src 'self'; " +
        "connect-src 'self' https://hcaptcha.com https://*.hcaptcha.com; " +
        "frame-src 'self' https://hcaptcha.com https://*.hcaptcha.com"
    )

  test("addsHstsOnlyForSecureRequests"):
    val interceptor = this.interceptor(
      "https://www.linux.org.ru/",
      Some("wss://www.linux.org.ru:8443/ws"),
      enableHsts = true)

    val secureRequest = new MockHttpServletRequest
    secureRequest.setSecure(true)
    val secureResponse = new MockHttpServletResponse
    interceptor.preHandle(secureRequest, secureResponse, null)

    assertEquals(secureResponse.getHeader("Strict-Transport-Security"), "max-age=31536000; includeSubDomains")

    val insecureResponse = new MockHttpServletResponse
    interceptor.preHandle(new MockHttpServletRequest, insecureResponse, null)

    assertEquals(insecureResponse.getHeader("Strict-Transport-Security"), null)

  private def interceptor(secureUrl: String, wsUrl: Option[String], enableHsts: Boolean): HstsInterceptor =
    val properties = new Properties()

    properties.setProperty("MainUrl", "http://www.linux.org.ru/")
    properties.setProperty("SecureUrl", secureUrl)
    properties.setProperty("EnableHsts", enableHsts.toString)
    wsUrl.foreach(properties.setProperty("WSUrl", _))

    new HstsInterceptor(new SiteConfig(properties))
