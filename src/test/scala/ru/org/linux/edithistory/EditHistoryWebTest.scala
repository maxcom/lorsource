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
package ru.org.linux.edithistory

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object EditHistoryWebTest:
  private val TestGroup = 4068
  private val GroupUrl = "linux-org-ru"
  private val TestTitle = "Edit History Web Test"

class EditHistoryWebTest extends FunSuite with WebHelper:
  import EditHistoryWebTest.*

  private def historyUrl(topicId: Int, commentId: Int): Uri =
    MainUrl.addPath("forum", GroupUrl, topicId.toString, commentId.toString, "history")

  private def topicUrl(topicId: Int): Uri = MainUrl.addPath("forum", GroupUrl, topicId.toString)

  private def postComment(auth: String, topicId: Int): Int =
    val postResponse = basicRequest
      .body(
        Map(
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

    postResponse
      .header(HeaderNames.Location)
      .map(Uri.parse)
      .flatMap(_.toOption)
      .flatMap(_.params.get("cid"))
      .map(_.toInt)
      .getOrElse(0)

  private def editComment(auth: String, topicId: Int, commentId: Int): Unit =
    val editResponse = basicRequest
      .body(
        Map(
          "section" -> Section.Forum.toString,
          "group" -> TestGroup.toString,
          "topic" -> topicId.toString,
          "original" -> commentId.toString,
          "msg" -> "not so blah blah blah",
          "csrf" -> "csrf"
        ))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .post(MainUrl.addPath("edit_comment"))
      .send(backend)

    assertEquals(editResponse.code, StatusCode.Ok, "edit should succeed")

    val editDoc = Jsoup.parse(editResponse.body.merge, MainUrl.toString())

    assert(editDoc.select(".error").text().isEmpty, "no errors in edit")

  private def deleteCommentQuietly(auth: String, commentId: Int): Unit =
    basicRequest
      .body(
        Map(
          "msgid" -> commentId.toString,
          "reason" -> "test cleanup",
          "bonus" -> "0",
          "delete_replys" -> "false",
          "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("delete_comment.jsp"))
      .send(backend)

  private def withEditedComment(test: (Int, Int, String) => Unit): Unit =
    val auth = doLogin()

    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    var commentId = 0

    try
      commentId = postComment(auth, topicId)
      assert(commentId > 0, "comment should be posted")

      editComment(auth, topicId, commentId)

      test(topicId, commentId, auth)
    finally
      if commentId > 0 then
        deleteCommentQuietly(auth, commentId)

      deleteTopic(auth, topicId)

  test("comment history permissions"):
    withEditedComment: (topicId, commentId, auth) =>
      val anonymousResponse = basicRequest.get(historyUrl(topicId, commentId)).send(backend)

      assertEquals(anonymousResponse.code, StatusCode.Forbidden, "anonymous should be forbidden")

      val userAuth = doLogin("edo", "passwd")

      val userResponse = basicRequest.cookie(AuthCookie, userAuth).get(historyUrl(topicId, commentId)).send(backend)

      assertEquals(userResponse.code, StatusCode.Ok, "authorized user should view history")

      deleteComment(auth, commentId)

      val deletedUserResponse = basicRequest
        .cookie(AuthCookie, userAuth)
        .get(historyUrl(topicId, commentId))
        .send(backend)

      assertEquals(
        deletedUserResponse.code,
        StatusCode.Forbidden,
        "deleted comment history should be forbidden for non-author")

      val authorResponse = basicRequest.cookie(AuthCookie, auth).get(historyUrl(topicId, commentId)).send(backend)

      assertEquals(authorResponse.code, StatusCode.Ok, "author should view history of own deleted comment")

      val moderatorAuth = doLogin("maxcom", "passwd")

      val moderatorResponse = basicRequest
        .cookie(AuthCookie, moderatorAuth)
        .get(historyUrl(topicId, commentId))
        .send(backend)

      assertEquals(moderatorResponse.code, StatusCode.Ok, "moderator should view history of deleted comment")

  test("comment history with foreign comment id"):
    withEditedComment: (_, commentId, auth) =>
      val otherTopicId = createTopic(auth, TestGroup, s"$TestTitle (other)").fold(v => fail(v), identity)

      try
        val response = basicRequest.get(historyUrl(otherTopicId, commentId)).send(backend)

        assertEquals(response.code, StatusCode.NotFound, "foreign comment id should be not found")
      finally
        deleteTopic(auth, otherTopicId)

  test("history link is visible to authorized users only"):
    withEditedComment: (topicId, commentId, _) =>
      val linkSelector = s"a[href=/forum/$GroupUrl/$topicId/$commentId/history]"

      val anonymousDoc = Jsoup.parse(basicRequest.get(topicUrl(topicId)).send(backend).body.merge, MainUrl.toString())

      assert(anonymousDoc.select(linkSelector).isEmpty, "anonymous should not see the history link")
      assert(anonymousDoc.html().contains("исправлений: 1"), "edit summary should be visible to anonymous")

      val userAuth = doLogin("edo", "passwd")

      val userDoc = Jsoup.parse(
        basicRequest.cookie(AuthCookie, userAuth).get(topicUrl(topicId)).send(backend).body.merge,
        MainUrl.toString())

      assert(!userDoc.select(linkSelector).isEmpty, "authorized user should see the history link")
