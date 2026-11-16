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

import com.github.benmanes.caffeine.cache.Caffeine
import org.jspecify.annotations.Nullable
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.User

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Ограничение частоты запросов к поиску (защита от исчерпания ресурсов через /search.jsp — каждый запрос выполняет
  * тяжёлый запрос к OpenSearch с агрегациями и подсветкой).
  *
  * Анонимные запросы считаются по IP клиента, авторизованные — по пользователю. Счётчик — фиксированное окно
  * длительностью [[SearchRateLimiter.WindowMinutes]] от первого запроса: инкремент AtomicLong не является записью в
  * кэш, поэтому запись истекает целиком (фиксированное окно, а не скользящее).
  */
@Component
class SearchRateLimiter(siteConfig: SiteConfig):
  private val counters: ConcurrentMap[String, AtomicLong] =
    Caffeine
      .newBuilder()
      .maximumSize(SearchRateLimiter.MaxEntries)
      .expireAfterWrite(SearchRateLimiter.WindowMinutes, TimeUnit.MINUTES)
      .build[String, AtomicLong]()
      .asMap()

  // for tests
  private val enabled: Boolean = siteConfig.getMainURI.getHost != "127.0.0.1"

  /** Бросает [[SearchRateLimitException]], если исчерпан лимит запросов в текущем окне. */
  def check(
      ip: String,
      @Nullable
      user: User): Unit =
    if enabled then
      val (key, limit) =
        if user != null && !user.anonymous then
          (s"user:${user.id}", SearchRateLimiter.AuthorizedLimit)
        else
          (s"ip:$ip", SearchRateLimiter.AnonymousLimit)

      val count = counters.computeIfAbsent(key, _ => new AtomicLong()).incrementAndGet()

      if count > limit then
        throw SearchRateLimitException()

object SearchRateLimiter:
  /** Лимит запросов в минуту с одного IP для анонимных пользователей. */
  val AnonymousLimit: Long = 20

  /** Лимит запросов в минуту для авторизованного пользователя. */
  val AuthorizedLimit: Long = 60

  val WindowMinutes: Long = 1

  /** Значение заголовка Retry-After (секунды): полное окно — худший случай ожидания сброса бюджета. */
  val RetryAfterSeconds: Long = WindowMinutes * 60

  private val MaxEntries = 1_000_000

class SearchRateLimitException extends RuntimeException
