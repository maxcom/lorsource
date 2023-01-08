/*
 * Copyright 1998-2022 Linux.org.ru
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
import sttp.client3.*
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.*

object AddTopicControllerWebTest {
  private val TestGroup = 4068
  private val TestUser = "Shaman007"
  private val TestPassword = "passwd"
  private val TestTitle = "Test Title"
}

@RunWith(classOf[JUnitRunner])
class AddTopicControllerWebTest extends Specification {
  "post form" should {
    "open and have CSRF" in {
      val response = basicRequest
        .get(uri"${WebHelper.MainUrl}add-section.jsp?section=${Section.SECTION_NEWS}")
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok

      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      doc.select("input[name=csrf]").asScala must not be empty
    }
  }

  "post action" should {
    "reject request without CSRF" in {
      val response = basicRequest
        .body(Map(
          "section" -> Section.SECTION_FORUM.toString,
          "group" -> AddTopicControllerWebTest.TestGroup.toString))
        .post(WebHelper.MainUrl.addPath("add.jsp"))
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok

      val doc = Jsoup.parse(response.body.merge, WebHelper.MainUrl.toString())

      doc.select("#messageForm").asScala must not be empty
      doc.select(".error").asScala must not be empty
      doc.select("input[name=csrf]").asScala must not be empty
    }

    "perform post" in {
      val auth = WebHelper.doLogin(AddTopicControllerWebTest.TestUser, AddTopicControllerWebTest.TestPassword)

      val response = basicRequest
        .body(Map(
          "section" -> Section.SECTION_FORUM.toString,
          "group" -> AddTopicControllerWebTest.TestGroup.toString,
          "csrf" -> "csrf",
          "title" -> AddTopicControllerWebTest.TestTitle))
        .cookie(WebHelper.AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
        .post(WebHelper.MainUrl.addPath("add.jsp"))
        .send(WebHelper.backend)

      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      doc.select("#messageForm").asScala must be empty

      response.code must be equalTo StatusCode.Ok

      val finalDoc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      finalDoc.select("h1[itemprop=headline] a").text must be equalTo AddTopicControllerWebTest.TestTitle
    }
  }
}