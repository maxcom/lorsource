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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy, TestContextManager}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

import java.io.File
import javax.sql.DataSource

@ContextHierarchy(Array(new ContextConfiguration(value = Array("classpath:database-admin.xml")),
  new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class UserpicControllerWebTest extends FunSuite with WebHelper:
  new TestContextManager(this.getClass).prepareTestInstance(this)
  @Autowired
  private var userDao: UserDao = scala.compiletime.uninitialized

  private var jdbcTemplate: JdbcTemplate = scala.compiletime.uninitialized

  @Autowired
  def setDatasource(ds: DataSource): Unit =
    jdbcTemplate = new JdbcTemplate(ds)

  private def rescueJB(): Unit =
    val user = userDao.getUser(userDao.findUserId("JB"))
    jdbcTemplate.update("DELETE FROM user_log WHERE userid=?", user.id)
    userDao.unblock(user)

  private def addPhoto(filename: String, auth: String) =
    val file = new File(filename)
    basicRequest
      .multipartBody(
        multipart("csrf", "csrf"),
        multipartFile("file", file))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .post(MainUrl.addPath("addphoto.jsp"))
      .followRedirects(false)
      .send(backend)

  override def beforeEach(context: BeforeEach): Unit =
    rescueJB()

  override def afterEach(context: AfterEach): Unit =
    rescueJB()

  test("Страница загрузки фото"):
    val auth = doLogin("JB", "passwd")
    val response = basicRequest
      .cookie(AuthCookie, auth)
      .get(MainUrl.addPath("addphoto.jsp"))
      .send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("Тест неправильной картинки: XML файл"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/database.xml", auth)
    assertEquals(cr.code, StatusCode.BadRequest, "status code")
    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())
    assertEquals("Ошибка! Invalid image", doc.select(".error").text, "error message")

  test("Тест неправильной картинки: слишком большой файл"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/img/pcard.jpg", auth)
    assertEquals(cr.code, StatusCode.BadRequest, "status code")
    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())
    assertEquals("Ошибка! Сбой загрузки изображения: слишком большой файл",
      doc.select(".error").text, "error message")

  test("Тест неправильной картинки: недопустимые размеры"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/img/twitter.png", auth)
    assertEquals(cr.code, StatusCode.BadRequest, "status code")
    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())
    assertEquals("Ошибка! Сбой загрузки изображения: недопустимые размеры фотографии",
      doc.select(".error").text, "error message")

  test("Тест неправильной картинки: анимация GIF"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/images/animated.gif", auth)
    assertEquals(cr.code, StatusCode.BadRequest, "status code")
    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())
    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима",
      doc.select(".error").text, "error message")

  test("Тест валидной картинки"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/tango/img/android.png", auth)
    assertEquals(StatusCode.Found, cr.code, "status code")
    val redirect = cr.header(HeaderNames.Location).map(Uri.unsafeParse)
    val url = Uri.unsafeParse("http://127.0.0.1:8080/people/JB/profile")
    assertEquals(Some(url.path), redirect.map(_.path), "redirect path")
    assert(redirect.get.params.get("nocache").isDefined, "nocache param exists")
    assert(redirect.get.params.get("nocache").exists(_.nonEmpty), "nocache has value")

  test("Тест APNG анимации"):
    val auth = doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png", auth)
    assertEquals(cr.code, StatusCode.BadRequest, "status code")
    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())
    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима", doc.select(".error").text, "error message")
