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
package ru.org.linux.group

import com.sun.jersey.api.client.{Client, ClientResponse}
import org.apache.commons.httpclient.HttpStatus
import org.jsoup.Jsoup
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import ru.org.linux.test.WebHelper

import javax.ws.rs.core.Cookie

@RunWith(classOf[JUnitRunner])
class GroupControllerWebTest extends Specification {
  private val resource = {
    val client = new Client
    client.setFollowRedirects(false)
    client.resource(WebHelper.MAIN_URL)
  }

  class AuthenticatedUser(user: String) extends Scope {
    val auth: String = WebHelper.doLogin(resource, user, "passwd")
  }

  "talks page" should {
    "contain info" in {
      val cr = resource.path("/forum/talks/").get(classOf[ClientResponse])

      cr.getStatus must be equalTo HttpStatus.SC_OK

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      doc.select(".infoblock").text must not be empty
    }

    "contain info and edit link for moderator" in new AuthenticatedUser("maxcom") {
      val cr = resource.path("/forum/talks/")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(classOf[ClientResponse])

      cr.getStatus must be equalTo HttpStatus.SC_OK

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      doc.select(".infoblock").text must not be empty

      // у модератора в последнем абзаце groupInfo ссылка на изменение groupinfo
      doc.select(".infoblock p").last.text must be equalTo "[править]"

      doc.select(".infoblock p").last.select("a").attr("href") must be equalTo "groupmod.jsp?group=8404"
    }
  }

  "job page" should {
    "contain empty info for moderator" in new AuthenticatedUser("maxcom")  {
      val cr = resource.path("/forum/job/")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(classOf[ClientResponse])

      cr.getStatus must be equalTo HttpStatus.SC_OK

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      // кстати, у форумов без userinfo кнопочки нет (
      doc.select(".infoblock").text must be empty

      doc.select(".infoblock p").text must be empty
    }
  }
}