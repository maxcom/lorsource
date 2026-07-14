/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.user

import com.google.common.base.Strings
import jakarta.mail.internet.AddressException
import jakarta.servlet.http.HttpServletRequest
import org.apache.pekko.actor.Scheduler
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.{ModelAttribute, RequestAttribute, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.auth.{AccessViolationException, CaptchaService, LoginController, SecretTokenService}
import ru.org.linux.email.EmailService

import java.time.Instant
import java.util.concurrent.CompletionStage
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Controller @RequestMapping(Array("/lostpwd.jsp"))
class LostPasswordController(
    userService: UserService,
    emailService: EmailService,
    userPermissionService: UserPermissionService,
    secretTokenService: SecretTokenService,
    captchaService: CaptchaService,
    scheduler: Scheduler):
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm(
      @ModelAttribute("form")
      form: LostPasswordRequest): ModelAndView = new ModelAndView("lostpwd-form")

  @RequestMapping(method = Array(RequestMethod.POST))
  def sendPassword(
      @ModelAttribute("form")
      form: LostPasswordRequest,
      errors: BindingResult,
      request: HttpServletRequest,
      @RequestAttribute("captchaRequired")
      captchaRequired: Boolean): CompletionStage[ModelAndView] =
    MaybeAuthorized { currentUser =>
      LoginController.delayResponse(scheduler):
        val email = form.email

        if Strings.isNullOrEmpty(email) then
          errors.rejectValue("email", null, "email не задан")

        if captchaRequired && !errors.hasErrors then
          captchaService.checkCaptcha(request, errors)

        if errors.hasErrors then
          formErrorView(form, errors)
        else
          userService.getByEmail(email, searchBlocked = true) match
            case None =>
              errors.rejectValue("email", null, "Этот email не зарегистрирован!")
              formErrorView(form, errors)
            case Some(user) =>
              if !userPermissionService.canResetPasswordByCode(user) then
                throw new AccessViolationException("Пароль этого пользователя нельзя сбросить через email")
              else if user.isModerator && !currentUser.moderator then
                throw new AccessViolationException("этот пароль могут сбросить только модераторы")
              else if !currentUser.moderator && !userPermissionService.canResetPassword(user) then
                throw new AccessViolationException("Нельзя запрашивать пароль чаще одного раза в день!")
              else
                val now = Instant.now

                try
                  val resetCode = secretTokenService.getResetCode(user.nick, user.email, now)

                  emailService.sendPasswordReset(user, resetCode)

                  userService.updateResetDate(user, currentUser.userOpt.orNull, user.email, now)

                  new ModelAndView("action-done", "message", "Инструкция по сбросу пароля была отправлена на ваш email")
                catch
                  case _: AddressException =>
                    errors.rejectValue("email", null, "Incorrect email address")
                    formErrorView(form, errors)
    }

  private def formErrorView(form: LostPasswordRequest, errors: BindingResult): ModelAndView =
    val mav = new ModelAndView("lostpwd-form")
    mav.addObject("form", form)
    mav.addObject(BindingResult.MODEL_KEY_PREFIX + "form", errors)
    mav

class LostPasswordRequest:
  @BeanProperty
  var email: String = uninitialized
