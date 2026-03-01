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

import com.google.common.base.Strings
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.syntax.*
import jakarta.servlet.http.HttpServletRequest
import org.joda.time.DateTimeZone
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.util.bbcode.LorCodeService

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage
import scala.jdk.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

@Controller
class WhoisController(userStatisticsService: UserStatisticsService, userDao: UserDao, ignoreListDao: IgnoreListDao,
                      lorCodeService: LorCodeService, userTagService: UserTagService,
                      topicPermissionService: TopicPermissionService, userService: UserService, userLogDao: UserLogDao,
                      userLogPrepareService: UserLogPrepareService, remarkDao: RemarkDao, memoriesDao: MemoriesDao,
                      topicDao: TopicDao, userPermissionService: UserPermissionService,
                      deleteInfoDao: DeleteInfoDao) extends StrictLogging {
  @RequestMapping(value = Array("/people/{nick}/profile"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def getInfoNew(@PathVariable nick: String, request: HttpServletRequest): CompletionStage[ModelAndView] = MaybeAuthorized { currentUserOpt =>
    val user = userService.getUser(nick)

    if (user.blocked && !currentUserOpt.authorized) {
      throw new UserBanedException(user, userDao.getBanInfoClass(user))
    }

    if (!user.activated && !currentUserOpt.moderator) {
      throw new UserNotFoundException(user.getName)
    }

    val userStatsF = userStatisticsService.getStats(user)

    val mv = new ModelAndView("whois")

    mv.getModel.put("user", user)
    val userInfo = userDao.getUserInfoClass(user)
    mv.getModel.put("userInfo", userInfo)

    mv.getModel.put("userpic", userService.getUserpic(user, currentUserOpt.profile.avatarMode, misteryMan = true))

    if (user.blocked) {
      val banInfo = userDao.getBanInfoClass(user)
      mv.getModel.put("banInfo", banInfo)

      if (banInfo.moderator != 0) {
        mv.getModel.put("bannedBy", userService.getUserCached(banInfo.moderator))
      }
    }

    mv.getModel.put("blockable", currentUserOpt.opt.exists(by => userService.isBlockable(user = user, by = by.user)))
    mv.getModel.put("freezable", currentUserOpt.opt.exists(by => userService.isFreezable(user = user, by = by.user)))

    // add the isFrozen to simplify controller,
    // and put information about moderator who
    // freezes the user, if frozen
    if (user.isFrozen) {
      mv.getModel.put("isFrozen", true)
      val freezer = userService.getUserCached(userInfo.frozenBy)
      mv.getModel.put("freezer", freezer)
    }

    if (currentUserOpt.moderator) {
      val othersWithSameEmail = userService.getAllByEmail(user.email).filter(_.id != user.id)

      mv.getModel.put("otherUsers", othersWithSameEmail.asJava)

      mv.getModel.put("recentScoreLoss", deleteInfoDao.getRecentScoreLoss(user))
    }

    if (!user.anonymous) {
      mv.getModel.put("watchPresent", memoriesDao.isWatchPresetForUser(user))
      mv.getModel.put("favPresent", memoriesDao.isFavPresetForUser(user))
    }

    val viewByOwner = currentUserOpt.userOpt.exists(_.nick == nick)

    mv.getModel.put("moderatorOrCurrentUser", viewByOwner || currentUserOpt.moderator)
    mv.getModel.put("viewByOwner", viewByOwner)

    currentUserOpt.userOpt.foreach { currentUser =>
      if (!viewByOwner) {
        val ignoreList = ignoreListDao.get(currentUser.id)

        mv.getModel.put("ignored", ignoreList.contains(user.id))

        remarkDao.getRemark(currentUser, user).foreach { remark =>
          mv.getModel.put("remark", remark)
        }
      }
    }

    if (viewByOwner) {
      currentUserOpt.opt.foreach { implicit authorized =>
        mv.getModel.put("hasRemarks", remarkDao.hasRemarks(user))
        mv.getModel.put("canLoadUserpic", userPermissionService.canLoadUserpic)
        mv.getModel.put("canInvite", userPermissionService.canInvite)
      }
    }

    val userinfo = userDao.getUserInfo(user)

    if (!Strings.isNullOrEmpty(userinfo)) {
      mv.getModel.put("userInfoText",
        lorCodeService.parseComment(userinfo, !topicPermissionService.followAuthorLinks(user), LorCodeService.Plain))
    }

    mv.addObject("favoriteTags", userTagService.favoritesGet(user))

    if (viewByOwner || currentUserOpt.moderator) {
      mv.addObject("ignoreTags", userTagService.ignoresGet(user))

      val logItems = userLogDao.getLogItems(user, currentUserOpt.moderator).asScala
      if (logItems.nonEmpty) {
        val timezone = request.getAttribute("timezone").asInstanceOf[DateTimeZone]

        mv.addObject("userlog", userLogPrepareService.prepare(logItems, timezone).asJava)
      }

      mv.getModel.put("hasDrafts", topicDao.hasDrafts(user))
      mv.getModel.put("invitedUsers", userService.getAllInvitedUsers(user))
    }

    userStatsF.map { userStat =>
      mv.getModel.put("userStat", userStat)

      mv
    }.asJava
  }

  @RequestMapping(path = Array("/whois.jsp"))
  def getInfo(@RequestParam("nick") nick: String) =
    new RedirectView("/people/" + URLEncoder.encode(nick, StandardCharsets.UTF_8) + "/profile")

  /**
   * Обрабатываем исключительную ситуацию для забаненого пользователя
   */
  @ExceptionHandler(Array(classOf[UserBanedException]))
  @ResponseStatus(HttpStatus.FORBIDDEN)
  def handleUserBanedException(ex: UserBanedException) =
    new ModelAndView("errors/user-banned", "exception", ex)

  @ExceptionHandler(Array(classOf[UserNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleUserNotFound(ex: UserNotFoundException): ModelAndView = {
    logger.debug("User not found {}", ex.toString)

    val mav = new ModelAndView("errors/good-penguin")

    mav.addObject("msgTitle", "Ошибка: пользователя не существует")
    mav.addObject("msgHeader", "Пользователя не существует")
    mav.addObject("msgMessage", "")

    mav
  }

  @RequestMapping(value = Array("/people/{nick}/profile"), method = Array(RequestMethod.GET, RequestMethod.HEAD), params = Array("year-stats"))
  @ResponseBody
  def yearStats(@PathVariable nick: String, request: HttpServletRequest): CompletionStage[Json] = MaybeAuthorized { currentUser =>
    val user = userService.getUser(nick)

    if (!currentUser.moderator && user.blocked) {
      throw new AccessViolationException("Пользователь заблокирован")
    }

    val timezone = request.getAttribute("timezone").asInstanceOf[DateTimeZone]

    userStatisticsService.getYearStats(user, timezone).map(_.asJson).asJava
  }
}