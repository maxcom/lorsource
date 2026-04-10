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
package ru.org.linux.comment

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object CommentWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Comment Web Test"

class CommentWebTest extends FunSuite with WebHelper:
  import CommentWebTest.*

  authorized().test("post and edit"): auth =>
    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => throw new RuntimeException(v), identity)

    val postResponse = basicRequest
      .body(Map(
        "section" -> Section.Forum.toString,
        "group" -> TestGroup.toString,
        "topic" -> topicId.toString,
        "msg" -> "blah blah blah",
        "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("add_comment.jsp"))
      .send(backend)

    assert(postResponse.code == StatusCode.Ok || postResponse.code == StatusCode.SeeOther, "post should succeed")

    val postDoc = Jsoup.parse(postResponse.body.merge, MainUrl.toString())

    assert(postDoc.select(".error").text().isEmpty, "no errors in post")

    val commentId = postResponse.header(HeaderNames.Location).map(Uri.parse).flatMap(_.toOption)
      .flatMap(_.params.get("cid")).map(_.toInt).getOrElse(0)

    val editResponse = basicRequest
      .body(Map(
        "section" -> Section.Forum.toString,
        "group" -> TestGroup.toString,
        "topic" -> topicId.toString,
        "original" -> commentId.toString,
        "msg" -> "not so blah blah blah",
        "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .post(MainUrl.addPath("edit_comment"))
      .send(backend)

    assertEquals(editResponse.code, StatusCode.Ok, "edit should succeed")

    val editDoc = Jsoup.parse(editResponse.body.merge, MainUrl.toString())

    assert(editDoc.select(".error").text().isEmpty, "no errors in edit")
