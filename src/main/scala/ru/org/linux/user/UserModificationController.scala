/*
 * Copyright 1998-2023 Linux.org.ru
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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, ModeratorOnly}
import ru.org.linux.comment.CommentDeleteService
import ru.org.linux.search.SearchQueueSender

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}

@Controller
object UserModificationController {
  private def redirectToProfile(user: User) = new ModelAndView(new RedirectView(getNoCacheLinkToProfile(user)))

  private def getNoCacheLinkToProfile(user: User) = {
    "/people/" + URLEncoder.encode(user.getNick, StandardCharsets.UTF_8) + "/profile?nocache=" + ThreadLocalRandom.current().nextInt
  }

  // get 'now', add the duration and returns result;
  // the duration can be negative
  private def getUntil(shift: String): Timestamp = {
    val d = Duration.parse(shift)
    val now = new Timestamp(System.currentTimeMillis)
    now.setTime(now.getTime + d.toMillis)
    now
  }
}

@Controller
class UserModificationController(searchQueueSender: SearchQueueSender, userDao: UserDao,
                                 commentService: CommentDeleteService, userService: UserService) extends StrictLogging {
  import UserModificationController.*

  /**
   * Контроллер блокировки пользователя
   *
   * @param user   блокируемый пользователь
   * @param reason причина блокировки
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=block"))
  def blockUser(@RequestParam("id") user: User,
                @RequestParam(value = "reason", required = false) reason: String): ModelAndView = ModeratorOnly { moderator =>
    if (!userService.isBlockable(user = user, by = moderator.user)) {
      throw new AccessViolationException(s"Пользователя ${user.getNick} нельзя заблокировать")
    }

    if (user.isBlocked) {
      throw new UserErrorException("Пользователь уже блокирован")
    }

    userDao.block(user, moderator.user, reason)

    logger.info(s"User ${user.getNick} blocked by ${moderator.user.getNick}")

    redirectToProfile(user)
  }

  /**
   * Выставляем score=50 для пользователей у которых score меньше
   *
   * @param user кому ставим score
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=score50"))
  def score50(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.isBlocked || user.isAnonymous || user.getScore > 50) {
      throw new AccessViolationException(s"Нельзя выставить score=50 пользователю ${user.getNick}")
    }

    userDao.score50(user, moderator.user)

    redirectToProfile(user)
  }

  /**
   * Контроллер разблокировки пользователя
   *
   * @param user разблокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=unblock"))
  def unblockUser(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (!userService.isBlockable(user = user, by = moderator.user)) {
      throw new AccessViolationException(s"Пользователя ${user.getNick} нельзя разблокировать")
    }

    userDao.unblock(user, moderator.user)

    logger.info(s"User ${user.getNick} unblocked by ${moderator.user.getNick}")

    redirectToProfile(user)
  }

  /**
   * Контроллер блокирования и полного удаления комментариев и топиков пользователя
   *
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=block-n-delete-comments"))
  def blockAndMassiveDeleteCommentUser(@RequestParam("id") user: User,
                                       @RequestParam(value = "reason", required = false) reason: String): ModelAndView = ModeratorOnly { moderator =>
    if (!userService.isBlockable(user = user, by = moderator.user)) {
      throw new AccessViolationException(s"Пользователя ${user.getNick} нельзя заблокировать")
    }

    if (user.isBlocked) {
      throw new UserErrorException("Пользователь уже блокирован")
    }

    val params = new mutable.HashMap[String, AnyRef]
    params.put("message", "Удалено")
    val deleteCommentResult = commentService.deleteAllCommentsAndBlock(user, moderator.user, reason)

    logger.info(s"User ${user.getNick} blocked by ${moderator.user.getNick}")

    params.put("bigMessage", s"Удалено комментариев: ${deleteCommentResult.getDeletedCommentIds.size}<br>Удалено тем: ${deleteCommentResult.getDeletedTopicIds.size}")

    deleteCommentResult.getDeletedTopicIds.asScala.foreach { topicId =>
      searchQueueSender.updateMessage(topicId, true)
    }

    searchQueueSender.updateComment(deleteCommentResult.getDeletedCommentIds)

    new ModelAndView("action-done", params.asJava)
  }

  /**
   * Контроллер смена признака корректора
   *
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=toggle_corrector"))
  def toggleUserCorrector(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.getScore < UserService.CorrectorScore) {
      throw new AccessViolationException(s"Пользователя ${user.getNick} нельзя сделать корректором")
    }

    userDao.toggleCorrector(user, moderator.user)

    logger.info(s"Toggle corrector ${user.getNick} by ${moderator.user.getNick}")

    redirectToProfile(user)
  }

  /**
   * Сброс пароля пользователю
   *
   * @param user пользователь которому сбрасываем пароль
   * @return сообщение о успешности сброса
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=reset-password"))
  def resetPassword(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.isModerator && !moderator.user.isAdministrator) {
      throw new AccessViolationException(s"Пользователю ${user.getNick} нельзя сбросить пароль")
    }

    if (user.isAnonymous) {
      throw new AccessViolationException(s"Пользователю ${user.getNick} нельзя сбросить пароль")
    }

    userDao.resetPassword(user, moderator.user)

    logger.info(s"Пароль ${user.getNick} сброшен модератором ${moderator.user.getNick}")

    val mv = new ModelAndView("action-done")

    mv.getModel.put("link", getNoCacheLinkToProfile(user))
    mv.getModel.put("message", "Пароль сброшен")

    mv
  }

  /**
   * Контроллер отчистки дополнительной информации в профиле
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=remove_userinfo"))
  def removeUserInfo(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.isAnonymous) {
      throw new AccessViolationException(s"Пользователю ${user.getNick} нельзя удалить сведения")
    }

    userService.removeUserInfo(user, moderator.user)

    UserModificationController.redirectToProfile(user)
  }

  /**
   * Контроллер отчистки поля город
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=remove_town"))
  def removeTown(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.isAnonymous) {
      throw new AccessViolationException(s"Пользователю ${user.getNick} нельзя удалить сведения")
    }

    userService.removeTown(user, moderator.user)

    redirectToProfile(user)
  }

  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=remove_url"))
  def removeUrl(@RequestParam("id") user: User): ModelAndView = ModeratorOnly { moderator =>
    if (user.isAnonymous) {
      throw new AccessViolationException(s"Пользователю ${user.getNick} нельзя удалить сведения")
    }

    userService.removeUrl(user, moderator.user)

    redirectToProfile(user)
  }

  @RequestMapping(value = Array("/remove-userpic.jsp"), method = Array(RequestMethod.POST))
  def removeUserpic(@RequestParam("id") user: User): ModelAndView = AuthorizedOnly { currentUser =>
    // Не модератор не может удалять чужие аватары
    if (!currentUser.moderator && currentUser.user.getId != user.getId) {
      throw new AccessViolationException("Not permitted")
    }

    if (userDao.resetUserpic(user, currentUser.user)) {
      logger.info(s"Clearing ${user.getNick} userpic by ${currentUser.user.getNick}")
    }

    redirectToProfile(user)
  }

  /**
   * Контроллер заморозки и разморозки пользователя
   *
   * @param user   блокируемый пользователь
   * @param reason причина заморозки, общедоступна в дальнейшем
   * @param shift  отсчёт времени от текущей точки, может быть отрицательным, в
   *               в результате даёт until, отрицательное значение используется
   *               для разморозки
   * @return возвращаемся в профиль
   */
  @RequestMapping(value = Array("/usermod.jsp"), method = Array(RequestMethod.POST), params = Array("action=freeze"))
  def freezeUser(@RequestParam(name = "id") user: User, @RequestParam(name = "reason") reason: String,
                 @RequestParam(name = "shift") shift: String): ModelAndView = ModeratorOnly { moderator =>
    if (reason.length > 255) {
      throw new UserErrorException("Причина слишком длиная, максимум 255 байт")
    }

    val until = UserModificationController.getUntil(shift)

    if (!userService.isFreezable(user = user, by = moderator.user)) {
      throw new AccessViolationException(s"Пользователя ${user.getNick} нельзя заморозить")
    }

    if (user.isBlocked) {
      throw new UserErrorException("Пользователь блокирован, его нельзя заморозить")
    }

    userDao.freezeUser(user, moderator.user, reason, until)

    logger.info(s"Freeze ${user.getNick} by ${moderator.user.getNick} until $until")

    redirectToProfile(user)
  }

  @RequestMapping(value = Array("/people/{nick}/profile/wipe"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def wipe(@PathVariable nick: String): ModelAndView = ModeratorOnly { moderator =>
    val user = userService.getUser(nick)

    if (!userService.isBlockable(user = user, by = moderator.user)) {
      throw new AccessViolationException("Пользователя нельзя заблокировать")
    }

    if (user.isBlocked) {
      throw new UserErrorException("Пользователь уже блокирован")
    }

    val mv = new ModelAndView("wipe-user")

    mv.getModel.put("user", user)
    mv.getModel.put("commentCount", userDao.getExactCommentCount(user))

    mv
  }

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[User], new UserIdPropertyEditor(userService))
  }
}