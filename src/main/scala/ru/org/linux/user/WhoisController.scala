/*
 * Copyright 1998-2024 Linux.org.ru
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
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.site.Template
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.util.bbcode.LorCodeService

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

@Controller
class WhoisController(userStatisticsService: UserStatisticsService, userDao: UserDao, ignoreListDao: IgnoreListDao,
                      lorCodeService: LorCodeService, userTagService: UserTagService,
                      topicPermissionService: TopicPermissionService, userService: UserService, userLogDao: UserLogDao,
                      userLogPrepareService: UserLogPrepareService, remarkDao: RemarkDao, memoriesDao: MemoriesDao,
                      topicDao: TopicDao) extends StrictLogging {
  @RequestMapping(value = Array("/people/{nick}/profile"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def getInfoNew(@PathVariable nick: String): CompletionStage[ModelAndView] = MaybeAuthorized { currentUserOpt =>
    val user = userService.getUser(nick)

    if (user.isBlocked && !currentUserOpt.authorized) {
      throw new UserBanedException(user, userDao.getBanInfoClass(user))
    }

    if (!user.isActivated && !currentUserOpt.moderator) {
      throw new UserNotFoundException(user.getName)
    }

    val userStatsF = userStatisticsService.getStats(user)

    val mv = new ModelAndView("whois")

    mv.getModel.put("user", user)
    mv.getModel.put("userInfo", userDao.getUserInfoClass(user))

    val tmpl = Template.getTemplate

    mv.getModel.put("userpic", userService.getUserpic(user, tmpl.getProf.getAvatarMode, misteryMan = true))

    if (user.isBlocked) {
      mv.getModel.put("banInfo", userDao.getBanInfoClass(user))
    }

    mv.getModel.put("blockable", currentUserOpt.opt.exists(by => userService.isBlockable(user = user, by = by.user)))
    mv.getModel.put("freezable", currentUserOpt.opt.exists(by => userService.isFreezable(user = user, by = by.user)))

    // add the isFrozen to simplify controller,
    // and put information about moderator who
    // freezes the user, if frozen
    if (user.isFrozen) {
      mv.getModel.put("isFrozen", true)
      val freezer = userService.getUserCached(user.getFrozenBy)
      mv.getModel.put("freezer", freezer)
    }

    if (currentUserOpt.moderator) {
      val othersWithSameEmail = userDao.getAllByEmail(user.getEmail).asScala.filter(_.getId != user.getId)

      mv.getModel.put("otherUsers", othersWithSameEmail.asJava)
    }

    if (!user.isAnonymous) {
      mv.getModel.put("watchPresent", memoriesDao.isWatchPresetForUser(user))
      mv.getModel.put("favPresent", memoriesDao.isFavPresetForUser(user))
    }

    val viewByOwner = currentUserOpt.userOpt.exists(_.getNick == nick)

    mv.getModel.put("moderatorOrCurrentUser", viewByOwner || currentUserOpt.moderator)
    mv.getModel.put("viewByOwner", viewByOwner)
    mv.getModel.put("canInvite", viewByOwner && userService.canInvite(user))

    currentUserOpt.userOpt.foreach { currentUser =>
      if (!viewByOwner) {
        val ignoreList = ignoreListDao.get(currentUser.getId)

        mv.getModel.put("ignored", ignoreList.contains(user.getId))

        remarkDao.getRemark(currentUser, user).foreach { remark =>
          mv.getModel.put("remark", remark)
        }
      }
    }

    if (viewByOwner) {
      mv.getModel.put("hasRemarks", remarkDao.hasRemarks(user))
      mv.getModel.put("canLoadUserpic", userService.canLoadUserpic(user))
    }

    val userinfo = userDao.getUserInfo(user)

    if (!Strings.isNullOrEmpty(userinfo)) {
      mv.getModel.put("userInfoText", lorCodeService.parseComment(userinfo, !topicPermissionService.followAuthorLinks(user)))
    }

    mv.addObject("favoriteTags", userTagService.favoritesGet(user))

    if (viewByOwner || currentUserOpt.moderator) {
      mv.addObject("ignoreTags", userTagService.ignoresGet(user))

      val logItems = userLogDao.getLogItems(user, currentUserOpt.moderator)
      if (!logItems.isEmpty) mv.addObject("userlog", userLogPrepareService.prepare(logItems))

      mv.getModel.put("hasDrafts", topicDao.hasDrafts(user))
      mv.getModel.put("invitedUsers", userService.getAllInvitedUsers(user))
    }

    userStatsF.map { userStat =>
      mv.getModel.put("userStat", userStat)

      mv
    }.toJava
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

    if (!currentUser.moderator) {
      user.checkBlocked()
    }

    val timezone = request.getAttribute("timezone").asInstanceOf[DateTimeZone]

    userStatisticsService.getYearStats(user, timezone).map(_.asJson).toJava
  }
}