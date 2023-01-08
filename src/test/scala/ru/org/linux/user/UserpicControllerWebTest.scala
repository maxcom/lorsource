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

import org.jsoup.Jsoup
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.runner.RunWith
import org.junit.{After, Before, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.test.WebHelper
import sttp.client3.*
import sttp.model.{HeaderNames, StatusCode}

import java.io.File
import javax.sql.DataSource

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(new ContextConfiguration(value = Array("classpath:database-admin.xml")),
  new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class UserpicControllerWebTest {
  @Autowired
  private var userDao: UserDao = _

  private var jdbcTemplate: JdbcTemplate = _

  @Autowired
  def setDatasource(ds: DataSource): Unit = {
    jdbcTemplate = new JdbcTemplate(ds)
  }

  private def rescueJB(): Unit = {
    val user = userDao.getUser(userDao.findUserId("JB"))

    jdbcTemplate.update("DELETE FROM user_log WHERE userid=?", user.getId)

    userDao.unblock(user, user)
  }

  private def addPhoto(filename: String, auth: String) = {
    val file = new File(filename)

    basicRequest
      .multipartBody(
        multipart("csrf", "csrf"),
        multipartFile("file", file))
      .cookie(WebHelper.AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .post(WebHelper.MainUrl.addPath("addphoto.jsp"))
      .followRedirects(false)
      .send(WebHelper.backend)
  }

  @After
  @Before
  def clean(): Unit = {
    rescueJB()
  }

  @Test
  def testPage(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")

    val response = basicRequest
      .cookie(WebHelper.AuthCookie, auth)
      .get(WebHelper.MainUrl.addPath("addphoto.jsp"))
      .send(WebHelper.backend)

    assertEquals(response.code, StatusCode.Ok)
  }

  /**
   * Тест неправильной картинки
   */
  @Test
  def testInvalidImage(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/database.xml", auth)

    assertEquals(cr.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("Ошибка! Invalid image", doc.select(".error").text) // сообщение об ошипке
  }

  /**
   * Тест неправильной картинки
   */
  @Test
  def testInvalid2Image(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/img/pcard.jpg", auth)

    assertEquals(cr.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("Ошибка! Сбой загрузки изображения: слишком большой файл",
      doc.select(".error").text) // сообщение об ошибке
  }

  /**
   * Тест неправильной картинки
   */
  @Test
  def testInvalid3Image(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/img/twitter.png", auth)

    assertEquals(cr.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("Ошибка! Сбой загрузки изображения: недопустимые размеры фотографии",
      doc.select(".error").text) // сообщение об ошибке
  }

  /**
   * Тест неправильной картинки
   */
  @Test
  def testInvalid4Image(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/images/animated.gif", auth)

    assertEquals(cr.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима",
      doc.select(".error").text) // сообщение об ошибке
  }

  /**
   * Тест неправильной картинки
   */
  @Test
  def testValidImage(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/main/webapp/tango/img/android.png", auth)

    assertEquals(cr.code, StatusCode.Found)

    val redirect = cr.header(HeaderNames.Location).getOrElse("")

    val url = "http://127.0.0.1:8080/people/JB/profile"
    val `val` = "?nocache="

    assertEquals(url, redirect.substring(0, url.length))
    assertEquals(`val`, redirect.substring(url.length, url.length + `val`.length))
    assertTrue("у nocache должен быть аргумент", redirect.length > url.length + `val`.length)
  }

  /**
   * Тест с apng анимацией и поней
   * image source via http://tamalesyatole.deviantart.com/art/I-want-to-be-a-Hero-APNG-Animated-332248278
   */
  @Test
  def testAPNGImage(): Unit = {
    val auth = WebHelper.doLogin("JB", "passwd")
    val cr = addPhoto("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png", auth)

    assertEquals(cr.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(cr.body.merge, cr.request.uri.toString())

    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима", doc.select(".error").text) // сообщение об ошибке
  }
}