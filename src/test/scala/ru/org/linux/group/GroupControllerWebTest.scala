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
package ru.org.linux.group

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

class GroupControllerWebTest extends FunSuite with WebHelper:
  test("talks page contains info"):
    val response = basicRequest
      .get(MainUrl.addPath("forum", "talks"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(doc.select(".infoblock").text().nonEmpty, "infoblock should have text")

  authorized("maxcom").test("talks page contains info and edit link for moderator"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("forum", "talks"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(doc.select(".infoblock").text().nonEmpty, "infoblock should have text")

    assertEquals("[править]", doc.select(".infoblock p").last.text(), "last paragraph should be edit link")

    assertEquals("groupmod.jsp?group=8404", doc.select(".infoblock p").last.select("a").attr("href"), "edit link href")

  authorized("maxcom").test("job page contains empty info for moderator"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("forum", "job"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(!doc.select(".infoblock").text().contains("править"), "infoblock should not contain edit link")
