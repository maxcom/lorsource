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
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

import java.time.Instant

class LogoutWebTest extends FunSuite with WebHelper:
  authorized().test("logout clears CSRF_TOKEN, JSESSIONID and remember_me cookies"): auth =>
    val response = basicRequest
      .body(Map("csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("logout"))
      .send(backend)

    assertEquals(response.code, StatusCode.Found)

    // Порядок Set-Cookie в sttp не совпадает с порядком на проводе (HttpHeaders.map()),
    // поэтому проверяем существование deletion-куки, а не позицию в списке.
    // Jetty для maxAge=0 эмитит Expires=epoch (без Max-Age)
    for name <- Seq(AuthCookie, "JSESSIONID", CSRFProtectionService.CSRF_COOKIE) do
      val deletion = response
        .unsafeCookies
        .find { c =>
          c.name == name && c.value.isEmpty && (c.maxAge.contains(0L) || c.expires.contains(Instant.EPOCH))
        }
      assert(deletion.isDefined, s"expected deletion Set-Cookie for $name, got: ${response.unsafeCookies}")

  test("fresh CSRF_TOKEN cookie is issued when absent"):
    val response = basicRequest.followRedirects(false).get(MainUrl.addPath("login.jsp")).send(backend)

    assertEquals(response.code, StatusCode.Ok)

    val csrfCookie = response.unsafeCookies.find(_.name == CSRFProtectionService.CSRF_COOKIE)
    assert(csrfCookie.isDefined, "expected a new CSRF_TOKEN cookie")
    assert(csrfCookie.get.maxAge.exists(_ > 0), "new CSRF_TOKEN cookie must be persistent")
