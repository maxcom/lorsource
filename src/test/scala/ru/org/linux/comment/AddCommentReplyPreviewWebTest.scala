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

object AddCommentReplyPreviewWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Reply Preview Access Web Test"
  private val Marker = "xyzzy-deleted-comment-marker-7917"

class AddCommentReplyPreviewWebTest extends FunSuite with WebHelper:
  import AddCommentReplyPreviewWebTest.*

  private def replyFormUrl(topicId: Int, commentId: Int): Uri =
    MainUrl.addPath("add_comment.jsp").addParam("topic", topicId.toString).addParam("replyto", commentId.toString)

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

  private def getReplyFormHtml(auth: String, topicId: Int, commentId: Int): String =
    val response = basicRequest.cookie(AuthCookie, auth).get(replyFormUrl(topicId, commentId)).send(backend)

    assertEquals(response.code, StatusCode.Ok, "reply form should render")

    response.body.merge

  test("deleted comment body is not disclosed via reply form preview"):
    val auth = doLogin()

    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    var commentId = 0

    try
      commentId = postComment(auth, topicId)
      assert(commentId > 0, "comment should be posted")

      deleteComment(auth, commentId)

      val anonymousHtml = getReplyFormHtml(null, topicId, commentId)

      assert(!anonymousHtml.contains(Marker), "anonymous should not see deleted comment body in reply preview")
      assert(anonymousHtml.contains("нельзя комментировать удаленные комментарии"), "form error should be shown")

      val authorHtml = getReplyFormHtml(auth, topicId, commentId)

      assert(authorHtml.contains(Marker), "author should see own deleted comment in reply preview")

      val moderatorAuth = doLogin("maxcom", "passwd")

      val moderatorHtml = getReplyFormHtml(moderatorAuth, topicId, commentId)

      assert(moderatorHtml.contains(Marker), "moderator should see deleted comment in reply preview")
    finally
      deleteTopic(auth, topicId)

  test("comment body in deleted topic is not disclosed via reply form preview"):
    val auth = doLogin()

    val topicId = createTopic(auth, TestGroup, s"$TestTitle (deleted topic)").fold(v => fail(v), identity)

    val commentId = postComment(auth, topicId)

    assert(commentId > 0, "comment should be posted")

    val moderatorAuth = doLogin("maxcom", "passwd")

    deleteTopic(moderatorAuth, topicId)

    val anonymousResponse = basicRequest.get(replyFormUrl(topicId, commentId)).send(backend)

    assertEquals(anonymousResponse.code, StatusCode.Ok, "reply form should render")

    val anonymousHtml = anonymousResponse.body.merge

    assert(!anonymousHtml.contains(Marker), "anonymous should not see comment body of deleted topic in reply preview")
