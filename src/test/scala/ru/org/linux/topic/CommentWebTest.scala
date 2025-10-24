/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.topic

import org.jsoup.Jsoup
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import ru.org.linux.topic.CommentWebTest.{TestGroup, TestTitle}
import sttp.client3.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object CommentWebTest {
  private val TestGroup = 4068
  private val TestTitle = "Comment Web Test"
}

@RunWith(classOf[JUnitRunner])
class CommentWebTest extends Specification {
  "post and edit" should {
    "post and edit" in WebHelper.Authorized() { auth =>
      val topicId = WebHelper.createTopic(auth, TestGroup, TestTitle).fold(v => throw new RuntimeException(v), identity)

      val postResponse = basicRequest
        .body(Map(
          "section" -> Section.SECTION_FORUM.toString,
          "group" -> TestGroup.toString,
          "topic" -> topicId.toString,
          "msg" -> "blah blah blah",
          "csrf" -> "csrf"))
        .cookie(WebHelper.AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
        .followRedirects(false)
        .post(WebHelper.MainUrl.addPath("add_comment.jsp"))
        .send(WebHelper.backend)

      postResponse.code must oneOf(StatusCode.Ok, StatusCode.SeeOther)

      val postDoc = Jsoup.parse(postResponse.body.merge, WebHelper.MainUrl.toString())

      postDoc.select(".error").text() must be empty

      val commentId = postResponse.header(HeaderNames.Location).map(Uri.parse).flatMap(_.toOption)
        .flatMap(_.params.get("cid")).map(_.toInt).getOrElse(0)

      val editResponse = basicRequest
        .body(Map(
          "section" -> Section.SECTION_FORUM.toString,
          "group" -> TestGroup.toString,
          "topic" -> topicId.toString,
          "original" -> commentId.toString,
          "msg" -> "not so blah blah blah",
          "csrf" -> "csrf"))
        .cookie(WebHelper.AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
        .post(WebHelper.MainUrl.addPath("edit_comment"))
        .send(WebHelper.backend)

      editResponse.code must be equalTo StatusCode.Ok

      val editDoc = Jsoup.parse(editResponse.body.merge, WebHelper.MainUrl.toString())

      editDoc.select(".error").text() must be empty
    }
  }
}