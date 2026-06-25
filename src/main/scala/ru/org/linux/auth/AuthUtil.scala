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

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.Errors
import org.springframework.web.context.request.{RequestAttributes, RequestContextHolder}
import ru.org.linux.user.{Profile, User, UserService}

import javax.annotation.Nullable
import scala.jdk.CollectionConverters.*

sealed trait AnySession:
  def authorized: Boolean
  def corrector: Boolean
  def moderator: Boolean
  def administrator: Boolean

  // None если аутентификации нет, Some(user) если есть
  def userOpt: Option[User]

  // user, если нет аутентификации то User для anonymous
  def user: User

  def opt: Option[AuthorizedSession]

  def profile: Profile
  def ipBlockInfo: IpBlockInfo

case class AuthorizedSession(
    user: User,
    corrector: Boolean,
    moderator: Boolean,
    administrator: Boolean,
    profile: Profile,
    ipBlockInfo: IpBlockInfo)
    extends AnySession:
  assert(!user.blocked, "blocked authorized user?!")
  
  override def userOpt: Some[User] = Some(user)
  override def opt: Option[AuthorizedSession] = Some(this)
  override def authorized: Boolean = true

case class NonAuthorizedSession(anonymous: User, ipBlockInfo: IpBlockInfo) extends AnySession:
  assert(!user.blocked, "blocked anonymous user?!")

  override def user: User = anonymous
  override def userOpt: None.type = None
  override def corrector: Boolean = false
  override def moderator: Boolean = false
  override def administrator: Boolean = false
  override def opt: Option[AuthorizedSession] = None
  override def authorized: Boolean = false
  override def profile: Profile = Profile.DEFAULT

object AuthUtil extends StrictLogging:
  def updateLastLogin(authentication: Authentication, userService: UserService): Unit =
    if authentication != null && authentication.isAuthenticated then
      val principal = authentication.getPrincipal

      principal match
        case userDetails: UserDetailsImpl =>
          val user = userDetails.getUser
          userService.updateLastLogin(user, force = true)
        case _ =>

  def isSessionAuthorized: Boolean =
    val authentication = SecurityContextHolder.getContext.getAuthentication

    authentication != null &&
    (authentication.isAuthenticated && !hasAuthority("ROLE_SYSTEM_ANONYMOUS") && hasAuthority("ROLE_ANONYMOUS"))

  def isModeratorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_MODERATOR")

  def isCorrectorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_CORRECTOR")

  private def isAdministratorSession: Boolean = isSessionAuthorized && hasAuthority("ROLE_ADMIN")

  private def hasAuthority(authName: String): Boolean =
    val authentication = SecurityContextHolder.getContext.getAuthentication

    if authentication == null then
      false
    else
      authentication.getAuthorities.asScala.exists(_.getAuthority == authName)

  /** Get current authorized users nick
    *
    * @return
    *   nick or null if not authorized
    */
  @Nullable
  def getNick: String =
    val currentUser = getCurrentUser

    if currentUser == null then
      null
    else
      currentUser.nick

  @Nullable
  def getCurrentUser: User =
    if !isSessionAuthorized then
      null
    else
      val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal

      principal match
        case details: UserDetailsImpl =>
          details.getUser
        case _ =>
          null

  def getProfile: Profile =
    if !isSessionAuthorized then
      Profile.DEFAULT
    else
      val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal

      principal match
        case details: UserDetailsImpl =>
          details.getProfile
        case _ =>
          Profile.DEFAULT

  def MaybeAuthorized[T](f: AnySession => T): T =
    if isSessionAuthorized then
      val session = AuthorizedSession(
        user = getCurrentUser,
        corrector = isCorrectorSession,
        moderator = isModeratorSession,
        administrator = isAdministratorSession,
        profile = getProfile,
        ipBlockInfo = mkIpBlockInfo
      )

      f(session)
    else
      f(mkNonAuthorizedSession)

  private def mkIpBlockInfo: IpBlockInfo =
    val ipBlockInfo = RequestContextHolder
      .currentRequestAttributes()
      .getAttribute("ipBlockInfo", RequestAttributes.SCOPE_REQUEST)
      .asInstanceOf[IpBlockInfo]

    if ipBlockInfo == null then
      throw new IllegalStateException("ipBlockInfo not set!?")

    ipBlockInfo

  private def mkNonAuthorizedSession: NonAuthorizedSession =
    val user = RequestContextHolder
      .currentRequestAttributes()
      .getAttribute("currentUser", RequestAttributes.SCOPE_REQUEST)
      .asInstanceOf[User]

    if user == null then
      throw new IllegalStateException("currentUser not set!?")
    if !user.anonymous then
      throw new IllegalStateException("expecting anonymous user for non-authorized session")

    NonAuthorizedSession(user, mkIpBlockInfo)

  def AuthorizedOnly[T](f: AuthorizedSession => T): T =
    if !isSessionAuthorized then
      throw new AccessViolationException("Not authorized")

    val session = AuthorizedSession(
      user = getCurrentUser,
      corrector = isCorrectorSession,
      moderator = isModeratorSession,
      administrator = isAdministratorSession,
      profile = getProfile,
      ipBlockInfo = mkIpBlockInfo
    )

    f(session)

  def ModeratorOnly[T](f: AuthorizedSession => T): T =
    if !isModeratorSession then
      throw new AccessViolationException("Not moderator")

    AuthorizedOnly(f)

  def CorrectorOrModerator[T](f: AuthorizedSession => T): T =
    if !(isCorrectorSession || isModeratorSession) then
      throw new AccessViolationException("Not corrector or moderator")

    AuthorizedOnly(f)

  def AdministratorOnly[T](f: AuthorizedSession => T): T =
    if !isAdministratorSession then
      throw new AccessViolationException("Not administrator")

    AuthorizedOnly(f)

  def postingUser(
      session: AnySession,
      formUser: Option[User],
      formPassword: Option[String],
      errors: Errors,
      passwordEncoder: PasswordEncoder,
      request: HttpServletRequest): AnySession =
    if errors.hasErrors then
      session
    else if session.authorized then
      session
    else
      formUser match
        case None =>
          session
        case Some(formUser) if formUser.anonymous =>
          session
        case Some(formUser) =>
          if formUser.blocked || !formUser.activated then
            errors.rejectValue("user", null, s"Пользователь \"${formUser.nick}\" заблокирован или не активирован")
            session
          else if !(formUser.anonymous && formPassword.get.isEmpty) &&
            !passwordEncoder.matches(formPassword.get, formUser.password)
          then
            logger.warn("Password of {} does not match; remote IP: {}; {}", formUser.nick, request.getRemoteAddr)

            errors.rejectValue("password", null, s"Пароль для пользователя \"${formUser.nick}\" задан неверно!")
            session
          else
            AuthorizedSession(
              formUser,
              corrector = false,
              moderator = false,
              administrator = false,
              profile = Profile.DEFAULT,
              ipBlockInfo = session.ipBlockInfo)
