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
package ru.org.linux.topic

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

import scala.jdk.CollectionConverters._

object AddTopicControllerWebTest:
  private val TestGroup = 4068
  private val TestGroupNews = 2
  private val TestTitle = "Test Title"

class AddTopicControllerWebTest extends FunSuite with WebHelper:
  import AddTopicControllerWebTest.*

  test("post form opens and has CSRF"):
    val response = basicRequest
      .get(uri"${MainUrl}add-section.jsp?section=${Section.News}")
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(doc.select("input[name=csrf]").asScala.nonEmpty, "csrf input should exist")

  test("post action rejects request without CSRF"):
    val response = basicRequest
      .body(Map(
        "section" -> Section.Forum.toString,
        "group" -> TestGroup.toString))
      .post(MainUrl.addPath("add.jsp"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())

    assert(doc.select("#messageForm").asScala.nonEmpty, "message form should exist")
    assert(doc.select(".error").asScala.nonEmpty, "error should exist")
    assert(doc.select("input[name=csrf]").asScala.nonEmpty, "csrf input should exist")

  authorized().test("post action performs post"): auth =>
    val result = createTopic(auth, TestGroup, TestTitle)
    assert(result.isRight, "topic should be created successfully")

  test("post news without auth"):
    val response = basicRequest
      .body(Map(
        "nick" -> TestUser,
        "password" -> TestPassword,
        "h-captcha-response" -> "10000000-aaaa-bbbb-cccc-000000000001",
        "section" -> Section.News.toString,
        "group" -> TestGroupNews.toString,
        "csrf" -> "csrf",
        "title" -> "Новость без аутентификации"))
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .post(MainUrl.addPath("add.jsp"))
      .send(backend)

    val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(doc.select("#messageForm").asScala.isEmpty, "message form should be empty")

    assertEquals(response.code, StatusCode.Ok, "status code")

    val finalDoc = Jsoup.parse(response.body.merge, response.request.uri.toString())

    assert(finalDoc.text().contains("Вы поместили сообщение в защищенный раздел."), "should contain success message")
