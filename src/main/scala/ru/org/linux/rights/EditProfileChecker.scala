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
import ru.org.linux.auth.AuthorizedSession
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.rights.EditProfileChecker.MaxUserpicScoreLoss
import ru.org.linux.user.{UserLogAction, UserLogDao}

import java.time.Duration

@Service
class EditProfileChecker(userLogDao: UserLogDao, deleteInfoDao: DeleteInfoDao):
  def checkEditProfileInfo(using session: AuthorizedSession): Permission =
    import session.user

    Unrestricted
      .restrict(FrozenUserChecker.checkChain)
      .restrict(
        userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.ResetInfo),
        "текст профиля был сброшен модератором менее 24 часов назад")
      .restrict(
        userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.ResetUrl),
        "url был сброшен модератором менее 24 часов назад")
      .restrict(
        userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.ResetTown),
        "поле города было сброшено модератором менее 24 часов назад")
      .seal

  def checkLoadUserpic(using session: AuthorizedSession): Permission =
    val user = session.user

    Unrestricted
      .restrict(FrozenUserChecker.checkChain)
      .restrict(user.getScore < 45, "низкий score < 45")
      .restrict(
        userLogDao.getUserpicSetCount(user, Duration.ofHours(1)) >= 3,
        "слишком частая смена userpic, 3 за последний час")
      .restrict(
        userLogDao.hasRecentModerationEvent(user, Duration.ofDays(30), UserLogAction.ResetUserpic),
        "userpic был сброшен модератором менее 30 дней назад")
      .restrict(
        deleteInfoDao.getRecentScoreLoss(user) >= MaxUserpicScoreLoss,
        "превышен лимит нарушений правил за последние 3 дня")
      .seal

object EditProfileChecker:
  private val MaxUserpicScoreLoss = 20
