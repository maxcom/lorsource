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

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.common.util.concurrent.UncheckedExecutionException
import com.typesafe.scalalogging.StrictLogging
import org.jasypt.exceptions.EncryptionOperationNotPossibleException
import org.jasypt.util.password.BasicPasswordEncryptor
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.markup.MarkupType
import ru.org.linux.site.DefaultProfile
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.UserAgentDao
import ru.org.linux.user.UserService.*
import ru.org.linux.util.image.{ImageInfo, ImageParam, ImageUtil}
import ru.org.linux.util.{BadImageException, StringUtil}

import java.io.{File, FileNotFoundException, IOException}
import java.sql.Timestamp
import java.time.{Duration, Instant}
import java.util
import java.util.Optional
import javax.annotation.Nullable
import javax.mail.internet.InternetAddress
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.{Failure, Success, Try}

object UserService {
  val MaxFileSize: Int = 100 * 1024
  val MinImageSize = 50
  val MaxImageSize = 300

  val DisabledUserpic: Userpic = Userpic("/img/p.gif", 1, 1)

  val AnonymousUserId = 2

  private val NameCacheSize = 10000

  val CorrectorScore = 200

  private val UserPasswordEncryptor = new BasicPasswordEncryptor

  def matchPassword(user: User, password: String): Boolean = {
    try {
      UserPasswordEncryptor.checkPassword(password, user.password)
    } catch {
      case _: EncryptionOperationNotPossibleException =>
        false
    }
  }
}

