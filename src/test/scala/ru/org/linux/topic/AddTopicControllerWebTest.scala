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

import com.sun.jersey.api.client.{Client, ClientResponse, WebResource}
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.apache.commons.httpclient.HttpStatus
import org.jsoup.Jsoup
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper

import javax.ws.rs.core.Cookie
import scala.jdk.CollectionConverters.ListHasAsScala

object AddTopicControllerWebTest {
  private val TestGroup = 4068
  private val TestUser = "Shaman007"
  private val TestPassword = "passwd"
  private val TestTitle = "Test Title"
}

@RunWith(classOf[JUnitRunner])
class AddTopicControllerWebTest extends Specification {
  private val resource: WebResource = {
    val client = new Client
    client.setFollowRedirects(false)

    client.resource(WebHelper.MAIN_URL)
  }

  "post form" should {
    "open and have CSRF" in {
      val cr = resource.path("add-section.jsp")
        .queryParam("section", Integer.toString(Section.SECTION_NEWS))
        .get(classOf[ClientResponse])

      cr.getStatus must be equalTo HttpStatus.SC_OK

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      doc.select("input[name=csrf]").asScala must not be empty
    }
  }

  "post action" should {
    "reject request without CSRF" in {
      val formData = new MultivaluedMapImpl

      formData.add("section", Integer.toString(Section.SECTION_FORUM))
      formData.add("group", Integer.toString(AddTopicControllerWebTest.TestGroup))

      val cr = resource.path("add.jsp").post(classOf[ClientResponse], formData)

      cr.getStatus must be equalTo HttpStatus.SC_OK

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      doc.select("#messageForm").asScala must not be empty
      doc.select(".error").asScala must not be empty
      doc.select("input[name=csrf]").asScala must not be empty
    }

    "perform post" in {
      val auth = WebHelper.doLogin(resource, AddTopicControllerWebTest.TestUser, AddTopicControllerWebTest.TestPassword)

      val formData = new MultivaluedMapImpl

      formData.add("section", Integer.toString(Section.SECTION_FORUM))
      formData.add("group", Integer.toString(AddTopicControllerWebTest.TestGroup))
      formData.add("csrf", "csrf")
      formData.add("title", AddTopicControllerWebTest.TestTitle)

      val cr = resource.path("add.jsp")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
        .post(classOf[ClientResponse], formData)

      val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)

      doc.select("#messageForm").asScala must be empty

      cr.getStatus must be equalTo HttpStatus.SC_MOVED_TEMPORARILY

      val tempPage = resource.uri(cr.getLocation).get(classOf[ClientResponse])
      tempPage.getStatus must be equalTo HttpStatus.SC_MOVED_TEMPORARILY

      val page = resource.uri(tempPage.getLocation).get(classOf[ClientResponse])

      page.getStatus must be equalTo HttpStatus.SC_OK

      val finalDoc = Jsoup.parse(page.getEntityInputStream, "UTF-8", resource.getURI.toString)

      finalDoc.select("h1[itemprop=headline] a").text must be equalTo AddTopicControllerWebTest.TestTitle
    }
  }
}