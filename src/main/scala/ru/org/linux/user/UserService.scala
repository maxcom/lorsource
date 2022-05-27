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
import play.api.libs.json.Json
import play.api.libs.ws.StandaloneWSClient
import ru.org.linux.auth.{AccessViolationException, IPBlockDao}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.user.UserService._
import ru.org.linux.util.image.{ImageInfo, ImageParam, ImageUtil}
import ru.org.linux.util.{BadImageException, StringUtil}

import java.io.{File, FileNotFoundException, IOException}
import java.sql.Timestamp
import java.util
import javax.annotation.Nullable
import javax.mail.internet.InternetAddress
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@Service
object UserService {
  val MaxFileSize: Int = 100 * 1024
  val MinImageSize = 50
  val MaxImageSize = 300

  val DisabledUserpic = new Userpic("/img/p.gif", 1, 1)

  val AnonymousUserId = 2

  private val NameCacheSize = 10000

  val MaxTotalInvites = 5
  val MaxUserInvites = 1
  val MaxInviteScoreLoss = 10
  val InviteScore = 200

  val MaxUnactivatedPerIp = 2
  val MaxNewUsers = 60 // 3 day window

  val CorrectorScore = 200
}

@Service
class UserService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao,
                  userInvitesDao: UserInvitesDao, userLogDao: UserLogDao, deleteInfoDao: DeleteInfoDao,
                  ipBlockDao: IPBlockDao, wsClient: StandaloneWSClient,
                  val transactionManager: PlatformTransactionManager)
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

  def ref(user: User, @Nullable requestUser: User): ApiUserRef = {
    if (requestUser != null && requestUser.isModerator && !user.isAnonymous) {
      new ApiUserRef(user.getNick, user.isBlocked, user.isAnonymous,
        User.getStars(user.getScore, user.getMaxScore, false), user.getScore, user.getMaxScore)
    } else {
      new ApiUserRef(user.getNick, user.isBlocked, user.isAnonymous,
        User.getStars(user.getScore, user.getMaxScore, false), null, null)
    }
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
      Some(new Userpic(gravatar("anonymous@linux.org.ru", avatarMode, 150), 150, 150))
    } else if (user.getPhoto != null && user.getPhoto.nonEmpty) {
      Try {
        val info = new ImageInfo(s"${siteConfig.getUploadPath}/photos/${user.getPhoto}").scale(150)

        new Userpic(s"/photos/${user.getPhoto}", info.getWidth, info.getHeight)
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
      if (user.hasGravatar && user.getPhoto != "") {
        new Userpic(gravatar(user.getEmail, avatarMode, 150), 150, 150)
      } else {
        UserService.DisabledUserpic
      }
    }
  }

  def getResetCode(nick: String, email: String, tm: Timestamp): String = {
    val base = siteConfig.getSecret
    StringUtil.md5hash(base + ':' + nick + ':' + email + ':' + tm.getTime.toString + ":reset")
  }

  def getUsersCached(ids: java.lang.Iterable[Integer]): java.util.List[User] =
    ids.asScala.map(x => userDao.getUserCached(x)).toSeq.asJava

  def getNewUsers: util.List[User] = getUsersCached(userDao.getNewUserIds)

  private def makeFrozenList(users: util.List[(Integer, DateTime)]): util.List[(User, Boolean)] = {
    val recentSeenDate = DateTime.now().minusDays(1)

    users.asScala.map { case (userId, lastlogin) =>
      val user = getUserCached(userId)
      (user, lastlogin.isAfter(recentSeenDate))
    }.asJava
  }

  def getFrozenUsers: util.List[(User, Boolean)] = makeFrozenList(userDao.getFrozenUserIds)
  def getUnFrozenUsers: util.List[(User, Boolean)] = makeFrozenList(userDao.getUnFrozenUserIds)

  def getRecentlyBlocked: util.List[User] = getUsersCached(userLogDao.getRecentlyHasEvent(UserLogAction.BLOCK_USER))
  def getRecentlyUnBlocked: util.List[User] = getUsersCached(userLogDao.getRecentlyHasEvent(UserLogAction.UNBLOCK_USER))

  def getModerators = getUsersCached(userDao.getModeratorIds)

  def getCorrectors = getUsersCached(userDao.getCorrectorIds)

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
                 ip: String, invite: Option[String]): Int = {
    transactional() { _ =>
      val newUserId = userDao.createUser(name, nick, password, url, mail, town, ip)

      invite.foreach { token =>
        val marked = userInvitesDao.markUsed(token, newUserId)

        if (!marked) {
          throw new AccessViolationException("Инвайт уже использован")
        }
      }

      val inviteOwner = invite.flatMap(userInvitesDao.ownerOfInvite)

      userLogDao.logRegister(newUserId, ip, inviteOwner.map(Integer.valueOf).asJava)

      newUserId
    }
  }

  def getAllInvitedUsers(user: User): util.List[User] =
    userInvitesDao.getAllInvitedUsers(user).map(userDao.getUserCached).asJava

  def canRegister(remoteAddr: String): Boolean = {
    val currentHour = DateTime.now().hourOfDay().get

    currentHour >= 8 && currentHour <= 22 &&
      !ipBlockDao.getBlockInfo(remoteAddr).isBlocked &&
      userDao.countUnactivated(remoteAddr) < MaxUnactivatedPerIp &&
      userDao.getNewUserIds.size() < MaxNewUsers &&
      getCountry(remoteAddr).exists(c => c != "UA")
  }

  def getCountry(remoteAddr: String): Option[String] = {
    val result = wsClient.url(s"https://ipwhois.app/json/$remoteAddr?lang=ru").get().map { response =>
      val data = Json.parse(response.body)

      if ((data \ "success").as[Boolean]) {
        val country = (data \ "country_code").asOpt[String]

        logger.debug(s"Country for ${remoteAddr}: $country")

        country // "RU"
      } else {
        logger.warn(s"Can't get country for $remoteAddr: ${data \ "message"}")
        None
      }
    }.recover { ex =>
      logger.warn(s"Can't get country for $remoteAddr", ex)
      None
    }

    Await.result(result, 10.seconds)
  }
}