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

package ru.org.linux.csrf

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.section.Section
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode, Uri}

import java.io.File
import scala.jdk.CollectionConverters.*

object CsrfRecoveryWebTest:
  private val TestGroup = 4068
  private val GalleryGroup = 19393 // раздел «Галерея» (imagepost: форма правки всегда multipart)
  private val GalleryTitle = "CSRF recovery gallery test"
  private val GalleryImage = "src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png"
  private val Marker = "csrf-recovery-marker-text"
  private val CookieToken = "actual-cookie-token"

class CsrfRecoveryWebTest extends FunSuite with WebHelper:
  import CsrfRecoveryWebTest.*

  private def formCsrfToken(html: String): String =
    val values = Jsoup.parse(html, MainUrl.toString).select("input[name=csrf]").asScala.map(_.attr("value")).distinct

    assertEquals(values.size, 1, "в форме должен быть ровно один csrf-токен")

    values.head

  private def assertFormWithText(body: String, formSelector: String, textareaSelector: String): Unit =
    val doc = Jsoup.parse(body, MainUrl.toString)

    assert(doc.select(formSelector).asScala.nonEmpty, "форма должна быть отрендерена заново")
    assert(doc.select(".error").asScala.nonEmpty, "должна быть показана ошибка CSRF")

    val textareas = doc.select(textareaSelector).asScala

    assert(textareas.nonEmpty, s"textarea $textareaSelector не найдена")
    assert(textareas.exists(_.`val`.contains(Marker)), "введённый текст должен быть сохранён в форме")

  authorized().test("add.jsp: при сбое CSRF форма с текстом и токеном актуальной куки"): auth =>
    val response = basicRequest
      .body(
        Map(
          "section" -> Section.Forum.toString,
          "group" -> TestGroup.toString,
          "title" -> "CSRF recovery test",
          "msg" -> Marker,
          "csrf" -> "wrong-csrf-value"))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
      .followRedirects(false)
      .post(MainUrl.addPath("add.jsp"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "форма должна быть возвращена вместо 403")

    assertFormWithText(response.body.merge, "#messageForm", "#form_msg")
    assertEquals(formCsrfToken(response.body.merge), CookieToken, "токен формы соответствует актуальной куке")

  authorized().test("add_comment.jsp: при сбое CSRF форма с текстом и токеном актуальной куки"): auth =>
    val topicId = createTopic(auth, TestGroup, "CSRF recovery comment test").fold(v => fail(v), identity)

    try
      val response = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> Marker, "csrf" -> "wrong-csrf-value"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment.jsp"))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, "форма должна быть возвращена вместо 403")

      assertFormWithText(response.body.merge, "#commentForm", "#msg")
      assertEquals(formCsrfToken(response.body.merge), CookieToken, "токен формы соответствует актуальной куке")
    finally
      deleteTopic(auth, topicId)

  authorized().test("add_comment.jsp: при сбое CSRF без куки выдаётся новая кука и совпадающий токен"): auth =>
    val topicId = createTopic(auth, TestGroup, "CSRF recovery new cookie test").fold(v => fail(v), identity)

    try
      val response = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> Marker, "csrf" -> "stale-token-value"))
        .cookie(AuthCookie, auth)
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment.jsp"))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, "форма должна быть возвращена вместо 403")

      assertFormWithText(response.body.merge, "#commentForm", "#msg")

      val csrfCookie = response.unsafeCookies.find(_.name == CSRFProtectionService.CSRF_COOKIE)

      assert(csrfCookie.isDefined, "должна быть выдана новая CSRF-кука")
      assert(csrfCookie.get.maxAge.exists(_ > 0), "новая CSRF-кука должна быть персистентной")
      assertEquals(formCsrfToken(response.body.merge), csrfCookie.get.value, "токен формы совпадает с новой кукой")
    finally
      deleteTopic(auth, topicId)

  authorized().test("add_comment.jsp: повторный сабмит с токеном из формы создаёт комментарий"): auth =>
    val topicId = createTopic(auth, TestGroup, "CSRF recovery retry test").fold(v => fail(v), identity)

    var commentId = 0

    try
      val failed = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> Marker, "csrf" -> "wrong-csrf-value"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment.jsp"))
        .send(backend)

      assertEquals(failed.code, StatusCode.Ok, "первая попытка возвращает форму")

      val retry = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> Marker, "csrf" -> formCsrfToken(failed.body.merge)))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment.jsp"))
        .send(backend)

      assertEquals(retry.code, StatusCode.SeeOther, "повторный сабмит должен создать комментарий")

      commentId = retry
        .header(HeaderNames.Location)
        .map(Uri.parse)
        .flatMap(_.toOption)
        .flatMap(_.params.get("cid"))
        .map(_.toInt)
        .getOrElse(0)
    finally
      if commentId > 0 then
        deleteComment(auth, commentId)

      deleteTopic(auth, topicId)

  authorized().test("edit_comment: при сбое CSRF форма с текстом и токеном актуальной куки"): auth =>
    val topicId = createTopic(auth, TestGroup, "CSRF recovery edit test").fold(v => fail(v), identity)

    var commentId = 0

    try
      val posted = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> "original text", "csrf" -> "csrf"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment.jsp"))
        .send(backend)

      assertEquals(posted.code, StatusCode.SeeOther, "исходный комментарий должен быть создан")

      commentId = posted
        .header(HeaderNames.Location)
        .map(Uri.parse)
        .flatMap(_.toOption)
        .flatMap(_.params.get("cid"))
        .map(_.toInt)
        .getOrElse(0)

      val response = basicRequest
        .body(
          Map(
            "topic" -> topicId.toString,
            "original" -> commentId.toString,
            "msg" -> Marker,
            "csrf" -> "wrong-csrf-value"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("edit_comment"))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, "форма должна быть возвращена вместо 403")

      assertFormWithText(response.body.merge, "#commentForm", "#msg")
      assertEquals(formCsrfToken(response.body.merge), CookieToken, "токен формы соответствует актуальной куке")
    finally
      if commentId > 0 then
        deleteComment(auth, commentId)

      deleteTopic(auth, topicId)

  /** Галерейный топик: раздел с imagepost, т.е. форма и create, и edit — multipart (CsrfFilter не видит параметры
    * таких запросов, CSRF проверяется вручную в контроллере).
    */
  private def createGalleryTopic(auth: String): Int =
    val response = basicRequest
      .multipartBody(
        multipart("section", Section.Gallery.toString),
        multipart("group", GalleryGroup.toString),
        multipart("title", GalleryTitle),
        multipart("msg", "исходный текст галерейного топика"),
        multipart("csrf", "csrf"),
        multipartFile("images", new File(GalleryImage)))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("add.jsp"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, s"не удалось создать галерейный топик: ${response.body.merge}")

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString)

    assert(doc.select(".error").asScala.isEmpty, s"ошибки создания галерейного топика: ${doc.select(".error").text()}")

    doc.select("a[href]").asScala
      .find(_.text().contains("Перейти к сообщению"))
      .map(_.attr("href").split("/").last.toInt)
      .getOrElse(fail("ссылка на созданный топик не найдена"))
  end createGalleryTopic

  authorized().test("edit.jsp: multipart-форма галереи при сбое CSRF возвращает форму, повторный сабмит правит"): auth =>
    val topicId = createGalleryTopic(auth)

    try
      val failed = basicRequest
        .multipartBody(
          multipart("msgid", topicId.toString),
          multipart("title", GalleryTitle),
          multipart("msg", Marker),
          multipart("csrf", "wrong-csrf-value"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("edit.jsp"))
        .send(backend)

      assertEquals(failed.code, StatusCode.Ok, "multipart-форма правки должна быть возвращена вместо 403")

      assertFormWithText(failed.body.merge, "#messageForm", "#form_msg")
      assertEquals(formCsrfToken(failed.body.merge), CookieToken, "токен формы соответствует актуальной куке")

      val retry = basicRequest
        .multipartBody(
          multipart("msgid", topicId.toString),
          multipart("title", GalleryTitle),
          multipart("msg", Marker),
          multipart("csrf", CookieToken))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("edit.jsp"))
        .send(backend)

      assertEquals(
        retry.code,
        StatusCode.Found,
        s"повторный multipart-сабмит с валидным CSRF должен править топик: ${retry.body.merge}")
    finally
      deleteTopic(auth, topicId)

  authorized().test("add_comment_ajax: при сбое CSRF возвращается JSON с ошибкой"): auth =>
    val topicId = createTopic(auth, TestGroup, "CSRF recovery ajax test").fold(v => fail(v), identity)

    try
      val response = basicRequest
        .body(Map("topic" -> topicId.toString, "msg" -> Marker, "csrf" -> "wrong-csrf-value"))
        .cookie(AuthCookie, auth)
        .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
        .followRedirects(false)
        .post(MainUrl.addPath("add_comment_ajax"))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, "JSON с ошибкой вместо 403")

      val body = response.body.merge

      assert(body.contains("сбой добавления"), s"ответ должен содержать ошибку CSRF, получено: $body")
    finally
      deleteTopic(auth, topicId)

  authorized("maxcom", "passwd").test("tags/delete: валидный CSRF из urlencoded формы проходит фильтр"): auth =>
    val response = basicRequest
      .body(Map("oldTagName" -> "csrf-test-nonexistent", "csrf" -> CookieToken))
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE -> CookieToken)
      .followRedirects(false)
      .post(MainUrl.addPath("tags", "delete"))
      .send(backend)

    assertEquals(response.code, StatusCode.Ok, "валидный CSRF не должен отбиваться CsrfFilter")

    assert(response.body.merge.contains("Тега с таким именем не существует"),
      s"форма должна быть перерисована с ошибкой, получено: ${response.body.merge}")

  test("tags/delete: POST без CSRF-токена отклоняется с 403"):
    val response = basicRequest
      .body(Map("oldTagName" -> "csrf-test-nonexistent"))
      .post(MainUrl.addPath("tags", "delete"))
      .send(backend)

    assertEquals(response.code, StatusCode.Forbidden, "запрос без токена должен быть отклонён CsrfFilter")

  test("защищённый CsrfFilter POST без токена: 403 со стилизованной страницей и сообщением"):
    val response = basicRequest.post(MainUrl.addPath("logout")).send(backend)

    assertEquals(response.code, StatusCode.Forbidden, "запрос должен быть отклонён с 403")

    val body = response.body.merge

    assert(body.contains("CSRF"), s"страница должна содержать сообщение об ошибке CSRF, получено: $body")
    assert(body.contains("combined.css"), "страница должна рендериться с контекстом запроса (стили темы)")
