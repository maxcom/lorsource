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
package ru.org.linux.test

import org.jsoup.Jsoup
import org.junit.Assert
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.Fixture
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import sttp.client3.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object WebHelper {
  val AuthCookie = "remember_me"
  val MainUrl: Uri = Uri.unsafeParse("http://127.0.0.1:8080/")

  val TestUser = "Shaman007"
  val TestPassword = "passwd"

  val backend: SttpBackend[Identity, Any] = HttpClientSyncBackend()

  def doLogin(user: String = TestUser, password: String = TestPassword): String = {
    val response = basicRequest
      .body(Map("nick" -> user, "passwd" -> password, "csrf" -> "csrf"))
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .post( MainUrl.addPath("login_process"))
      .followRedirects(false)
      .send(backend)

    Assert.assertEquals(StatusCode.Found, response.code)

    response.unsafeCookies.find(_.name == AuthCookie).map(_.value).orNull
  }

  def createTopic(auth: String, groupId: Int, title: String): Either[String, Int] = {
    val response = basicRequest
      .body(Map(
        "section" -> Section.SECTION_FORUM.toString,
        "group" -> groupId.toString,
        "csrf" -> "csrf",
        "title" -> title))
      .cookie(WebHelper.AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(WebHelper.MainUrl.addPath("add.jsp"))
      .send(WebHelper.backend)

    if (response.code == StatusCode.SeeOther && response.header(HeaderNames.Location).isDefined) {
      val parsed = Uri.parse(response.header(HeaderNames.Location).get)

      parsed.map(u => u.pathSegments.segments.last.v.toInt)
    } else {
      val doc = Jsoup.parse(response.body.merge, response.request.uri.toString())

      Left(doc.select(".error").text())
    }
  }

  def Authorized(user: String = TestUser, password: String = TestPassword): Fixture[String] = new Fixture[String] {
    override def apply[R: AsResult](f: String => R): Result = {
      val auth = doLogin(user, password)

      AsResult(f(auth))
    }
  }
}