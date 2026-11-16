/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.search

import munit.FunSuite
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.User

import java.util.Properties

class SearchRateLimiterTest extends FunSuite:
  private def makeLimiter(mainUrl: String): SearchRateLimiter =
    val props = new Properties()
    props.setProperty("MainUrl", mainUrl)
    new SearchRateLimiter(new SiteConfig(props))

  private def makeUser(id: Int): User =
    User(
      nick = s"user$id",
      id = id,
      canmod = false,
      candel = false,
      anonymous = false,
      corrector = false,
      blocked = false,
      password = "password",
      score = 100,
      maxScore = 100,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

  test("anonymous requests are limited per IP"):
    val limiter = makeLimiter("http://www.linux.org.ru/")

    for _ <- 1 to SearchRateLimiter.AnonymousLimit.toInt do
      limiter.check("10.0.0.1", null)

    intercept[SearchRateLimitException]:
      limiter.check("10.0.0.1", null)

  test("anonymous budget is independent per IP"):
    val limiter = makeLimiter("http://www.linux.org.ru/")

    for _ <- 1 to SearchRateLimiter.AnonymousLimit.toInt do
      limiter.check("10.0.0.1", null)

    // другой IP не затронут исчерпанным бюджетом первого
    limiter.check("10.0.0.2", null)

  test("authorized user has separate higher budget keyed by user id"):
    val limiter = makeLimiter("http://www.linux.org.ru/")
    val user = makeUser(42)

    for _ <- 1 to SearchRateLimiter.AuthorizedLimit.toInt do
      limiter.check("10.0.0.1", user)

    intercept[SearchRateLimitException]:
      limiter.check("10.0.0.1", user)

    // другой пользователь и анонимный бюджет с того же IP не затронуты
    limiter.check("10.0.0.1", makeUser(43))
    limiter.check("10.0.0.1", null)

  test("limiter is disabled for local dev/test MainUrl"):
    val limiter = makeLimiter("http://127.0.0.1:8080/")

    for _ <- 1 to (SearchRateLimiter.AnonymousLimit * 10).toInt do
      limiter.check("10.0.0.1", null)

end SearchRateLimiterTest
