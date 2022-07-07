/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import ru.org.linux.user.{Profile, User, UserDao}

import javax.annotation.Nullable
import scala.jdk.CollectionConverters._

case class CurrentUser(user: User, corrector: Boolean, moderator: Boolean)

object AuthUtil {
  def updateLastLogin(authentication: Authentication, userDao: UserDao): Unit = {
    if (authentication != null && authentication.isAuthenticated) {
      val principal = authentication.getPrincipal

      principal match {
        case userDetails: UserDetailsImpl =>
          val user = userDetails.getUser
          userDao.updateLastlogin(user, true)
        case _ =>
      }
    }
  }

  def isSessionAuthorized: Boolean = {
    val authentication = SecurityContextHolder.getContext.getAuthentication

    authentication != null &&
      (authentication.isAuthenticated &&
        !hasAuthority("ROLE_SYSTEM_ANONYMOUS") &&
        hasAuthority("ROLE_ANONYMOUS"))
  }

  def isModeratorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_MODERATOR")

  def isCorrectorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_CORRECTOR")

  private def hasAuthority(authName: String): Boolean = {
    val authentication = SecurityContextHolder.getContext.getAuthentication

    if (authentication == null) {
      false
    } else {
      authentication.getAuthorities.asScala.exists(_.getAuthority == authName)
    }
  }

  /**
   * Get current authorized users nick
   *
   * @return nick or null if not authorized
   */
  @Nullable
  def getNick: String = {
    val currentUser = getCurrentUser

    if (currentUser == null) {
      null
    } else {
      currentUser.getNick
    }
  }

  @Nullable
  def getCurrentUser: User = {
    if (!isSessionAuthorized) {
      null
    } else {
      val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal

      principal match {
        case details: UserDetailsImpl =>
          details.getUser
        case _ =>
          null
      }
    }
  }

  def getProfile: Profile = {
    if (!isSessionAuthorized) {
      Profile.createDefault
    } else {
      val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal

      principal match {
        case details: UserDetailsImpl =>
          details.getProfile
        case _ =>
          Profile.createDefault
      }
    }
  }

  def AuthorizedOnly[T](f: CurrentUser => T): T = {
    if (!isSessionAuthorized) {
      throw new AccessViolationException("Not authorized")
    }

    val currentUser = CurrentUser(getCurrentUser, isCorrectorSession, isModeratorSession)

    f(currentUser)
  }

  def ModeratorOnly[T](f: CurrentUser => T): T = {
    if (!isModeratorSession) {
      throw new AccessViolationException("Not moderator")
    }

    AuthorizedOnly(f)
  }

  def CorrectorOrModerator[T](f: CurrentUser => T): T = {
    if (!(isCorrectorSession || isModeratorSession)) {
      throw new AccessViolationException("Not corrector or moderator")
    }

    AuthorizedOnly(f)
  }
}