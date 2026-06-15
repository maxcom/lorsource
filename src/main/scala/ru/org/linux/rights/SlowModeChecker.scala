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

package ru.org.linux.rights

import org.springframework.stereotype.Service
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.user.User

import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class SlowModeChecker(deleteInfoDao: DeleteInfoDao):
  def check(user: User): Permission =
    Unrestricted
      .permit(user.anonymous || user.isFrozen || user.blocked)
      .restrict(user.getScore < 35, "большое число нарушений правил, score < 35")
      .restrict(
        Option(user.frozenUntil).map(_.toInstant).exists(_.isAfter(Instant.now.minus(3, ChronoUnit.DAYS))),
        "заморозка закончилась менее трех дней назад")
      .restrict(deleteInfoDao.getRecentScoreLoss(user) >= 30, "превышен лимит нарушений правил за последние 3 дня")
      .seal
