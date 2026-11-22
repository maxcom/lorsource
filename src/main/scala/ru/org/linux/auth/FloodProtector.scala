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

import com.google.common.cache.{Cache, CacheBuilder}
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import ru.org.linux.rights.SlowModeChecker
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.User

import java.time.{Duration, Instant}
import org.jspecify.annotations.Nullable

object FloodProtector:
  case class Action(
      thresholdLowScore: Duration,
      threshold: Duration,
      thresholdTrusted: Duration,
      trustedScore: Int = 100,
      checkSlowMode: Boolean = true)

  val AddComment: Action =
    Action(
      thresholdLowScore = Duration.ofMinutes(5),
      threshold = Duration.ofSeconds(30),
      thresholdTrusted = Duration.ofSeconds(3))

  val AddTopic: Action =
    Action(
      thresholdLowScore = Duration.ofMinutes(10),
      threshold = Duration.ofMinutes(10),
      thresholdTrusted = Duration.ofSeconds(30))

  /** Предпросмотр разметки: пороги зависят только от score, slow-mode не учитывается — отдельного лимита для него нет,
    * а проверка slow-mode требует запроса к БД на каждый предпросмотр.
    */
  val PreviewMarkup: Action =
    Action(
      thresholdLowScore = Duration.ofSeconds(5),
      threshold = Duration.ofSeconds(5),
      thresholdTrusted = Duration.ofSeconds(2),
      trustedScore = 50,
      checkSlowMode = false)

@Component
class FloodProtector(siteConfig: SiteConfig, slowModeChecker: SlowModeChecker):
  final private val performedActions: Cache[String, Instant] =
    CacheBuilder.newBuilder.expireAfterWrite(Duration.ofMinutes(30)).build

  // for tests
  private val enabled: Boolean = siteConfig.getMainURI.getHost != "127.0.0.1"

  private def check(action: FloodProtector.Action, ip: String, threshold: Duration): Boolean =
    val key = action.toString + ':' + ip
    val now = Instant.now

    val prev = performedActions.asMap().putIfAbsent(key, now)
    if prev == null then
      true
    else if prev.plus(threshold).isAfter(now) then
      false
    else
      performedActions.asMap().replace(key, prev, now)

  private def thresholdFor(
      action: FloodProtector.Action,
      @Nullable
      user: User): Duration =
    if user == null || user.anonymous then
      action.threshold
    else if action.checkSlowMode && slowModeChecker.check(user).restricted then
      action.thresholdLowScore
    else if user.getScore >= action.trustedScore then
      action.thresholdTrusted
    else
      action.threshold

  /** Проверка лимита частоты без привязки к WEB-форме.
    *
    * @return
    *   None если запрос разрешён, иначе Some(актуальный порог)
    */
  def checkRateLimit(
      action: FloodProtector.Action,
      ip: String,
      @Nullable
      user: User): Option[Duration] =
    if !enabled then
      None
    else
      val threshold = thresholdFor(action, user)

      if check(action, ip, threshold) then
        None
      else
        Some(threshold)

  def checkRateLimit(
      action: FloodProtector.Action,
      ip: String,
      @Nullable
      user: User,
      errors: Errors): Unit =
    checkRateLimit(action, ip, user).foreach(threshold =>
      errors.reject(
        null,
        s"Следующее сообщение может быть записано не менее чем через ${threshold.toSeconds} секунд после предыдущего"))
