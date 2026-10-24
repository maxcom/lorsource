/*
 *  Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.org.linux.topic

import munit.FunSuite
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

object SectionRssWebTest:
  private val TestGroup = 4068

class SectionRssWebTest extends FunSuite with WebHelper:
  import SectionRssWebTest.*
  test("section RSS feed returns valid RSS"):
    val response = basicRequest.get(MainUrl.addPath("section-rss.jsp")).send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")
    assert(response.contentType.exists(_.startsWith("application/rss+xml")), s"content type: ${response.contentType}")
    val body = response.body.getOrElse("")
    assert(body.contains("<rss"), "body contains <rss")
    assert(body.contains("<channel"), "body contains <channel")

  test("forum section RSS feed with filter returns created topic"):
    val auth = doLogin()
    val topicId = createTopic(auth, TestGroup, "Section RSS Web Test").fold(v => fail(v), identity)

    try
      val response = basicRequest
        .get(MainUrl.addPath("section-rss.jsp").addParam("section", "2").addParam("filter", "notalks"))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, "status code")
      assert(response.contentType.exists(_.startsWith("application/rss+xml")), s"content type: ${response.contentType}")
      val body = response.body.getOrElse("")
      assert(body.contains("<item"), "body contains <item")
      assert(body.contains(s"/forum/linux-org-ru/$topicId"), "created topic is in the feed")
      assert(body.contains("без talks"), "channel title contains filter name")
    finally
      deleteTopic(auth, topicId)
