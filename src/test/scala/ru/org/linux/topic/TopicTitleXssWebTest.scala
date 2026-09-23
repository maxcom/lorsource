/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
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

/** Заголовок хранится в БД в исходном виде, экранирование выполняется при отображении. */
object TopicTitleXssWebTest:
  private val TestGroup = 4068
  private val XssTitle = "<img src=x onerror=alert(1)>"
  private val EscapedTitle = "&lt;img src=x onerror=alert(1)&gt;"

class TopicTitleXssWebTest extends FunSuite with WebHelper:
  import TopicTitleXssWebTest.*

  private def assertTitleEscaped(body: String): Unit =
    assert(!body.contains(XssTitle), "raw topic title must not appear in HTML")
    assert(body.contains(EscapedTitle), "escaped topic title should appear in HTML")

  private def fetchTopicUri(auth: String, topicId: Int): Uri =
    // view-message.jsp редиректит на каноническую ссылку топика
    val redirect = basicRequest
      .get(uri"${MainUrl}view-message.jsp?msgid=$topicId")
      .cookie(AuthCookie, auth)
      .followRedirects(false)
      .send(backend)

    assertEquals(redirect.code, StatusCode.Found, "redirect status code")

    val location = redirect.header(HeaderNames.Location).getOrElse(fail("Location header missing"))
    Uri.parse(MainUrl.toString.stripSuffix("/") + location).fold(e => fail(e), identity)

  authorized().test("topic title is escaped on topic page"): auth =>
    val topicId = createTopic(auth, TestGroup, XssTitle).fold(v => fail(v), identity)

    try
      val topicUri = fetchTopicUri(auth, topicId)

      val response = basicRequest.get(topicUri).cookie(AuthCookie, auth).send(backend)

      assertEquals(response.code, StatusCode.Ok, "status code")

      assertTitleEscaped(response.body.merge)
    finally
      deleteTopic(auth, topicId)

  authorized().test("topic title is escaped on group topic list"): auth =>
    val topicId = createTopic(auth, TestGroup, XssTitle).fold(v => fail(v), identity)

    try
      val response = basicRequest.get(MainUrl.addPath("forum", "linux-org-ru")).cookie(AuthCookie, auth).send(backend)

      assertEquals(response.code, StatusCode.Ok, "status code")

      assertTitleEscaped(response.body.merge)
    finally
      deleteTopic(auth, topicId)
