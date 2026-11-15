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
import ru.org.linux.test.WebHelper
import sttp.client4.*

// CommonContextFilter отдаёт ник в X-Lor-User — nginx логирует его в access.log
// ($upstream_http_x_lor_user) и прячет от клиента (proxy_hide_header)
class UserHeaderWebTest extends FunSuite with WebHelper:
  private val UserHeader = "X-Lor-User"

  authorized().test("X-Lor-User header contains nick for authorized request"): auth =>
    val response = basicRequest
      .cookie(AuthCookie, auth)
      .get(MainUrl)
      .send(backend)

    assertEquals(response.header(UserHeader), Some(TestUser))

  test("X-Lor-User header is absent for anonymous request"):
    val response = basicRequest
      .get(MainUrl)
      .send(backend)

    assert(response.header(UserHeader).isEmpty, s"expected no $UserHeader header for anonymous request")
