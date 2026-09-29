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
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

class CanonicalRedirectWebTest extends FunSuite with WebHelper:
  private def assertRedirect(path: String, location: String): Unit =
    val response = basicRequest.get(Uri.unsafeParse(s"$MainUrl$path")).followRedirects(false).send(backend)

    assertEquals(response.code, StatusCode.MovedPermanently, s"status code for /$path")
    assertEquals(response.header(HeaderNames.Location), Some(location), s"location for /$path")

  test("user topics without trailing slash redirects to canonical form"):
    assertRedirect("people/maxcom", "/people/maxcom/")

  test("user topics redirect keeps query string"):
    assertRedirect("people/maxcom?output=rss", "/people/maxcom/?output=rss")

  test("user topics canonical URL with trailing slash opens"):
    val response = basicRequest.get(Uri.unsafeParse(s"${MainUrl}people/maxcom/")).send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("user remark with trailing slash redirects to canonical form"):
    assertRedirect("people/maxcom/remark/", "/people/maxcom/remark")

  test("tracker with trailing slash redirects to canonical form"):
    assertRedirect("tracker/", "/tracker")

  test("tags without trailing slash redirects to canonical form"):
    assertRedirect("tags", "/tags/")

  test("tag letter page with trailing slash redirects to canonical form"):
    assertRedirect("tags/a/", "/tags/a")

  test("group archive with trailing slash redirects to canonical form"):
    assertRedirect("forum/talks/archive/", "/forum/talks/archive")

  test("doubled leading slash does not redirect off-site"):
    val response = basicRequest.get(Uri.unsafeParse(s"$MainUrl//evil.com/")).followRedirects(false).send(backend)

    assert(
      response.header(HeaderNames.Location).forall(!_.startsWith("//")),
      s"off-site redirect to ${response.header(HeaderNames.Location)}"
    )
    assert(response.code != StatusCode.MovedPermanently, s"unexpected redirect to ${response.header(HeaderNames.Location)}")
