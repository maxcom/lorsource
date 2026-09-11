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
import sttp.client4.*
import sttp.model.StatusCode

object AddCommentPreviewWebTest:
  private val TestGroup = 4068
  private val TestTitle = "Comment Preview Web Test"

class AddCommentPreviewWebTest extends FunSuite with WebHelper:
  import AddCommentPreviewWebTest.*

  // Пользователь тестовой БД (пароль у всех тестовых пользователей "passwd")
  private val OracleNick = "edo"

  /** Анонимный предпросмотр комментария с указанием nick/password не должен быть неограниченным оракулом подбора
    * пароля: неудачная попытка учитывается в LoginAttemptCache, и далее (как и в /login_process) требуется captcha — до
    * проверки пароля.
    */
  authorized().test("preview posting is not a password brute-force oracle"): auth =>
    val topicId = createTopic(auth, TestGroup, TestTitle).fold(v => fail(v), identity)

    try
      def previewPost(password: String): String =
        val response = basicRequest
          .body(
            Map(
              "topic" -> topicId.toString,
              "msg" -> "preview oracle test",
              "nick" -> OracleNick,
              "password" -> password,
              "preview" -> "Предпросмотр"))
          .post(MainUrl.addPath("add_comment.jsp"))
          .send(backend)

        assertEquals(response.code, StatusCode.Ok, "preview should render form")

        Jsoup.parse(response.body.merge, MainUrl.toString()).select(".error").text()

      // Первая попытка свободна (как в /login_process): ошибка неверного пароля, captcha не требуется
      val first = previewPost("definitely-wrong-password")
      assert(first.contains("задан неверно"), first)

      // Неудача учтена: повторная попытка без h-captcha-response отклоняется до проверки пароля
      val second = previewPost("another-wrong-password")
      assert(second.contains("Код проверки защиты от роботов не указан"), second)
      assert(!second.contains("задан неверно"), second)
    finally
      deleteTopic(auth, topicId)
