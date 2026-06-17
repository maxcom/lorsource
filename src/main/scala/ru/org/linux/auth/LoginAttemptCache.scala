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

import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Component
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.*
import java.time.Duration

@Component
class LoginAttemptCache:
  private val ipCache =
    CacheBuilder.newBuilder().maximumSize(1_000_000).expireAfterWrite(Duration.ofMinutes(30)).build[String, Deadline]()

  private val userCache =
    CacheBuilder.newBuilder().maximumSize(1_000_000).expireAfterWrite(Duration.ofMinutes(30)).build[String, Deadline]()

  def requireCaptchaForIp(ip: String): Boolean =
    val deadline = ipCache.getIfPresent(ip)
    deadline != null && deadline.hasTimeLeft()

  def requireCaptchaForUser(username: String): Boolean =
    val deadline = userCache.getIfPresent(username.toLowerCase)
    deadline != null && deadline.hasTimeLeft()

  def recordFailedAttempt(ip: String, username: String): Unit =
    val deadline = 30.minutes.fromNow
    ipCache.put(ip, deadline)
    userCache.put(username.toLowerCase, deadline)
