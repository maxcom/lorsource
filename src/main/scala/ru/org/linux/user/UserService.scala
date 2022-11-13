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
package ru.org.linux.user

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.common.util.concurrent.UncheckedExecutionException
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.auth.{AccessViolationException, IPBlockDao}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.{DeleteInfoDao, UserAgentDao}
import ru.org.linux.user.UserService.*
import ru.org.linux.util.image.{ImageInfo, ImageParam, ImageUtil}
import ru.org.linux.util.{BadImageException, StringUtil}

import java.io.{File, FileNotFoundException, IOException}
import java.sql.Timestamp
import java.time.Duration
import java.util
import javax.annotation.Nullable
import javax.mail.internet.InternetAddress
import scala.collection.mutable
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

@Service
object UserService {
  val MaxFileSize: Int = 100 * 1024
  val MinImageSize = 50
  val MaxImageSize = 300

  val DisabledUserpic: Userpic = Userpic("/img/p.gif", 1, 1)

  val AnonymousUserId = 2

  private val NameCacheSize = 10000

  val MaxTotalInvites = 15
  val MaxUserInvites = 1
  val MaxInviteScoreLoss = 10
  val InviteScore = 200

  val MaxUnactivatedPerIp = 2

  // используется в jsp, не удалять!
  val CorrectorScore = 200

  val MaxUserpicScoreLoss = 20
}

