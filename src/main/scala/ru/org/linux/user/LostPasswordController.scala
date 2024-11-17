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
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ExceptionHandler, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.email.EmailService
import ru.org.linux.site.{BadInputException, Template}

import java.sql.Timestamp
import javax.mail.internet.AddressException

@Controller
@RequestMapping(Array("/lostpwd.jsp"))
class LostPasswordController(userDao: UserDao, userService: UserService, emailService: EmailService) {
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm: ModelAndView = new ModelAndView("lostpwd-form")

  @RequestMapping(method = Array(RequestMethod.POST))
  @throws[Exception]
  def sendPassword(@RequestParam("email") email: String): ModelAndView = MaybeAuthorized { currentUser =>
    val tmpl = Template.getTemplate
    if (Strings.isNullOrEmpty(email)) throw new BadInputException("email не задан")

    val user = userDao.getByEmail(email, true)
    if (user == null) {
      throw new BadInputException("Этот email не зарегистрирован!")
    }

    user.checkBlocked()

    if (user.isAnonymous) {
      throw new AccessViolationException("Anonymous user")
    }

    if (user.isModerator && !currentUser.moderator) {
      throw new AccessViolationException("этот пароль могут сбросить только модераторы")
    }

    if (!currentUser.moderator && !userService.canResetPassword(user)) {
      throw new BadInputException("Нельзя запрашивать пароль чаще одного раза в неделю!")
    }

    val now = new Timestamp(System.currentTimeMillis)

    try {
      val resetCode = userService.getResetCode(user.getNick, user.getEmail, now)

      emailService.sendPasswordReset(user, resetCode)

      userService.updateResetDate(user, currentUser.userOpt.orNull, user.getEmail, now)

      new ModelAndView("action-done", "message", "Инструкция по сбросу пароля была отправлена на ваш email")
    } catch {
      case _: AddressException =>
        throw new UserErrorException("Incorrect email address")
    }
  }

  @ExceptionHandler(Array(classOf[UserErrorException]))
  def handleUserError(ex: UserErrorException) = new ModelAndView("lostpwd-form", "error", ex.getMessage)
}