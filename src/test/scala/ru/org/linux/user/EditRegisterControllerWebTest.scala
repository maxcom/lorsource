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
package ru.org.linux.user

import com.sun.jersey.api.client.{Client, ClientResponse, WebResource}
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.apache.commons.httpclient.HttpStatus
import org.jsoup.Jsoup
import org.junit.Assert.{assertEquals, assertNotNull}
import org.junit.{After, Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper

import javax.ws.rs.core.Cookie

object EditRegisterControllerWebTest {
  private val MAXCOM_NAME = "Максим Валянский"
  private val MAXCOM_URL = "http://maxcom.pp.ru/"
  private val MAXCOM_EMAIL = "max.valjanski+test93@gmail.com"
  private val MAXCOM_TOWN = "Москва"
  private val MAXCOM_INFO = "test<b>test</b>"
  private val MAXCOM_PASS = "passwd"
  private val JB_NAME = "Тёма"
  private val JB_URL = "http://darkmachine.org"
  private val JB_EMAIL = "mail@darkmachine.org"
  private val JB_TOWN = "Самара"
  private val JB_INFO = "[i]Эффективный менеджер по распилу гос-бабла[/i]"
  private val JB_PASS = "passwd"
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class EditRegisterControllerWebTest {
  private val resource: WebResource = {
    val client = new Client
    client.setFollowRedirects(false)
    client.resource(WebHelper.MainUrl.toString())
  }

  @Autowired
  private var userDao: UserDao = _

  @Autowired
  private var userService: UserService = _

  private def rescueMaxcom(): Unit = {
    val user = userDao.getUser(userDao.findUserId("maxcom"))
    userService.updateUser(user, EditRegisterControllerWebTest.MAXCOM_NAME, EditRegisterControllerWebTest.MAXCOM_URL, EditRegisterControllerWebTest.MAXCOM_EMAIL, EditRegisterControllerWebTest.MAXCOM_TOWN, EditRegisterControllerWebTest.MAXCOM_PASS, EditRegisterControllerWebTest.MAXCOM_INFO)
    userDao.acceptNewEmail(user, EditRegisterControllerWebTest.MAXCOM_EMAIL)
  }

  private def rescueJB(): Unit = {
    val user = userDao.getUser(userDao.findUserId("JB"))
    userService.updateUser(user, EditRegisterControllerWebTest.JB_NAME, EditRegisterControllerWebTest.JB_URL, EditRegisterControllerWebTest.JB_EMAIL, EditRegisterControllerWebTest.JB_TOWN, EditRegisterControllerWebTest.JB_PASS, EditRegisterControllerWebTest.JB_INFO)
    userDao.acceptNewEmail(user, EditRegisterControllerWebTest.JB_EMAIL)
    userDao.unblock(user, user)
  }

  @After
  @Before
  def clean(): Unit = {
    rescueMaxcom()
    rescueJB()
  }

  /**
   * Вводим теже данные которые и были изначально. После изменений должен быть
   * redirect в профиль
   * */
  @Test
  def testSimple(): Unit = {
    val auth = WebHelper.doLogin("JB", EditRegisterControllerWebTest.JB_PASS)

    val cr = resource.path("people/JB/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .get(classOf[ClientResponse])
    assertEquals(HttpStatus.SC_OK, cr.getStatus)

    val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)
    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"))

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(EditRegisterControllerWebTest.JB_NAME, name)
    assertEquals(EditRegisterControllerWebTest.JB_URL, url)
    assertEquals(EditRegisterControllerWebTest.JB_EMAIL, email)
    assertEquals(EditRegisterControllerWebTest.JB_TOWN, town)
    assertEquals(EditRegisterControllerWebTest.JB_INFO, info)

    val formData = new MultivaluedMapImpl
    formData.add("name", name)
    formData.add("url", url)
    formData.add("email", email)
    formData.add("town", town)
    formData.add("info", info)
    formData.add("csrf", "csrf")
    formData.add("oldpass", EditRegisterControllerWebTest.JB_PASS)

    val cr2 = resource.path("people/maxcom/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
      .post(classOf[ClientResponse], formData)

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr2.getStatus)
    assertEquals("http://127.0.0.1:8080/people/JB/profile", cr2.getLocation.toString)
  }

  @Test
  def testChangePassword(): Unit = {
    val auth = WebHelper.doLogin("maxcom", EditRegisterControllerWebTest.MAXCOM_PASS)

    val cr = resource.path("people/maxcom/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .get(classOf[ClientResponse])

    assertEquals(HttpStatus.SC_OK, cr.getStatus)

    val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)
    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(EditRegisterControllerWebTest.MAXCOM_NAME, name)
    assertEquals(EditRegisterControllerWebTest.MAXCOM_URL, url)
    assertEquals(EditRegisterControllerWebTest.MAXCOM_EMAIL, email)
    assertEquals(EditRegisterControllerWebTest.MAXCOM_TOWN, town)
    assertEquals(EditRegisterControllerWebTest.MAXCOM_INFO, info)

    val formData = new MultivaluedMapImpl
    formData.add("name", name)
    formData.add("url", url)
    formData.add("email", email)
    formData.add("town", town)
    formData.add("info", info)
    formData.add("csrf", "csrf")
    formData.add("oldpass", "passwd")
    formData.add("password", "passwd2")
    formData.add("password2", "passwd2")

    val cr2 = resource.path("people/maxcom/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
      .post(classOf[ClientResponse], formData)

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr2.getStatus)

    val newAuth = WebHelper.getAuthCookie(cr2)
    assertNotNull(newAuth)

    val cr3 = resource.uri(cr2.getLocation)
      .cookie(new Cookie(WebHelper.AuthCookie, newAuth, "/", "127.0.0.1", 1))
      .get(classOf[ClientResponse])

    assertEquals(HttpStatus.SC_OK, cr3.getStatus)

    val formData2 = new MultivaluedMapImpl

    formData2.add("name", name)
    formData2.add("url", url)
    formData2.add("email", email)
    formData2.add("town", town)
    formData2.add("info", info)
    formData2.add("csrf", "csrf")
    formData2.add("oldpass", "passwd2")
    formData2.add("password", "passwd")
    formData2.add("password2", "passwd")

    val cr4 = resource.path("people/maxcom/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, newAuth, "/", "127.0.0.1", 1))
      .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
      .post(classOf[ClientResponse], formData2)

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr4.getStatus)

    val newAuth2 = WebHelper.getAuthCookie(cr4)

    val cr5 = resource.uri(cr4.getLocation)
      .cookie(new Cookie(WebHelper.AuthCookie, newAuth2, "/", "127.0.0.1", 1))
      .get(classOf[ClientResponse])

    assertEquals(HttpStatus.SC_OK, cr5.getStatus)
  }

  @Test
  def testChange(): Unit = {
    val auth = WebHelper.doLogin("JB", EditRegisterControllerWebTest.JB_PASS)

    val cr = resource.path("people/JB/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .get(classOf[ClientResponse])

    assertEquals(HttpStatus.SC_OK, cr.getStatus)

    val doc = Jsoup.parse(cr.getEntityInputStream, "UTF-8", resource.getURI.toString)
    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"))

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(EditRegisterControllerWebTest.JB_NAME, name)
    assertEquals(EditRegisterControllerWebTest.JB_URL, url)
    assertEquals(EditRegisterControllerWebTest.JB_EMAIL, email)
    assertEquals(EditRegisterControllerWebTest.JB_TOWN, town)
    assertEquals(EditRegisterControllerWebTest.JB_INFO, info)

    val formData = new MultivaluedMapImpl
    formData.add("name", name)
    formData.add("url", url)
    formData.add("email", email)
    formData.add("town", town)
    formData.add("info", info)
    formData.add("csrf", "csrf")

    val cr2 = resource.path("people/JB/edit")
      .cookie(new Cookie(WebHelper.AuthCookie, auth, "/", "127.0.0.1", 1))
      .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
      .post(classOf[ClientResponse], formData)

    val doc2 = Jsoup.parse(cr2.getEntityInputStream, "UTF-8", resource.getURI.toString)

    assertEquals(HttpStatus.SC_OK, cr2.getStatus)
    assertEquals("Для изменения регистрации нужен ваш пароль", doc2.select(".error").text)
    assertEquals("/people/JB/edit", doc2.getElementById("editRegForm").attr("action"))
  }
}