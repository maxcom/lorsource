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

package ru.org.linux.comment

import munit.FunSuite
import org.jsoup.Jsoup
import ru.org.linux.test.WebHelper
import sttp.client4.basicRequest
import sttp.model.StatusCode

object ExpiredTopicCommentFormWebTest:
  /** Топик из demo.db (форум, 2007 год): deleted=f, sticky=f => устаревший (expired). */
  private val ExpiredTopicId = 1937504

  /** Неудалённый комментарий в этом топике. */
  private val ExpiredTopicCommentId = 1938061

  private val ExpiredMessage = "нельзя добавлять в устаревшие темы"

class ExpiredTopicCommentFormWebTest extends FunSuite with WebHelper:
  import ExpiredTopicCommentFormWebTest.*

  test("GET comment-message.jsp по устаревшей теме показывает страницу ошибки"):
    val response = basicRequest
      .get(MainUrl.addPath("comment-message.jsp").addParam("topic", ExpiredTopicId.toString))
      .send(backend)

    assertEquals(response.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())

    assert(doc.select("h1").text().contains(ExpiredMessage), s"строка ошибки не найдена: ${doc.select("h1").text()}")

  authorized().test("GET edit_comment по устаревшей теме показывает страницу ошибки"): auth =>
    val response = basicRequest
      .get(
        MainUrl
          .addPath("edit_comment")
          .addParam("topic", ExpiredTopicId.toString)
          .addParam("original", ExpiredTopicCommentId.toString))
      .cookie(AuthCookie, auth)
      .send(backend)

    assertEquals(response.code, StatusCode.BadRequest)

    val doc = Jsoup.parse(response.body.merge, MainUrl.toString())

    assert(doc.select("h1").text().contains(ExpiredMessage), s"строка ошибки не найдена: ${doc.select("h1").text()}")
