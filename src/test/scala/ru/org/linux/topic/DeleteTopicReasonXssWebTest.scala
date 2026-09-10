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
package ru.org.linux.topic

import munit.FunSuite
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object DeleteTopicReasonXssWebTest:
  private val TestGroup = 4068
  private val XssReason = "<img src=x onerror=alert(1)>"
  private val EscapedReason = "&lt;img src=x onerror=alert(1)&gt;"

class DeleteTopicReasonXssWebTest extends FunSuite with WebHelper:
  import DeleteTopicReasonXssWebTest.*

  private def assertReasonEscaped(body: String): Unit =
    assert(!body.contains(XssReason), "raw deletion reason must not appear in HTML")
    assert(body.contains(EscapedReason), "escaped deletion reason should appear in HTML")

  authorized().test("deletion reason is escaped on deleted topic page"): auth =>
    val topicId = createTopic(auth, TestGroup, "Test deletion reason escaping (topic page)").fold(
      v => fail(v),
      identity)

    deleteTopic(auth, topicId, reason = XssReason)

    // view-message.jsp redirects to the canonical topic link; follow manually to keep the auth cookie
    val redirect = basicRequest
      .get(uri"${MainUrl}view-message.jsp?msgid=$topicId")
      .cookie(AuthCookie, auth)
      .followRedirects(false)
      .send(backend)

    assertEquals(redirect.code, StatusCode.Found, "redirect status code")

    val location = redirect.header(HeaderNames.Location).getOrElse(fail("Location header missing"))
    val topicUri = Uri.parse(MainUrl.toString.stripSuffix("/") + location).fold(e => fail(e), identity)

    val response = basicRequest.get(topicUri).cookie(AuthCookie, auth).send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    assertReasonEscaped(response.body.merge)

  authorized().test("deletion reason is escaped on user deleted-topics page"): auth =>
    val topicId = createTopic(auth, TestGroup, "Test deletion reason escaping (deleted-topics page)").fold(
      v => fail(v),
      identity)

    deleteTopic(auth, topicId, reason = XssReason)

    val response = basicRequest
      .get(uri"${MainUrl}people/$TestUser/deleted-topics")
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    assertReasonEscaped(response.body.merge)
