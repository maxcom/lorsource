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
package ru.org.linux.user

import org.springframework.stereotype.Service
import ru.org.linux.auth.IpBlockInfo
import ru.org.linux.markup.MarkupType
import ru.org.linux.markup.MarkupType.{Html, Lorcode, LorcodeUlb, Markdown}
import ru.org.linux.user.UserPermissionService.*

import java.time.Duration

object UserPermissionService {
  private val MaxUnactivatedPerIp = 2
  val DeprecatedFeaturesScore = 500

  def allowedFormats(user: User): Set[MarkupType] = {
    if (user==null) { // anonymous
      Set(Lorcode, Markdown)
    } else if (user.isAdministrator) {
      Set(Lorcode, LorcodeUlb, Markdown, Html)
    } else {
      Set(Lorcode, LorcodeUlb, Markdown)
    }
  }

  val ResetCodeMaxAge: Duration = Duration.ofDays(1)
}

@Service
class UserPermissionService(userLogDao: UserLogDao, userDao: UserDao) {
  def canResetPassword(user: User): Boolean = {
    !userLogDao.hasRecentSelfEvent(user, UserPermissionService.ResetCodeMaxAge, UserLogAction.SentPasswordReset)
  }

  def canRegister(ipBlockInfo: IpBlockInfo): Boolean = !ipBlockInfo.isBlocked &&
    userDao.countUnactivated(ipBlockInfo.ip) < MaxUnactivatedPerIp
  
  def canResetPasswordByCode(user: User): Boolean =
    !user.blocked && user.activated && !user.anonymous && !user.isAdministrator
}