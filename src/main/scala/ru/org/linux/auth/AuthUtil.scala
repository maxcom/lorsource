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

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.Errors
import ru.org.linux.user.{Profile, User, UserDao, UserService}

import javax.annotation.Nullable
import scala.jdk.CollectionConverters.*

sealed trait AnySession {
  def authorized: Boolean
  def corrector: Boolean
  def moderator: Boolean
  def administrator: Boolean

  // TODO minimize usages
  // TODO тут можно возвращать userService.getAnonymous для анонимуса, но надо убрать все места, где
  // TODO это используется как признак наличия аутентификации
  def userOpt: Option[User]

  def opt: Option[AuthorizedSession]

  def profile: Profile
}

case class AuthorizedSession(user: User, corrector: Boolean, moderator: Boolean,
                             administrator: Boolean, profile: Profile) extends AnySession {
  override def userOpt: Some[User] = Some(user)
  override def opt: Option[AuthorizedSession] = Some(this)
  override def authorized: Boolean = true
}

case object NonAuthorizedSession extends AnySession {
  override def userOpt: None.type = None
  override def corrector: Boolean = false
  override def moderator: Boolean = false
  override def administrator: Boolean = false
  override def opt: Option[AuthorizedSession] = None
  override def authorized: Boolean = false
  override def profile: Profile = Profile.DEFAULT
}

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

  private def isAdministratorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_ADMIN")

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
      currentUser.nick
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
      Profile.DEFAULT
    } else {
      val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal

      principal match {
        case details: UserDetailsImpl =>
          details.getProfile
        case _ =>
          Profile.DEFAULT
      }
    }
  }

  def MaybeAuthorized[T](f: AnySession => T): T = {
    if (isSessionAuthorized) {
      val currentUser = AuthorizedSession(
        user = getCurrentUser,
        corrector = isCorrectorSession,
        moderator = isModeratorSession,
        administrator = isAdministratorSession,
        profile = getProfile)

      f(currentUser)
    } else {
      f(NonAuthorizedSession)
    }
  }

  def AuthorizedOnly[T](f: AuthorizedSession => T): T = {
    if (!isSessionAuthorized) {
      throw new AccessViolationException("Not authorized")
    }

    val currentUser = AuthorizedSession(
      user = getCurrentUser,
      corrector = isCorrectorSession,
      moderator = isModeratorSession,
      administrator = isAdministratorSession,
      profile = getProfile)

    f(currentUser)
  }

  def ModeratorOnly[T](f: AuthorizedSession => T): T = {
    if (!isModeratorSession) {
      throw new AccessViolationException("Not moderator")
    }

    AuthorizedOnly(f)
  }

  def CorrectorOrModerator[T](f: AuthorizedSession => T): T = {
    if (!(isCorrectorSession || isModeratorSession)) {
      throw new AccessViolationException("Not corrector or moderator")
    }

    AuthorizedOnly(f)
  }

  def AdministratorOnly[T](f: AuthorizedSession => T): T = {
    if (!isAdministratorSession) {
      throw new AccessViolationException("Not administrator")
    }

    AuthorizedOnly(f)
  }

  def postingUser(session: AnySession, formUser: Option[User], formPassword: Option[String], errors: Errors): AnySession = {
    if (session.authorized) {
      session
    } else {
      formUser match {
        case None =>
          NonAuthorizedSession
        case Some(formUser) if formUser.anonymous =>
          NonAuthorizedSession
        case Some(formUser) =>
          if (formUser.blocked || !formUser.activated) {
            errors.rejectValue("user", null, s"Пользователь \"${formUser.nick}\" заблокирован или не активирован")
            NonAuthorizedSession
          } else if (!(formUser.anonymous && formPassword.get.isEmpty) && !UserService.matchPassword(formUser, formPassword.get)) {
            errors.rejectValue("password", null, s"Пароль для пользователя \"${formUser.nick}\" задан неверно!")
            NonAuthorizedSession
          } else {
            AuthorizedSession(formUser, corrector = false, moderator = false, administrator = false, profile = Profile.DEFAULT)
          }
      }
    }
  }
}