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

package ru.org.linux.auth

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.springframework.validation.BeanPropertyBindingResult
import ru.org.linux.rights.{Permitted, Permission, Restricted, SlowModeChecker}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.User

import java.time.Duration
import java.util.Properties

class FloodProtectorTest extends FunSuite:

  private def makeUser(score: Int, anonymous: Boolean = false): User =
    User(
      nick = "user",
      id = 1,
      canmod = false,
      candel = false,
      anonymous = anonymous,
      corrector = false,
      blocked = false,
      password = "password",
      score = score,
      maxScore = score,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

  private def makeProtector(permission: Permission = Permitted): FloodProtector =
    val properties = new Properties
    properties.setProperty("MainUrl", "https://linux.org.ru/")

    val slowModeChecker = mock(classOf[SlowModeChecker])
    when(slowModeChecker.check(any[User])).thenReturn(permission)

    new FloodProtector(new SiteConfig(properties), slowModeChecker)

  test("аноним: первый запрос предпросмотра проходит, повторный в окне блокируется порогом 5 секунд") {
    val floodProtector = makeProtector()

    assertEquals(floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.1", null), None)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.1", null),
      Some(Duration.ofSeconds(5)))
  }

  test("лимит предпросмотра независим для разных IP") {
    val floodProtector = makeProtector()

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.1", null)

    assertEquals(floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.2", null), None)
  }

  test("анонимный пользователь получает порог 5 секунд") {
    val floodProtector = makeProtector()
    val user = makeUser(score = 100, anonymous = true)

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.3", user)

    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.3", user),
      Some(Duration.ofSeconds(5)))
  }

  test("предпросмотр: score>=50 получает порог 2 секунды, score<50 — 5 секунд") {
    val floodProtector = makeProtector()
    val trusted = makeUser(score = 50)
    val regular = makeUser(score = 49)

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.4", trusted)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.4", trusted),
      Some(Duration.ofSeconds(2)))

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.5", regular)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.5", regular),
      Some(Duration.ofSeconds(5)))
  }

  test("slow-mode не влияет на предпросмотр: порог определяется только score") {
    val floodProtector = makeProtector(permission = Restricted("тест"))
    val highScore = makeUser(score = 200)
    val lowScore = makeUser(score = 40)

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.6", highScore)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.6", highScore),
      Some(Duration.ofSeconds(2)))

    floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.10", lowScore)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.PreviewMarkup, "10.0.0.10", lowScore),
      Some(Duration.ofSeconds(5)))
  }

  test("граница доверенного порога постинга не изменилась: AddComment score 99 — 30 секунд, score 100 — 3 секунды") {
    val floodProtector = makeProtector()
    val almostTrusted = makeUser(score = 99)
    val trusted = makeUser(score = 100)

    floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.7", almostTrusted)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.7", almostTrusted),
      Some(Duration.ofSeconds(30)))

    floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.8", trusted)
    assertEquals(
      floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.8", trusted),
      Some(Duration.ofSeconds(3)))
  }

  test("вариант с Errors пишет прежнее сообщение об ошибке") {
    val floodProtector = makeProtector()
    val errors = new BeanPropertyBindingResult(new Object, "form")

    floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.9", null, errors)
    assert(!errors.hasErrors)

    floodProtector.checkRateLimit(FloodProtector.AddComment, "10.0.0.9", null, errors)
    assert(errors.hasErrors)
    assertEquals(
      errors.getAllErrors.get(0).getDefaultMessage,
      "Следующее сообщение может быть записано не менее чем через 30 секунд после предыдущего")
  }
