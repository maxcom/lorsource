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
import sttp.client4.testing.{ResponseStub, SyncBackendStub}
import sttp.client4.wrappers.FollowRedirectsBackend
import sttp.model.{Header, HeaderNames, StatusCode}

class GeoLocationServiceTest extends FunSuite:
  // Пустой стаб: если сервис всё же отправит запрос, send бросит IllegalArgumentException ("No behavior stubbed")
  test("некорректный ip отклоняется без исходящего запроса") {
    val service = GeoLocationService(SyncBackendStub)

    assertEquals(service.getLocation("8.8.8.8/../../internal"), Left("Invalid IP address: 8.8.8.8/../../internal"))
    assertEquals(service.getLocation("evil.com"), Left("Invalid IP address: evil.com"))
    assertEquals(service.getLocation(""), Left("Invalid IP address: "))
  }

  test("валидный ipv4 уходит на https://ipwho.is без следования редиректам и разбирается") {
    val okBody =
      """{"success":true,"country":"US","region":"California","city":"Mountain View","connection":{"org":"Google"}}"""
    val backend = SyncBackendStub
      .whenRequestMatches(r => r.uri.toString == "https://ipwho.is/8.8.8.8" && !r.options.followRedirects)
      .thenRespondAdjust(okBody)

    val service = GeoLocationService(backend)

    assertEquals(
      service.getLocation("8.8.8.8"),
      Right(GeoLocation(Some("US"), Some("California"), Some("Mountain View"), Some(Connection(Some("Google"))))))
  }

  test("валидный ipv6 принимается") {
    val backend = SyncBackendStub
      .whenRequestMatches(r =>
        r.uri.scheme.contains("https") && r.uri.host.exists(_.toString == "ipwho.is") && !r.options.followRedirects)
      .thenRespondAdjust("""{"success":true}""")

    val service = GeoLocationService(backend)

    assertEquals(service.getLocation("2001:db8::1"), Right(GeoLocation(None, None, None, None)))
  }

  test("3xx от ipwho.is не преследуется даже на бэкенде со включённым следованием редиректов") {
    val backend = FollowRedirectsBackend(
      SyncBackendStub
        .whenRequestMatches(r => r.uri.toString == "https://ipwho.is/8.8.8.8")
        .thenRespond(
          ResponseStub.adjust(
            "",
            StatusCode.Found,
            Header(HeaderNames.Location, "http://169.254.169.254/latest/meta-data/") :: Nil)))

    val service = GeoLocationService(backend)

    // при регрессии (следование редиректам) стаб бросит IllegalArgumentException на Location-URL
    assert(service.getLocation("8.8.8.8").isLeft)
  }