@Service
class UserService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao,
                  userInvitesDao: UserInvitesDao, userLogDao: UserLogDao, deleteInfoDao: DeleteInfoDao,
                  ipBlockDao: IPBlockDao, userAgentDao: UserAgentDao, val transactionManager: PlatformTransactionManager)
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

  def isIgnoring(userId: Int, ignoredUserId: Int): Boolean = {
    val ignoredUsers = ignoreListDao.get(getUserCached(userId))
    ignoredUsers.contains(ignoredUserId)
  }

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

    val userpic = if (user.isAnonymous && misteryMan) {
      Some(Userpic(gravatar("anonymous@linux.org.ru", avatarMode, 150), 150, 150))
    } else if (user.getPhoto != null && user.getPhoto.nonEmpty) {
      Try {
        val info = new ImageInfo(s"${siteConfig.getUploadPath}/photos/${user.getPhoto}").scale(150)

        Userpic(s"/photos/${user.getPhoto}", info.getWidth, info.getHeight)
      } match {
        case Failure(e: FileNotFoundException) =>
          logger.warn(s"Userpic not found for ${user.getNick}: ${e.getMessage}")
          None
        case Failure(e) =>
          logger.warn(s"Bad userpic for ${user.getNick}", e)
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
        Userpic(gravatar(user.getEmail, avatarMode, 150), 150, 150)
      }
    }
  }

  def getResetCode(nick: String, email: String, tm: Timestamp): String = {
    val base = siteConfig.getSecret
    StringUtil.md5hash(s"$base:$nick:$email:${tm.getTime.toString}:reset")
  }

  def getUsersCached(ids: Iterable[Int]): Seq[User] = ids.map(x => userDao.getUserCached(x)).toSeq

  def getUsersCachedMap(userIds: Iterable[Int]): Map[Int, User] =
    getUsersCached(userIds.toSet).view.map(u => u.getId -> u).toMap

  def getUsersCachedJava(ids: java.lang.Iterable[Integer]): util.List[User] =
    getUsersCached(ids.asScala.map(i => i)).asJava

  def getNewUsers: util.List[User] = getUsersCachedJava(userDao.getNewUserIds)

  def getNewUsersByIp(ip: String): util.List[(User, Timestamp, Timestamp)] = userDao.getNewUsersByIP(ip).asScala.map { case (id, regdate, lastlogin) =>
    (getUserCached(id), regdate, lastlogin)
  }.asJava

  private def makeFrozenList(users: util.List[(Integer, DateTime)], activityDays: Int): util.List[(User, Boolean)] = {
    val recentSeenDate = DateTime.now().minusDays(activityDays)

    users.asScala.map { case (userId, lastlogin) =>
      val user = getUserCached(userId)
      (user, lastlogin.isAfter(recentSeenDate))
    }.asJava
  }

  def getFrozenUsers: util.List[(User, Boolean)] = makeFrozenList(userDao.getFrozenUserIds, activityDays = 1)

  def getUnFrozenUsers: util.List[(User, Boolean)] = makeFrozenList(userDao.getUnFrozenUserIds, activityDays = 1)

  def getRecentlyBlocked: util.List[User] = getUsersCachedJava(userLogDao.getRecentlyHasEvent(UserLogAction.BLOCK_USER))

  def getRecentlyUnBlocked: util.List[User] = getUsersCachedJava(userLogDao.getRecentlyHasEvent(UserLogAction.UNBLOCK_USER))

  def getModerators: util.List[(User, Boolean)] = makeFrozenList(userDao.getModerators, activityDays = 30)

  def getCorrectors: util.List[(User, Boolean)] = makeFrozenList(userDao.getCorrectors, activityDays = 30)

  def getRecentUserpics: util.List[(User, Userpic)] = {
    val userIds: Seq[Int] = userLogDao.getRecentlyHasEvent(UserLogAction.SET_USERPIC).asScala.map(_.toInt).toSeq.distinct

    getUsersCached(userIds).map { user =>
      user -> getUserpic(user, "empty", misteryMan = false)
    }.filterNot(_._2 == DisabledUserpic).asJava
  }

  private def findUserIdCached(nick: String): Int = {
    try {
      nameToIdCache.get(nick)
    } catch {
      case ex: UncheckedExecutionException => throw ex.getCause
    }
  }

  def getUserCached(nick: String): User = userDao.getUserCached(findUserIdCached(nick))

  def findUserCached(nick: String): Option[User] = try {
    Some(userDao.getUserCached(findUserIdCached(nick)))
  } catch {
    case _: UserNotFoundException =>
      None
  }

  def getUserCached(id: Int): User = userDao.getUserCached(id)

  def getUser(nick: String): User = userDao.getUser(findUserIdCached(nick))

  def getAnonymous: User = {
    try {
      userDao.getUserCached(UserService.AnonymousUserId)
    } catch {
      case e: UserNotFoundException =>
        throw new RuntimeException("Anonymous not found!?", e)
    }
  }

  def canInvite(user: User): Boolean = user.isModerator || {
    lazy val (totalInvites, userInvites) = userInvitesDao.countValidInvites(user)
    lazy val userScoreLoss = deleteInfoDao.getRecentScoreLoss(user)

    !user.isFrozen && user.getScore > InviteScore &&
      totalInvites < MaxTotalInvites &&
      userInvites < MaxUserInvites &&
      userScoreLoss < MaxInviteScoreLoss
  }

  def createUser(name: String, nick: String, password: String, url: String, mail: InternetAddress, town: String,
                 ip: String, invite: Option[String], userAgent: Option[String]): Int = {
    transactional() { _ =>
      val newUserId = userDao.createUser(name, nick, password, url, mail, town)

      invite.foreach { token =>
        val marked = userInvitesDao.markUsed(token, newUserId)

        if (!marked) {
          throw new AccessViolationException("Инвайт уже использован")
        }
      }

      val inviteOwner = invite.flatMap(userInvitesDao.ownerOfInvite)

      val userAgentId = userAgent.map(userAgentDao.createOrGetId).getOrElse(0)

      userLogDao.logRegister(newUserId, ip, inviteOwner.map(Integer.valueOf).asJava, userAgentId)

      newUserId
    }
  }

  def getAllInvitedUsers(user: User): util.List[User] =
    userInvitesDao.getAllInvitedUsers(user).map(userDao.getUserCached).asJava

  def canRegister(remoteAddr: String): Boolean = {
    !ipBlockDao.getBlockInfo(remoteAddr).isBlocked &&
      userDao.countUnactivated(remoteAddr) < MaxUnactivatedPerIp
  }

  def canLoadUserpic(user: User): Boolean = {
    def userpicSetCount = userLogDao.getUserpicSetCount(user, Duration.ofHours(1))

    def wasReset = userLogDao.hasRecentModerationEvent(user, Duration.ofDays(30), UserLogAction.RESET_USERPIC)

    def userScoreLoss = deleteInfoDao.getRecentScoreLoss(user)

    user.getScore >= 50 &&
      !user.isFrozen &&
      (userpicSetCount < 3) &&
      !wasReset &&
      (userScoreLoss < MaxUserpicScoreLoss)
  }

  def canEditProfileInfo(user: User): Boolean =
    !user.isFrozen &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_INFO) &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_URL) &&
      !userLogDao.hasRecentModerationEvent(user, Duration.ofDays(1), UserLogAction.RESET_TOWN)

  def removeUserInfo(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfo(user)

    if ((userInfo != null) && userInfo.trim.nonEmpty) {
      userDao.updateUserInfo(user.getId, null)
      userDao.changeScore(user.getId, -10)
      userLogDao.logResetInfo(user, moderator, userInfo, -10)
    }
  }

  def removeTown(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfoClass(user)

    if (userInfo.getTown != null && userInfo.getTown.trim.nonEmpty) {
      userDao.removeTown(user)
      userLogDao.logResetTown(user, moderator, userInfo.getTown, -10)
      userDao.changeScore(user.getId, -10)
    }
  }

  def removeUrl(user: User, moderator: User): Unit = transactional() { _ =>
    val userInfo = userDao.getUserInfoClass(user)

    if (userInfo.getUrl != null || userInfo.getUrl.trim.nonEmpty) {
      userDao.removeUrl(user)
      userLogDao.logResetUrl(user, moderator, userInfo.getUrl, 0)
      userDao.changeScore(user.getId, 0)
    }
  }

  def updateUser(user: User, name: String, url: String, @Nullable newEmail: String, town: String,
                @Nullable password: String, info: String): Unit = transactional() { _ =>
    val changed = mutable.Map[String, String]()

    if (userDao.updateName(user, name)) {
      changed += "name" -> name
    }

    if (userDao.updateUrl(user, url)) {
      changed += "url" -> url
    }

    if (userDao.updateTown(user, town)) {
      changed += "town" -> town
    }

    if (userDao.updateUserInfo(user.getId, info)) {
      changed += "info" -> info
    }

    updateEmailPasswd(user, newEmail, password)

    if (changed.nonEmpty) {
      userLogDao.logSetUserInfo(user, changed.asJava)
    }
  }

  def updateEmailPasswd(user: User, @Nullable newEmail: String,
                        @Nullable password: String): Unit = transactional() { _ =>
    if (password != null) {
      userDao.setPassword(user, password)
      userLogDao.logSetPassword(user)
    }

    if (newEmail != null) {
      userDao.setNewEmail(user, newEmail)
    }
  }
}