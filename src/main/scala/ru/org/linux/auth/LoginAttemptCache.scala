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

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

@Component
class LoginAttemptCache:
  private val ipCache = LoginAttemptCache.newCache()
  private val userCache = LoginAttemptCache.newCache()

  def requireCaptchaForIp(ip: String): Boolean = isLive(ipCache.get(ip))

  def requireCaptchaForUser(username: String): Boolean = isLive(userCache.get(username.toLowerCase))

  def recordFailedAttempt(ip: String, username: String): Unit =
    val deadline = 30.minutes.fromNow
    ipCache.put(ip, deadline)
    userCache.put(username.toLowerCase, deadline)

  /** Атомарная регистрация попытки проверки пароля ДО её выполнения (check-then-act TOCTOU: параллельные запросы не
    * должны одновременно видеть «captcha не требуется»). Если живая метка уже есть (недавняя неудача или чужая
    * незавершённая попытка) — captcha требуется; иначе ставится предварительная метка, которую cancel() снимает при
    * успешной/несостоявшейся попытке, а recordFailedAttempt замещает при неудаче. Итог: не более одной попытки без
    * captcha на IP/ник в 30-минутном окне даже при гонке.
    */
  def beginAttempt(ip: String, username: String): LoginAttemptCache.AttemptGate =
    val userKey = username.toLowerCase
    val ipReservation = reserve(ipCache, ip)
    val userReservation = reserve(userCache, userKey)

    LoginAttemptCache.AttemptGate(
      captchaRequired = ipReservation.isEmpty || userReservation.isEmpty,
      onCancel =
        () =>
          ipReservation.foreach(deadline => cancelReservation(ipCache, ip, deadline))
          userReservation.foreach(deadline => cancelReservation(userCache, userKey, deadline))
    )

  private def isLive(deadline: Deadline): Boolean = deadline != null && deadline.hasTimeLeft()

  private def reserve(map: ConcurrentMap[String, Deadline], key: String): Option[Deadline] =
    val fresh = 30.minutes.fromNow
    val stored = map.compute(
      key,
      (_, current) =>
        if isLive(current) then
          current
        else
          fresh)
    if stored eq fresh then
      Some(fresh)
    else
      None

  private def cancelReservation(map: ConcurrentMap[String, Deadline], key: String, deadline: Deadline): Unit =
    map.computeIfPresent(
      key,
      (_, current) =>
        if current eq deadline then
          null
        else
          current)

object LoginAttemptCache:
  private val MaxEntries = 1_000_000

  private def newCache(): ConcurrentMap[String, Deadline] =
    Caffeine
      .newBuilder()
      .maximumSize(MaxEntries)
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build[String, Deadline]()
      .asMap()

  final class AttemptGate(val captchaRequired: Boolean, private val onCancel: () => Unit):
    /** Снимает только собственные предварительные метки; запись о неудаче (recordFailedAttempt), заместившая метку,
      * остаётся. Поэтому безопасно вызывать из finally безусловно: после recordFailedAttempt cancel() — no-op.
      */
    def cancel(): Unit = onCancel()

  /** Gate для путей, где кэш не консультируется (captcha обязательна по другим правилам). */
  val NoAttempt: AttemptGate = AttemptGate(captchaRequired = false, () => ())
