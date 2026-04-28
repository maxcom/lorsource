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

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.util.StringUtil

import scala.jdk.CollectionConverters.MapHasAsJava

@Controller
class ResetPasswordController(userDao: UserDao, userService: UserService,
                              userPermissionService: UserPermissionService) extends StrictLogging {
  @RequestMapping(value = Array("/people/{nick}/profile"), method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("reset-password"))
  def showModeratorForm(@PathVariable nick: String): ModelAndView = ModeratorOnly { _ =>
    val user = userService.getUser(nick)

    val modelAndView = new ModelAndView("confirm-password-reset")

    modelAndView.addObject("user", user)
    modelAndView.addObject("whoisLink",
      UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode.toUriString)

    modelAndView
  }

  @RequestMapping(value = Array("/reset-password"), method = Array(RequestMethod.GET))
  def showCodeForm = new ModelAndView("reset-password-form")

  @RequestMapping(value = Array("/reset-password"), method = Array(RequestMethod.POST))
  def resetPassword(request: HttpServletRequest, @RequestParam("nick") nick: String,
                    @RequestParam("code") formCode: String): ModelAndView = {
    val user = userService.getUser(nick)

    if (!userPermissionService.canResetPasswordByCode(user)) {
      throw new AccessViolationException("Пароль этого пользователя нельзя сбросить")
    }

    val resetDate = userDao.getResetDate(user)
    val resetCode = userService.getResetCode(user.nick, user.email, resetDate)

    if (resetCode != formCode) {
      logger.warn("Код проверки не совпадает; login={} formCode={} resetCode={}", nick, formCode, resetCode)

      throw new UserErrorException("Код не совпадает")
    } else {
      val password = userService.resetPassword(user)

      request.setAttribute("enableAjaxLogin", false)

      new ModelAndView("action-done", Map("message" -> "Установлен новый пароль",
        "bigMessage" -> ("Ваш новый пароль: " + StringUtil.escapeHtml(password))).asJava)
    }
  }

  @ExceptionHandler(Array(classOf[UserNotFoundException], classOf[UserErrorException]))
  def handleUserError(ex: Exception) = new ModelAndView("reset-password-form", "error", ex.getMessage)
}