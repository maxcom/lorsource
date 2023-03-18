/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.group

import org.jsoup.Jsoup
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import ru.org.linux.test.WebHelper
import sttp.client3.*
import sttp.model.StatusCode

@RunWith(classOf[JUnitRunner])
class GroupControllerWebTest extends Specification {
  class AuthenticatedUser(user: String) extends Scope {
    val auth: String = WebHelper.doLogin(user, "passwd")
  }

  "talks page" should {
    "contain info" in {
      val response = basicRequest
        .get(WebHelper.MainUrl.addPath("forum", "talks"))
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok

      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      doc.select(".infoblock").text must not be empty
    }

    "contain info and edit link for moderator" in new AuthenticatedUser("maxcom") {
      val response = basicRequest
        .get(WebHelper.MainUrl.addPath("forum", "talks"))
        .cookie(WebHelper.AuthCookie, auth)
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok

      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      doc.select(".infoblock").text must not be empty

      // у модератора в последнем абзаце groupInfo ссылка на изменение groupinfo
      doc.select(".infoblock p").last.text must be equalTo "[править]"

      doc.select(".infoblock p").last.select("a").attr("href") must be equalTo "groupmod.jsp?group=8404"
    }
  }

  "job page" should {
    "contain empty info for moderator" in new AuthenticatedUser("maxcom")  {
      val response = basicRequest
        .get(WebHelper.MainUrl.addPath("forum", "job"))
        .cookie(WebHelper.AuthCookie, auth)
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok

      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      // кстати, у форумов без userinfo кнопочки нет (
      doc.select(".infoblock").text must not contain("править")
    }
  }
}