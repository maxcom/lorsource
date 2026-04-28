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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy, TestContextManager}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

object EditRegisterControllerWebTest:
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

@ContextHierarchy(Array(new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class EditRegisterControllerWebTest extends FunSuite with WebHelper:
  import EditRegisterControllerWebTest._

  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  private var userDao: UserDao = scala.compiletime.uninitialized

  @Autowired
  private var userService: UserService = scala.compiletime.uninitialized

  private def rescueMaxcom(): Unit =
    val user = userDao.getUser(userDao.findUserId("maxcom"))
    userService.updateUser(user, MAXCOM_NAME, MAXCOM_URL, Some(MAXCOM_EMAIL), MAXCOM_TOWN, Some(MAXCOM_PASS), MAXCOM_INFO, "127.0.0.1")
    userDao.acceptNewEmail(user, MAXCOM_EMAIL)

  private def rescueJB(): Unit =
    val user = userDao.getUser(userDao.findUserId("JB"))
    userService.updateUser(user, JB_NAME, JB_URL, Some(JB_EMAIL), JB_TOWN, Some(JB_PASS), JB_INFO, "127.0.0.1")
    userDao.acceptNewEmail(user, JB_EMAIL)
    userDao.unblock(user)

  override def beforeEach(context: BeforeEach): Unit =
    rescueMaxcom()
    rescueJB()

  override def afterEach(context: AfterEach): Unit =
    rescueMaxcom()
    rescueJB()

  test("Вводим те же данные которые и были изначально. После изменений должен быть redirect в профиль"):
    val auth = doLogin("JB", JB_PASS)

    val cr = basicRequest
      .get(MainUrl.addPath("people", "JB", "edit"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(cr.code, StatusCode.Ok, "status code")

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"), "form action")

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(JB_NAME, name, "JB_NAME")
    assertEquals(JB_URL, url, "JB_URL")
    assertEquals(JB_EMAIL, email, "JB_EMAIL")
    assertEquals(JB_TOWN, town, "JB_TOWN")
    assertEquals(JB_INFO, info, "JB_INFO")

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
      .post(MainUrl.addPath("people", "JB", "edit"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(backend)

    assertEquals(StatusCode.Found, cr2.code, "redirect status")
    assertEquals(Some(Uri.unsafeParse("http://127.0.0.1:8080/people/JB/profile")),
      cr2.header(HeaderNames.Location).map(Uri.unsafeParse).map(MainUrl.resolve), "redirect location")

  test("ChangePassword"):
    val auth = doLogin("maxcom", MAXCOM_PASS)

    val cr = basicRequest
      .get(MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(StatusCode.Ok, cr.code, "status code")

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(MAXCOM_NAME, name, "MAXCOM_NAME")
    assertEquals(MAXCOM_URL, url, "MAXCOM_URL")
    assertEquals(MAXCOM_EMAIL, email, "MAXCOM_EMAIL")
    assertEquals(MAXCOM_TOWN, town, "MAXCOM_TOWN")
    assertEquals(MAXCOM_INFO, info, "MAXCOM_INFO")

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
      .post(MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(backend)

    assertEquals(StatusCode.Found, cr2.code, "redirect status")

    val newAuth = getAuthCookie(cr2)
    assert(newAuth != null, "newAuth should not be null")

    val location = Uri.unsafeParse(cr2.header(HeaderNames.Location).get)

    val cr3 = basicRequest
      .get(MainUrl.resolve(location))
      .cookie(AuthCookie, newAuth)
      .send(backend)

    assertEquals(StatusCode.Ok, cr3.code, "status code")

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
      .post(MainUrl.addPath("people", "maxcom", "edit"))
      .cookie(AuthCookie, newAuth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(backend)

    assertEquals(StatusCode.Found, cr4.code, "redirect status")

    val newAuth2 = getAuthCookie(cr4)

    val location2 = Uri.unsafeParse(cr4.header(HeaderNames.Location).get)

    val cr5 = basicRequest
      .get(MainUrl.resolve(location2))
      .cookie(AuthCookie, newAuth2)
      .send(backend)

    assertEquals(StatusCode.Ok, cr5.code, "status code")

  test("Для изменения регистрации нужен ваш пароль"):
    val auth = doLogin("JB", JB_PASS)

    val cr = basicRequest
      .get(MainUrl.addPath("people", "JB", "edit"))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(StatusCode.Ok, cr.code, "status code")

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"), "form action")

    val name = doc.getElementById("name").`val`
    val url = doc.getElementById("url").`val`
    val email = doc.getElementById("email").`val`
    val town = doc.getElementById("town").`val`
    val info = doc.getElementById("info").`val`

    assertEquals(JB_NAME, name, "JB_NAME")
    assertEquals(JB_URL, url, "JB_URL")
    assertEquals(JB_EMAIL, email, "JB_EMAIL")
    assertEquals(JB_TOWN, town, "JB_TOWN")
    assertEquals(JB_INFO, info, "JB_INFO")

    val cr2 = basicRequest
      .body(
        Map(
          ("name", name),
          ("url", url),
          ("email", email),
          ("town", town),
          ("info", info),
          ("csrf", "csrf")))
      .post(MainUrl.addPath("people", "JB", "edit"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .send(backend)

    val doc2 = Jsoup.parse(cr2.body.merge, cr2.request.uri.toString())

    assertEquals(StatusCode.Ok, cr2.code, "status code")

    assertEquals("Для изменения регистрации нужен ваш пароль", doc2.select(".error").text.trim, "error message")
    assertEquals("/people/JB/edit", doc2.getElementById("editRegForm").attr("action"), "form action")

  private def getAuthCookie(cr: Response[?]): String =
    cr.unsafeCookies.find(_.name == AuthCookie).map(_.value).orNull
