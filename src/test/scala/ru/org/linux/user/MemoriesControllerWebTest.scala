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
package ru.org.linux.user

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

/** POST /memories.jsp?add проверяет видимость топика через TopicPermissionService.checkView, как и страница топика:
  * несуществующий, удаленный и чужой черновик неотличимы друг от друга (404), обычный топик — 200.
  */
object MemoriesControllerWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Memories Controller Web Test"
  private val DraftTitle = "Memories Controller Web Test (draft)"
  private val NonexistentMsgid = 99999999

class MemoriesControllerWebTest extends FunSuite with WebHelper:
  import MemoriesControllerWebTest.*

  private def postAdd(auth: String, msgid: Int): Response[Either[String, String]] =
    basicRequest
      .body(Map("add" -> "add", "msgid" -> msgid.toString, "watch" -> "false", "csrf" -> "csrf"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("memories.jsp"))
      .send(backend)
  end postAdd

  private def createDraft(auth: String): Either[String, Int] =
    val response = basicRequest
      .body(
        Map(
          "section" -> Section.Forum.toString,
          "group" -> TestGroup.toString,
          "csrf" -> "csrf",
          "title" -> DraftTitle,
          "msg" -> "черновик для MemoriesControllerWebTest",
          "draft" -> "Сохранить в черновики"
        ))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("add.jsp"))
      .send(backend)

    if (response.code == StatusCode.SeeOther || response.code == StatusCode.Found) &&
      response.header(HeaderNames.Location).isDefined
    then
      Uri.parse(response.header(HeaderNames.Location).get).map(u => u.pathSegments.segments.last.v.toInt)
    else
      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())
      Left(doc.select(".error").text())
  end createDraft

  test("add favorite to normal topic returns 200"):
    val authorAuth = doLogin()
    val topicId = createTopic(authorAuth, TestGroup, TestTitle).fold(v => fail(v), identity)

    try
      val userAuth = doLogin("edo", "passwd")

      val response = postAdd(userAuth, topicId)

      assertEquals(response.code, StatusCode.Ok, s"body: ${response.body.merge}")
      assert(response.body.merge.contains("\"id\""), s"unexpected body: ${response.body.merge}")
    finally
      deleteTopic(authorAuth, topicId)

  test("add favorite to nonexistent topic returns 404"):
    val userAuth = doLogin("edo", "passwd")

    val response = postAdd(userAuth, NonexistentMsgid)

    assertEquals(response.code, StatusCode.NotFound, s"body: ${response.body.merge}")

  test("add favorite to another user's draft is indistinguishable from nonexistent (404)"):
    val authorAuth = doLogin()
    val topicId = createDraft(authorAuth).fold(v => fail(v), identity)

    try
      val userAuth = doLogin("edo", "passwd")

      val response = postAdd(userAuth, topicId)

      assertEquals(response.code, StatusCode.NotFound, s"body: ${response.body.merge}")
    finally
      deleteTopic(authorAuth, topicId)

  test("add favorite to deleted topic is indistinguishable from nonexistent (404)"):
    val authorAuth = doLogin()
    val topicId = createTopic(authorAuth, TestGroup, TestTitle).fold(v => fail(v), identity)
    deleteTopic(authorAuth, topicId)

    val userAuth = doLogin("edo", "passwd")

    val response = postAdd(userAuth, topicId)

    assertEquals(response.code, StatusCode.NotFound, s"body: ${response.body.merge}")

end MemoriesControllerWebTest
