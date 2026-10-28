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
import sttp.model.{HeaderNames, StatusCode}

class TopicListControllerWebTest extends FunSuite with WebHelper:
  test("TopicListController loads archive with 200 code"):
    val response = basicRequest.get(MainUrl.addPath("news", "archive", "2007", "5")).send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("polls section loads with 200 code"):
    val response = basicRequest.get(uri"${MainUrl}polls/").send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("show-topics.jsp redirects valid nick to people page"):
    val response = basicRequest
      .get(MainUrl.addPath("show-topics.jsp").addParam("nick", "maxcom"))
      .followRedirects(false)
      .send(backend)
    assertEquals(response.code, StatusCode.Found, "status code")
    assertEquals(response.header(HeaderNames.Location), Some("/people/maxcom/"), "location")

  test("show-topics.jsp redirects valid nick to rss feed"):
    val response = basicRequest
      .get(MainUrl.addPath("show-topics.jsp").addParam("nick", "maxcom").addParam("output", "rss"))
      .followRedirects(false)
      .send(backend)
    assertEquals(response.code, StatusCode.Found, "status code")
    assertEquals(response.header(HeaderNames.Location), Some("/people/maxcom/?output=rss"), "location")

  test("show-topics.jsp rejects nick with CRLF injection"):
    val response = basicRequest
      .get(MainUrl.addPath("show-topics.jsp").addParam("nick", "maxcom\r\nSet-Cookie: x"))
      .followRedirects(false)
      .send(backend)
    assertEquals(response.code, StatusCode.InternalServerError, "status code")
    assert(clue(response.header(HeaderNames.Location)).isEmpty, "no redirect header must be sent")
