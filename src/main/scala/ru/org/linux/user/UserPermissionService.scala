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
import org.springframework.validation.Errors
import ru.org.linux.auth.{AuthorizedSession, IPBlockDao, IPBlockInfo}
import ru.org.linux.markup.MarkupType
import ru.org.linux.markup.MarkupType.{Html, Lorcode, LorcodeUlb, Markdown}
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.user.UserPermissionService.*

import java.time.Duration
import javax.annotation.Nullable
import scala.jdk.CollectionConverters.SetHasAsJava

object UserPermissionService {
  val MaxTotalInvites = 15
  val MaxUserInvites = 1
  val MaxInviteScoreLoss = 10
  val InviteScore = 200
  val MaxUnactivatedPerIp = 2
  val MaxUserpicScoreLoss = 20

  def allowedFormats(user: User): Set[MarkupType] = {
    if (user==null) { // anonymous
      Set(Lorcode, Markdown)
    } else if (user.isAdministrator) {
      Set(Lorcode, LorcodeUlb, Markdown, Html)
    } else {
      Set(Lorcode, LorcodeUlb, Markdown)
    }
  }

  def allowedFormatsJava(user: User): java.util.Set[MarkupType] = allowedFormats(user).asJava

  def checkBlockIP(block: IPBlockInfo, errors: Errors, @Nullable user: User): Unit = {
    if (block.isBlocked && (user == null || user.isAnonymousScore || !block.isAllowRegisteredPosting)) {
      errors.reject(null, "Постинг заблокирован: " + block.reason)
    }
  }
}

@Service
class UserPermissionService(userLogDao: UserLogDao, userInvitesDao: UserInvitesDao, deleteInfoDao: DeleteInfoDao,
                            ipBlockDao: IPBlockDao, userDao: UserDao) {
  def canResetPassword(user: User): Boolean = {
    !userLogDao.hasRecentSelfEvent(user, Duration.ofDays(7), UserLogAction.SENT_PASSWORD_RESET)
  }

  def canInvite(implicit session: AuthorizedSession): Boolean = session.moderator || {
    lazy val (totalInvites, userInvites) = userInvitesDao.countValidInvites(session.user)
    lazy val userScoreLoss = deleteInfoDao.getRecentScoreLoss(session.user)

    !session.user.isFrozen && session.user.getScore > InviteScore &&
      totalInvites < MaxTotalInvites &&
      userInvites < MaxUserInvites &&
      userScoreLoss < MaxInviteScoreLoss
  }

  def canRegister(remoteAddr: String): Boolean = !ipBlockDao.getBlockInfo(remoteAddr).isBlocked &&
    userDao.countUnactivated(remoteAddr) < MaxUnactivatedPerIp

  def canLoadUserpic(implicit session: AuthorizedSession): Boolean = {
    def userpicSetCount = userLogDao.getUserpicSetCount(session.user, Duration.ofHours(1))

    def wasReset = userLogDao.hasRecentModerationEvent(session.user, Duration.ofDays(30), UserLogAction.RESET_USERPIC)

    def userScoreLoss = deleteInfoDao.getRecentScoreLoss(session.user)

    session.user.getScore >= 45 &&
      !session.user.isFrozen &&
      (userpicSetCount < 3) &&
      !wasReset &&
      (userScoreLoss < MaxUserpicScoreLoss)
  }

  def canEditProfileInfo(implicit session: AuthorizedSession): Boolean = {
    import session.user

    !user.isFrozen &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_INFO) &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_URL) &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_TOWN)
  }

  def canResetPasswordByCode(user: User): Boolean =
    !user.isBlocked && user.isActivated && !user.isAnonymous && !user.isAdministrator
}