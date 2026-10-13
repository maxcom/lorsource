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
package ru.org.linux.tag

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

class TagControllerWebTest extends FunSuite with WebHelper:
  private val TagImgPayload = """"><img src=x onerror=alert(1)>"""
  private val TagScriptPayload = """"><script>alert(1)</script>"""

  test("tags change form requires moderator"):
    val response = basicRequest
      .get(MainUrl.addPath("tags", "change").addParam("tagName", "метка"))
      .followRedirects(false)
      .send(backend)

    assertEquals(response.code, StatusCode.Forbidden)

  authorized("maxcom").test("tags change form renders plain cancel link for moderator"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("tags", "change").addParam("tagName", "метка").addParam("firstLetter", "м"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assertEquals(doc.select("h1").text(), "Изменение тега метка")

    val cancel = doc.select("form a.btn-default")

    assertEquals(cancel.size, 1, "single cancel link")
    assertEquals(cancel.attr("href"), "/tags/м")
    assert(doc.select("[onclick]").isEmpty, "no onClick handlers")

  authorized("maxcom").test("tags change form rejects unvalidated tagName"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("tags", "change").addParam("tagName", TagImgPayload))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)
    assert(!response.body.merge.contains("onerror=alert(1)"), "payload must not be reflected")

  authorized("maxcom").test("tags change form rejects unvalidated firstLetter"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("tags", "change").addParam("tagName", "метка").addParam("firstLetter", TagScriptPayload))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)
    assert(!response.body.merge.contains("<script>alert(1)"), "payload must not be reflected")

  authorized("maxcom").test("tags delete form renders plain cancel link for moderator"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("tags", "delete").addParam("tagName", "метка"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assertEquals(doc.select("h1").text(), "Удаление метки «метка»")

    val cancel = doc.select("form a.btn-default")

    assertEquals(cancel.size, 1, "single cancel link")
    assertEquals(cancel.attr("href"), "/tags/")
    assert(doc.select("[onclick]").isEmpty, "no onClick handlers")

  authorized("maxcom").test("tags delete form rejects unvalidated tagName and firstLetter"): auth =>
    val response = basicRequest
      .get(MainUrl.addPath("tags", "delete").addParam("tagName", TagImgPayload).addParam("firstLetter", TagImgPayload))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)
    assert(!response.body.merge.contains("onerror=alert(1)"), "payload must not be reflected")

  authorized("maxcom").test("tags change submit rejects unvalidated oldTagName"): auth =>
    val response = basicRequest
      .body(Map("oldTagName" -> TagScriptPayload, "tagName" -> "метка", "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("tags", "change"))
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)
    assert(!response.body.merge.contains("<script>alert(1)"), "payload must not be reflected")

  authorized("maxcom").test("tags change submit rejects missing oldTagName"): auth =>
    val response = basicRequest
      .body(Map("tagName" -> "метка", "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("tags", "change"))
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)

  authorized("maxcom").test("tags list rejects multi-character firstLetter"): auth =>
    val response = basicRequest.get(MainUrl.addPath("tags", "ме")).cookie(AuthCookie, auth).send(backend)

    assertEquals(response.code, StatusCode.NotFound)

end TagControllerWebTest
