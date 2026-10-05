/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.topic

import munit.FunSuite
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode}

object ResolveControllerWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Resolve Controller Web Test"

class ResolveControllerWebTest extends FunSuite with WebHelper:
  import ResolveControllerWebTest.*

  authorized().test("POST resolve.jsp with CSRF token redirects to the topic"): auth =>
    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    try
      val response = basicRequest
        .body(Map("msgid" -> topicId.toString, "resolve" -> "yes", "csrf" -> "csrf"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
        .followRedirects(false)
        .post(MainUrl.addPath("resolve.jsp"))
        .send(backend)

      assertEquals(response.code, StatusCode.Found)

      val location = response.header(HeaderNames.Location)
      assert(location.isDefined, "redirect location expected")
      assert(location.get.contains(topicId.toString), s"expected redirect to the topic, got: $location")
    finally
      deleteTopic(auth, topicId)

  test("GET resolve.jsp is rejected"):
    val response = basicRequest.followRedirects(false).get(MainUrl.addPath("resolve.jsp")).send(backend)

    assertEquals(response.code, StatusCode.MethodNotAllowed)

  authorized().test("POST resolve.jsp without CSRF token is rejected"): auth =>
    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    try
      val response = basicRequest
        .body(Map("msgid" -> topicId.toString, "resolve" -> "yes"))
        .cookie(AuthCookie, auth)
        .followRedirects(false)
        .post(MainUrl.addPath("resolve.jsp"))
        .send(backend)

      assertEquals(response.code, StatusCode.Forbidden)
      assert(response.body.merge.contains("CSRF"), s"CSRF error message expected, got: ${response.body.merge}")
    finally
      deleteTopic(auth, topicId)
