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

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LoginAttemptCacheTest extends FunSuite:
  test("first attempt is captcha-free, an overlapping attempt requires captcha") {
    val cache = LoginAttemptCache()

    val first = cache.beginAttempt("192.0.2.1", "user")
    assertEquals(first.captchaRequired, false)

    // до фикса вторая параллельная попытка читала флаг до записи первой и шла без captcha
    val overlapping = cache.beginAttempt("192.0.2.1", "user")
    assertEquals(overlapping.captchaRequired, true)

    first.cancel()
    val afterCancel = cache.beginAttempt("192.0.2.1", "user")
    assertEquals(afterCancel.captchaRequired, false)
  }

  test("in-flight attempt is visible to requireCaptcha checks") {
    val cache = LoginAttemptCache()

    cache.beginAttempt("192.0.2.1", "user")

    assert(cache.requireCaptchaForIp("192.0.2.1"))
    assert(cache.requireCaptchaForUser("user"))
  }

  test("overlapping attempt sharing only the ip requires captcha") {
    val cache = LoginAttemptCache()

    cache.beginAttempt("192.0.2.1", "user1")

    assert(cache.beginAttempt("192.0.2.1", "user2").captchaRequired)
  }

  test("overlapping attempt sharing only the nick requires captcha (case-insensitive)") {
    val cache = LoginAttemptCache()

    cache.beginAttempt("192.0.2.1", "User")

    assert(cache.beginAttempt("192.0.2.2", "USER").captchaRequired)
  }

  test("cancel of a superseded gate keeps the failed-attempt flag") {
    val cache = LoginAttemptCache()

    val attempt = cache.beginAttempt("192.0.2.1", "user")
    cache.recordFailedAttempt("192.0.2.1", "user")

    assert(cache.requireCaptchaForIp("192.0.2.1"))
    assert(cache.requireCaptchaForUser("user"))

    attempt.cancel()
    assert(cache.requireCaptchaForIp("192.0.2.1"))
    assert(cache.requireCaptchaForUser("user"))
  }

  test("cancel does not touch flags of other keys") {
    val cache = LoginAttemptCache()

    cache.recordFailedAttempt("192.0.2.1", "user1")
    cache.beginAttempt("192.0.2.2", "user2").cancel()

    assert(cache.requireCaptchaForIp("192.0.2.1"))
    assert(cache.requireCaptchaForUser("user1"))
    assert(!cache.requireCaptchaForIp("192.0.2.2"))
    assert(!cache.requireCaptchaForUser("user2"))
  }

  test("at most one captcha-free attempt among concurrent attempts") {
    val cache = LoginAttemptCache()
    val threads = 16
    val barrier = new CyclicBarrier(threads)
    val freeAttempts = new AtomicInteger

    val started =
      for _ <- 1 to threads
      yield
        val t =
          new Thread(() =>
            barrier.await(10, TimeUnit.SECONDS)
            if !cache.beginAttempt("192.0.2.1", "victim").captchaRequired then
              freeAttempts.incrementAndGet())
        t.start()
        t

    started.foreach(_.join(10_000))
    assert(started.forall(!_.isAlive), "a thread hung: did not finish within 10 seconds")

    // 0 тоже возможен и корректен (fail-closed): выигравший ip-резервацию и выигравший user-резервацию —
    // разные потоки, тогда captcha требуется всем. Недопустимо только >1 бесплатной попытки.
    assert(freeAttempts.get <= 1)

    // обе резервации зафиксированы
    assert(cache.requireCaptchaForIp("192.0.2.1"))
    assert(cache.requireCaptchaForUser("victim"))
  }
