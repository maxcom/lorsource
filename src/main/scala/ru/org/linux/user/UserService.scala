/*
 * Copyright 1998-2018 Linux.org.ru
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

import java.io.{File, FileNotFoundException, IOException}
import java.sql.Timestamp
import javax.annotation.Nullable

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.common.util.concurrent.UncheckedExecutionException
import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Service
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.image.{ImageInfo, ImageParam, ImageUtil}
import ru.org.linux.util.{BadImageException, StringUtil}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Service
object UserService {
  val MaxFileSize = 35000
  val MinImageSize = 50
  val MaxImageSize = 150

  val DisabledUserpic = new Userpic("/img/p.gif", 1, 1)

  val AnonymousUserId = 2

  private val NameCacheSize = 10000
}

@Service
class UserService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao) extends StrictLogging {
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

    s"https://secure.gravatar.com/avatar/$emailHash?s=$size&r=g&d=$nonExist"
  }

  def getUserpic(user: User, avatarStyle: String, misteryMan: Boolean): Userpic = {
    val avatarMode = if (misteryMan && ("empty" == avatarStyle)) {
       "mm"
    } else {
      avatarStyle
    }

    val userpic = if (user.isAnonymous && misteryMan) {
      Some(new Userpic(gravatar("anonymous@linux.org.ru", avatarMode, 150), 150, 150))
    } else if (user.getPhoto != null && !user.getPhoto.isEmpty) {
      Try {
        val info = new ImageInfo(siteConfig.getUploadPath + "/photos/" + user.getPhoto)
        new Userpic("/photos/" + user.getPhoto, info.getWidth, info.getHeight)
      } match {
        case Failure(e: FileNotFoundException) ⇒
          logger.warn(s"Userpic not found for ${user.getNick}: ${e.getMessage}")
          None
        case Failure(e) ⇒
          logger.warn(s"Bad userpic for ${user.getNick}", e)
          None
        case Success(u) ⇒
          Some(u)
      }
    } else {
      None
    }

    userpic.getOrElse {
      if (user.hasGravatar && user.getPhoto!="") {
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
    ids.asScala.map(x ⇒ userDao.getUserCached(x)).toSeq.asJava

  def getNewUsers = getUsersCached(userDao.getNewUserIds)

  def getModerators = getUsersCached(userDao.getModeratorIds)

  def getCorrectors = getUsersCached(userDao.getCorrectorIds)

  private def findUserIdCached(nick:String):Int = {
    try {
      nameToIdCache.get(nick)
    } catch {
      case ex:UncheckedExecutionException ⇒ throw ex.getCause
    }
  }

  def getUserCached(nick: String): User = userDao.getUserCached(findUserIdCached(nick))

  def findUserCached(nick: String): Option[User] = try {
    Some(userDao.getUserCached(findUserIdCached(nick)))
  } catch {
    case _: UserNotFoundException ⇒
      None
  }

  def getUserCached(id: Int) = userDao.getUserCached(id)

  def getUser(nick: String) = userDao.getUser(findUserIdCached(nick))

  def getAnonymous: User = {
    try {
      userDao.getUserCached(UserService.AnonymousUserId)
    } catch {
      case e: UserNotFoundException ⇒
        throw new RuntimeException("Anonymous not found!?", e)
    }
  }
}