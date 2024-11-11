/*
 * Copyright 1998-2024 Linux.org.ru
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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.user.User

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

object FloodProtector {
  case class Action(thresholdLowScore: Duration, threshold: Duration, thresholdTrusted: Duration)

  val AddComment: Action = Action(
    thresholdLowScore = Duration.ofMinutes(5),
    threshold = Duration.ofSeconds(30),
    thresholdTrusted = Duration.ofSeconds(3))

  val AddTopic: Action = Action(
    thresholdLowScore = Duration.ofMinutes(10),
    threshold = Duration.ofMinutes(10),
    thresholdTrusted = Duration.ofSeconds(30))
}

@Component
class FloodProtector(deleteInfoDao: DeleteInfoDao) {
  final private val performedActions: Cache[String, Instant] =
    CacheBuilder.newBuilder.expireAfterWrite(30, TimeUnit.MINUTES).build

  private def check(action: FloodProtector.Action, ip: String, threshold: Duration): Boolean = {
    val key = action.toString + ':' + ip

    val date = Option(performedActions.getIfPresent(key))

    if (date.exists(_.plus(threshold).isAfter(Instant.now))) {
      false
    } else {
      performedActions.put(key, Instant.now)
      true
    }
  }

  def checkRateLimit(action: FloodProtector.Action, ip: String, user: User, errors: Errors): Unit = {
    val threshold: Duration = if (user.isAnonymous) {
      action.threshold
    } else if (user.getScore < 35 || deleteInfoDao.getRecentScoreLoss(user) >= 30) {
      action.thresholdLowScore
    } else if (user.getScore >= 100) {
      action.thresholdTrusted
    } else {
      action.threshold
    }

    if (!check(action, ip, threshold)) {
      errors.reject(null,
        s"Следующее сообщение может быть записано не менее чем через ${threshold.toSeconds} секунд после предыдущего")
    }
  }
}