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

package ru.org.linux.site

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

/** Страницы 404, возвращаемые контроллерами напрямую (ModelAndView errors/code404 без исключения), обязаны иметь статус
  * 404: статус для такой вьюшки не ставится ни SimpleMappingExceptionResolver (statusCodes), ни самой
  * errors/code404.jsp.
  */
object NotFoundPageWebTest:
  /** Пользователь из demo.db без сообщений. */
  private val UserWithoutTopics = "ivlad"

  /** Тег из demo.db (6 тем): offset=60 гарантированно за концом ленты. */
  private val TagName = "lor"

class NotFoundPageWebTest extends FunSuite with WebHelper:
  import NotFoundPageWebTest.*

  test("форум с несуществующим тегом отдаёт 404"):
    val response = basicRequest.get(uri"${MainUrl}forum/general/?tag=xnotexist").send(backend)

    assertEquals(response.code, StatusCode.NotFound, "status code")

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())
    assert(doc.select("h1").text().contains("404"), s"unexpected body: ${doc.select("h1").text()}")

  test("профиль пользователя без сообщений отдаёт 404"):
    val response = basicRequest.get(uri"${MainUrl}people/$UserWithoutTopics/").send(backend)

    assertEquals(response.code, StatusCode.NotFound, "status code")

  test("тег с пустой лентой (offset за концом) отдаёт 404"):
    val response = basicRequest.get(uri"${MainUrl}tag/$TagName?section=1&offset=60").send(backend)

    assertEquals(response.code, StatusCode.NotFound, "status code")
