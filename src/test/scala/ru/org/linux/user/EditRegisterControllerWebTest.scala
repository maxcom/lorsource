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
package ru.org.linux.user

import org.jsoup.Jsoup
import org.junit.Assert.{assertEquals, assertNotNull}
import org.junit.runner.RunWith
import org.junit.{After, Before, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import ru.org.linux.test.WebHelper.AuthCookie
import ru.org.linux.user.EditRegisterControllerWebTest.*
import sttp.client3.*
import sttp.model.{HeaderNames, StatusCode, Uri}

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
  @Autowired
  private var userDao: UserDao = _

  @Autowired
  private var userService: UserService = _

  private def rescueMaxcom(): Unit = {
    val user = userDao.getUser(userDao.findUserId("maxcom"))
    userService.updateUser(user, MAXCOM_NAME, MAXCOM_URL, MAXCOM_EMAIL, MAXCOM_TOWN, MAXCOM_PASS, MAXCOM_INFO,  "127.0.0.1")
    userDao.acceptNewEmail(user, MAXCOM_EMAIL)
  }

  private def rescueJB(): Unit = {
    val user = userDao.getUser(userDao.findUserId("JB"))
    userService.updateUser(user, JB_NAME, JB_URL, JB_EMAIL, JB_TOWN, JB_PASS, JB_INFO, "127.0.0.1")
    userDao.acceptNewEmail(user, JB_EMAIL)
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
    val auth = WebHelper.doLogin("JB", JB_PASS)

    val cr = basicRequest
      .get(WebHelper.MainUrl.addPath("people", "JB", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .send(WebHelper.backend)

    assertEquals(cr.code, StatusCode.Ok)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"))

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(JB_NAME, name)
    assertEquals(JB_URL, url)
    assertEquals(JB_EMAIL, email)
    assertEquals(JB_TOWN, town)
    assertEquals(JB_INFO, info)

    val cr2 = basicRequest
      .body(
        Map(
          ("name", name),
          ("url", url),
          ("email", email),
          ("town", town),
          ("info", info),
          ("csrf", "csrf"),
          ("oldpass", JB_PASS)))
      .post(WebHelper.MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Found, cr2.code)
    assertEquals(Some("http://127.0.0.1:8080/people/JB/profile"), cr2.header(HeaderNames.Location))
  }

  @Test
  def testChangePassword(): Unit = {
    val auth = WebHelper.doLogin("maxcom", MAXCOM_PASS)

    val cr = basicRequest
      .get(WebHelper.MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Ok, cr.code)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(MAXCOM_NAME, name)
    assertEquals(MAXCOM_URL, url)
    assertEquals(MAXCOM_EMAIL, email)
    assertEquals(MAXCOM_TOWN, town)
    assertEquals(MAXCOM_INFO, info)

    val cr2 = basicRequest
      .body(
        Map(
          ("name", name),
          ("url", url),
          ("email", email),
          ("town", town),
          ("info", info),
          ("csrf", "csrf"),
          ("oldpass", "passwd"),
          ("password", "passwd2"),
          ("password2", "passwd2")))
      .post(WebHelper.MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Found, cr2.code)

    val newAuth = getAuthCookie(cr2)
    assertNotNull(newAuth)

    val cr3 = basicRequest
      .get(Uri.unsafeParse(cr2.header(HeaderNames.Location).get))
      .cookie(WebHelper.AuthCookie, newAuth)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Ok, cr3.code)

    val cr4 = basicRequest
      .body(
        Map(
          ("name", name),
          ("url", url),
          ("email", email),
          ("town", town),
          ("info", info),
          ("csrf", "csrf"),
          ("oldpass", "passwd2"),
          ("password", "passwd"),
          ("password2", "passwd")))
      .post(WebHelper.MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(WebHelper.AuthCookie, newAuth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Found, cr4.code)

    val newAuth2 = getAuthCookie(cr4)

    val cr5 = basicRequest
      .get(Uri.unsafeParse(cr4.header(HeaderNames.Location).get))
      .cookie(WebHelper.AuthCookie, newAuth2)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Ok, cr5.code)
  }

  @Test
  def testChange(): Unit = {
    val auth = WebHelper.doLogin("JB", JB_PASS)

    val cr = basicRequest
      .get(WebHelper.MainUrl.addPath("people", "JB", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .send(WebHelper.backend)

    assertEquals(StatusCode.Ok, cr.code)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"))

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(JB_NAME, name)
    assertEquals(JB_URL, url)
    assertEquals(JB_EMAIL, email)
    assertEquals(JB_TOWN, town)
    assertEquals(JB_INFO, info)

    val cr2 = basicRequest
      .body(
        Map(
          ("name", name),
          ("url", url),
          ("email", email),
          ("town", town),
          ("info", info),
          ("csrf", "csrf")))
      .post(WebHelper.MainUrl.addPath("people", "JB", "edit"))
      .cookie(WebHelper.AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(WebHelper.backend)

    val doc2 = Jsoup.parse(cr2.body.merge, cr2.request.uri.toString())

    assertEquals(StatusCode.Ok, cr2.code)

    assertEquals("Для изменения регистрации нужен ваш пароль", doc2.select(".error").text)
    assertEquals("/people/JB/edit", doc2.getElementById("editRegForm").attr("action"))
  }

  private def getAuthCookie(cr: Response[?]): String =
    cr.unsafeCookies.find(_.name == AuthCookie).map(_.value).orNull
}