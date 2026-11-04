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

package ru.org.linux.exception

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.test.WebHelper
import sttp.client4.basicRequest
import sttp.model.{HeaderNames, StatusCode}

class MvcClientErrorWebTest extends FunSuite with WebHelper:
  test("GET на POST-only адрес отдаёт страницу 405 с заголовком Allow"):
    val response = basicRequest.get(MainUrl.addPath("add_comment_ajax")).send(backend)

    assertEquals(response.code, StatusCode.MethodNotAllowed)

    val allow = response.header(HeaderNames.Allow)
    assert(allow.exists(_.contains("POST")), s"Allow header: $allow")

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())
    assert(doc.select("h1").text().contains("405"), s"unexpected body: ${doc.select("h1").text()}")

  test("отсутствие обязательного параметра отдаёт страницу bad-parameter"):
    val response = basicRequest.get(MainUrl.addPath("show-topics.jsp")).send(backend)

    assertEquals(response.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())
    assert(doc.text().contains("некорректные"), s"unexpected body: ${doc.text().take(300)}")

  test("comment-message.jsp без параметра topic отдаёт 400 с сообщением валидатора"):
    val response = basicRequest.get(MainUrl.addPath("comment-message.jsp")).send(backend)

    assertEquals(response.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())
    assert(doc.select("h1").text().contains("тема не задана"), s"unexpected body: ${doc.select("h1").text()}")
