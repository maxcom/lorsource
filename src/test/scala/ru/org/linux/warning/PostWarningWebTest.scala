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

package ru.org.linux.warning

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object PostWarningWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Post Warning Deleted Comment Web Test"
  private val Marker = "xyzzy-deleted-warning-marker-7918"

class PostWarningWebTest extends FunSuite with WebHelper:
  import PostWarningWebTest.*

  private def warningFormUrl(topicId: Int, commentId: Int): Uri =
    MainUrl.addPath("post-warning").addParam("topic", topicId.toString).addParam("comment", commentId.toString)

  private def postComment(auth: String, topicId: Int): Int =
    val response = basicRequest
      .body(
        Map(
          "section" -> Section.Forum.toString,
          "group" -> TestGroup.toString,
          "topic" -> topicId.toString,
          "msg" -> s"comment body $Marker",
          "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("add_comment.jsp"))
      .send(backend)

    assert(response.code == StatusCode.Ok || response.code == StatusCode.SeeOther, "post should succeed")

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())

    assert(doc.select(".error").text().isEmpty, "no errors in post")

    response
      .header(HeaderNames.Location)
      .map(Uri.parse)
      .flatMap(_.toOption)
      .flatMap(_.params.get("cid"))
      .map(_.toInt)
      .getOrElse(0)

  private def getWarningFormHtml(auth: String, topicId: Int, commentId: Int): String =
    val response = basicRequest.cookie(AuthCookie, auth).get(warningFormUrl(topicId, commentId)).send(backend)

    assertEquals(response.code, StatusCode.Ok, "warning form should render")

    response.body.merge

  private def postWarningErrorHtml(auth: String, topicId: Int, commentId: Int): String =
    val response = basicRequest
      .body(Map("topic" -> topicId.toString, "comment" -> commentId.toString, "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .post(MainUrl.addPath("post-warning"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "warning form should re-render on error")

    response.body.merge

  test("deleted comment body is not disclosed via post-warning form"):
    val auth = doLogin()

    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    var commentId = 0

    try
      commentId = postComment(auth, topicId)
      assert(commentId > 0, "comment should be posted")

      deleteComment(auth, commentId)

      val userAuth = doLogin("edo", "passwd")

      val userHtml = getWarningFormHtml(userAuth, topicId, commentId)

      assert(!userHtml.contains(Marker), "user should not see deleted comment body in warning form")
      assert(userHtml.contains("Комментарий удален"), "form error should be shown")

      val authorHtml = getWarningFormHtml(auth, topicId, commentId)

      assert(!authorHtml.contains(Marker), "author should not see deleted comment body in warning form")

      val moderatorAuth = doLogin("maxcom", "passwd")

      val moderatorHtml = getWarningFormHtml(moderatorAuth, topicId, commentId)

      assert(!moderatorHtml.contains(Marker), "moderator should not see deleted comment body in warning form")
    finally
      deleteTopic(auth, topicId)

  test("deleted comment body is not disclosed via post-warning form error re-render"):
    val auth = doLogin()

    val topicId = createTopic(auth, TestGroup, s"$TestTitle (post)").fold(v => fail(v), identity)

    var commentId = 0

    try
      commentId = postComment(auth, topicId)
      assert(commentId > 0, "comment should be posted")

      deleteComment(auth, commentId)

      val html = postWarningErrorHtml(auth, topicId, commentId)

      assert(!html.contains(Marker), "deleted comment body should not be rendered on form error re-render")
      assert(html.contains("Заполните комментарий"), "form error should be shown")
    finally
      deleteTopic(auth, topicId)