@Service
class UserService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao,
                  userInvitesDao: UserInvitesDao, userLogDao: UserLogDao, userAgentDao: UserAgentDao,
                  profileDao: ProfileDao, val transactionManager: PlatformTransactionManager)
    extends StrictLogging with TransactionManagement {
  private val nameToIdCache =
    CacheBuilder.newBuilder().maximumSize(UserService.NameCacheSize).build[String, Integer](
      new CacheLoader[String, Integer] {
        override def load(nick: String): Integer = userDao.findUserId(nick)
      }
    )

  @throws(classOf[UserErrorException])
  @throws(classOf[IOException])
  @throws(classOf[BadImageException])
  def checkUserPic(file: File): ImageParam = {
    if (!file.isFile) {
      throw new UserErrorException("Сбой загрузки изображения: не файл")
    }

    val param = ImageUtil.imageCheck(file)

    if (param.isAnimated) {
      throw new UserErrorException("Сбой загрузки изображения: анимация не допустима")
    }

    if (param.getSize > UserService.MaxFileSize) {
      throw new UserErrorException("Сбой загрузки изображения: слишком большой файл")
    }

    if (param.getHeight < UserService.MinImageSize || param.getHeight > UserService.MaxImageSize) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии")
    }

    if (param.getWidth < UserService.MinImageSize || param.getWidth > UserService.MaxImageSize) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии")
    }

    param
  }

  def isIgnoring(userId: Int, ignoredUserId: Int): Boolean = ignoreListDao.get(userId).contains(ignoredUserId)

  private def gravatar(email: String, avatarStyle: String, size: Int): String = {
    val nonExist: String = if ("empty" == avatarStyle) {
      "blank"
    } else {
      avatarStyle
    }

    val emailHash = StringUtil.md5hash(email.toLowerCase)

    s"https://secure.gravatar.com/avatar/$emailHash?s=$size&r=g&d=$nonExist&f=y"
  }

  def getUserpic(user: User, avatarStyle: String, misteryMan: Boolean): Userpic = {
    val avatarMode = if (misteryMan && ("empty" == avatarStyle)) {
      "mm"
    } else {
      avatarStyle
    }

    val userpic = if (user.anonymous && misteryMan) {
      Some(Userpic(gravatar("anonymous@linux.org.ru", avatarMode, 150), 150, 150))
    } else if (user.photo != null && user.photo.nonEmpty) {
      Try {
        val info = new ImageInfo(s"${siteConfig.getUploadPath}/photos/${user.photo}").scale(150)

        Userpic(s"/photos/${user.photo}", info.getWidth, info.getHeight)
      } match {
        case Failure(e: FileNotFoundException) =>
          logger.warn(s"Userpic not found for ${user.nick}: ${e.getMessage}")
          None
        case Failure(e) =>
          logger.warn(s"Bad userpic for ${user.nick}", e)
          None
        case Success(u) =>
          Some(u)
      }
    } else {
      None
    }

    userpic.getOrElse {
      if (avatarMode == "empty" || !user.hasEmail) {
        UserService.DisabledUserpic
      } else {
        Userpic(gravatar(user.email, avatarMode, 150), 150, 150)
      }
    }
  }

  def getResetCode(nick: String, email: String, tm: Timestamp): String = {
    val base = siteConfig.getSecret
    StringUtil.md5hash(s"$base:$nick:$email:${tm.getTime.toString}:reset")
  }

  def updateResetDate(forUser: User, byUser: User, email: String, now: Timestamp): Unit = transactional() { _ =>
    userDao.updateResetDate(forUser, now)
    userLogDao.logSentPasswordReset(forUser, byUser, email)
  }

  def getUsersCached(ids: Iterable[Int]): Seq[User] = ids.map(x => getUserCached(x)).toSeq

  def getUsersCachedMap(userIds: Iterable[Int]): Map[Int, User] =
    getUsersCached(userIds.toSet).view.map(u => u.id -> u).toMap

  def getUsersCachedJava(ids: java.lang.Iterable[Integer]): util.List[User] =
    getUsersCached(ids.asScala.map(i => i)).asJava

  def getNewUsers: util.List[User] = getUsersCachedJava(userDao.getNewUserIds)

  def getNewUsersByUAIp(ip: Option[String], @Nullable userAgent: Integer): util.List[(User, Timestamp, Timestamp)] =
    userDao.getNewUsersByIP(ip.orNull, userAgent).asScala.map { case (id, regdate, lastlogin) =>
      (getUserCached(id), regdate, lastlogin)
    }.asJava

  private def prepareUserWithActivity(users: util.List[(Integer, Optional[Instant])],
                                      activityDays: Int) = {
    val recentSeenDate = Instant.now().minus(Duration.ofDays(activityDays))

    users.asScala.map { case (userId, lastlogin) =>
      val user = getUserCached(userId)
      (user, lastlogin.toScala.exists(_.isAfter(recentSeenDate)))
    }
  }

  def getFrozenUsers: collection.Seq[(User, Boolean)] = prepareUserWithActivity(userDao.getFrozenUserIds, activityDays = 1)

  def getUnFrozenUsers: collection.Seq[(User, Boolean)] = prepareUserWithActivity(userDao.getUnFrozenUserIds, activityDays = 1)

  def getRecentlyBlocked: collection.Seq[User] = getUsersCachedJava(userLogDao.getRecentlyHasEvent(UserLogAction.BLOCK_USER)).asScala

  def getRecentlyUnBlocked: collection.Seq[User] = getUsersCachedJava(userLogDao.getRecentlyHasEvent(UserLogAction.UNBLOCK_USER)).asScala

  def getModerators: collection.Seq[(User, Boolean)] = prepareUserWithActivity(userDao.getModerators, activityDays = 30)

  def getCorrectors: collection.Seq[(User, Boolean)] = prepareUserWithActivity(userDao.getCorrectors, activityDays = 30)

  def getRecentUserpics: Seq[(User, Userpic)] = {
    val userIds = userLogDao.getRecentlyHasEvent(UserLogAction.SET_USERPIC).asScala.map(_.toInt).toSeq.distinct

    getUsersCached(userIds).map { user =>
      user -> getUserpic(user, "empty", misteryMan = false)
    }.filterNot(_._2 == DisabledUserpic)
  }

  private def findUserIdCached(nick: String) = try nameToIdCache.get(nick) catch {
    case ex: UncheckedExecutionException => throw ex.getCause
  }

  def getUserCached(nick: String): User = userDao.getUserCached(findUserIdCached(nick))

  def findUserCached(nick: String): Option[User] = try Some(getUserCached(findUserIdCached(nick))) catch {
    case _: UserNotFoundException =>
      None
  }

  def getUserCached(id: Int): User = userDao.getUserCached(id)

  def getUser(nick: String): User = userDao.getUser(findUserIdCached(nick))

  def getAnonymous: User = try {
    getUserCached(UserService.AnonymousUserId)
  } catch {
    case e: UserNotFoundException =>
      throw new RuntimeException("Anonymous not found!?", e)
  }

  def createUser(nick: String, password: String, mail: InternetAddress, ip: String, invite: Option[String],
                 userAgent: Option[String], language: Option[String]): Int = {
    val result = transactional() { _ =>
      val newUserId = userDao.createUser("", nick, password, null, mail, null)

      invite.foreach { token =>
        val marked = userInvitesDao.markUsed(token, newUserId)

        if (!marked) throw new AccessViolationException("Инвайт уже использован")
      }

      val inviteOwner = invite.flatMap(userInvitesDao.ownerOfInvite)

      val userAgentId = userAgent.map(userAgentDao.createOrGetId).getOrElse(0)

      userLogDao.logRegister(newUserId, ip, inviteOwner.map(Integer.valueOf).toJava, userAgentId, language.toJava)

      newUserId
    }

    // обновляет кеш на случай, если пользователь был заново зарегистрирован с тем же ником после неудачи с активацией
    nameToIdCache.put(nick, result)

    result
  }

  def getAllInvitedUsers(user: User): util.List[User] =
    userInvitesDao.getAllInvitedUsers(user).map(getUserCached).asJava

  def wasRecentlyBlocker(user: User): Boolean =
    userLogDao.hasRecentModerationEvent(user, Duration.ofDays(14), UserLogAction.BLOCK_USER)

  def removeUserInfo(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfo(user)

    if ((userInfo != null) && userInfo.trim.nonEmpty) {
      userDao.updateUserInfo(user.id, null)
      userDao.changeScore(user.id, -10)
      userLogDao.logResetInfo(user, moderator, userInfo, -10)
    }
  }

  def removeTown(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfoClass(user)

    if (userInfo.town != null && userInfo.town.trim.nonEmpty) {
      userDao.removeTown(user)
      userLogDao.logResetTown(user, moderator, userInfo.town, -10)
      userDao.changeScore(user.id, -10)
    }
  }

  def removeUrl(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfoClass(user)

    if (userInfo.url != null || userInfo.url.trim.nonEmpty) {
      userDao.removeUrl(user)
      userLogDao.logResetUrl(user, moderator, userInfo.url, 0)
      userDao.changeScore(user.id, 0)
    }
  }

  def updateUser(user: User, name: String, url: String, newEmail: Option[String], town: String,
                 password: Option[String], info: String, ip: String): Unit = transactional() { _ =>
    val changed = mutable.Map[String, String]()

    if (userDao.updateName(user, name)) changed += "name" -> name

    if (userDao.updateUrl(user, url)) changed += "url" -> url

    if (userDao.updateTown(user, town)) changed += "town" -> town

    if (userDao.updateUserInfo(user.id, info)) changed += "info" -> info

    updateEmailPasswd(user, newEmail, password, ip)

    if (changed.nonEmpty) {
      userLogDao.logSetUserInfo(user, changed.asJava)
    }
  }

  def updateEmailPasswd(user: User, newEmail: Option[String], password: Option[String],
                        ip: String): Unit = transactional() { _ =>
    password.foreach { password =>
      userDao.setPassword(user, password)
      userLogDao.logSetPassword(user, ip)
    }

    newEmail.foreach(userDao.setNewEmail(user, _))
  }

  def isBlockable(user: User, by: User): Boolean =
    !user.anonymous && by.isModerator && (!user.isModerator || by.isAdministrator)

  def isFreezable(user: User, by: User): Boolean = by.isModerator && !user.isModerator

  def getUsersWithAgent(ip: Option[String], userAgent: Option[Int], limit: Int): util.List[UserAndAgent] =
    userDao.getUsersWithAgent(ip.orNull, userAgent.map(Integer.valueOf).orNull, limit)

  def deregister(user: User, remoteAddr: String): Unit = transactional() { _ =>
    userDao.resetUserpic(user, user)

    updateUser(user, "", "", None, "", None, "", remoteAddr)

    userDao.block(user, user, "самостоятельная блокировка аккаунта")
  }

  def getProfile(user: User): Profile = {
    val profile = profileDao.readProfile(user.id)

    val mode = profile.formatMode

    val modeFixed = if (UserPermissionService.allowedFormats(user).contains(mode)) {
      mode
    } else {
      MarkupType.ofFormId(DefaultProfile.getDefaultProfile.get("format.mode").asInstanceOf[String])
    }

    val boxletsFixed = profile.boxes.filter(DefaultProfile.isBox)

    profile.copy(formatMode = modeFixed, boxes = boxletsFixed)
  }

  def freezeUser(user: User, moderator: User, reason: String, until: Timestamp): Unit = transactional() { _ =>
    userDao.freezeUser(user, moderator, reason, until)
    userLogDao.logFreezeUser(user, moderator, reason, until.toInstant)
  }

  def setPhoto(user: User, photo: String): Unit = transactional() { _ =>
    userDao.setPhoto(user, photo)
  }

  def setStyle(user: User, style: String): Unit = transactional() { _ =>
    userDao.setStyle(user, style)
  }

  def changeScore(userId: Int, delta: Int): Unit = transactional() { _ =>
    userDao.changeScore(userId, delta)
  }

  def block(user: User, moderator: User, reason: String): Unit = transactional() { _ =>
    userDao.block(user, moderator, reason)
  }

  def score50(user: User, moderator: User): Unit = transactional() { _ =>
    userDao.score50(user, moderator)
  }

  def unblock(user: User, moderator: User): Unit = transactional() { _ =>
    userDao.unblock(user, moderator)
  }

  def toggleCorrector(user: User, moderator: User): Unit = transactional() { _ =>
    userDao.toggleCorrector(user, moderator)
  }

  def resetPassword(user: User): String = transactional() { _ =>
    val newPassword = StringUtil.generatePassword

    userDao.setPassword(user, newPassword)
    userLogDao.logResetPassword(user, user)

    newPassword
  }

  def resetPassword(user: User, moderator: User): Unit = transactional() { _ =>
    userDao.setPassword(user, StringUtil.generatePassword)
    userLogDao.logResetPassword(user, moderator)
  }

  def activateUser(user: User): Unit = transactional() { _ =>
    userDao.activateUser(user)
  }

  def acceptNewEmail(user: User, newEmail: String): Unit = transactional() { _ =>
    userDao.acceptNewEmail(user, newEmail)
  }

  def resetUserpic(user: User, cleaner: User): Boolean = transactional() { _ =>
    userDao.resetUserpic(user, cleaner)
  }

  def getByEmail(email: String, searchBlocked: Boolean): Option[User] = {
    val id = userDao.getByEmail(email, searchBlocked)

    if (id != 0) {
      Some(getUserCached(id))
    } else {
      None
    }
  }

  def getAllByEmail(email: String): Seq[User] = {
    if (email == null || email.isEmpty) {
      Seq.empty
    } else {
      val userIds = userDao.getAllByEmail(email).asScala

      userIds.view.map(i => getUserCached(i)).toVector
    }
  }
}