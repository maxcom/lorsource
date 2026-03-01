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
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.*
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.email.EmailService
import ru.org.linux.util.ExceptionBindingErrorProcessor
import ru.org.linux.util.StringUtil
import ru.org.linux.util.URLUtil

import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.validation.Valid

@Controller
@RequestMapping(Array("/people/{nick}/edit"))
class EditRegisterController(rememberMeServices: RememberMeServices, authenticationManager: AuthenticationManager,
                             userDetailsService: UserDetailsServiceImpl, ipBlockDao: IPBlockDao, userDao: UserDao,
                             userService: UserService, emailService: EmailService, emailDomainsBlockDao: EmailDomainsBlockDao,
                             userPermissionService: UserPermissionService)
    extends StrictLogging {
  private val validator = new EditRegisterRequestValidator(emailDomainsBlockDao)

  @RequestMapping(method = Array(RequestMethod.GET))
  def show(@ModelAttribute("form") form: EditRegisterRequest, @PathVariable("nick") nick: String,
           response: HttpServletResponse): ModelAndView = AuthorizedOnly { implicit currentUser =>
    if (currentUser.user.nick != nick) {
      throw new AccessViolationException("Not authorized")
    }

    val user = currentUser.user

    val userInfo = userDao.getUserInfoClass(user)

    val mv = new ModelAndView("edit-reg")

    mv.getModel.put("canLoadUserpic", userPermissionService.canLoadUserpic)
    mv.getModel.put("canEditInfo", userPermissionService.canEditProfileInfo)

    form.setEmail(user.email)
    form.setUrl(userInfo.url)
    form.setTown(userInfo.town)
    form.setName(user.getName)
    form.setInfo(userDao.getUserInfo(user))

    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")

    mv
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def edit(request: HttpServletRequest, response: HttpServletResponse, @PathVariable("nick") nick: String,
           @Valid @ModelAttribute("form") form: EditRegisterRequest,
           errors: Errors): ModelAndView = AuthorizedOnly { implicit currentUser =>
    if (currentUser.user.nick != nick) {
      throw new AccessViolationException("Not authorized")
    }

    val newPassword = Option(Strings.emptyToNull(form.getPassword))

    if (newPassword.exists(_.equalsIgnoreCase(nick))) {
      errors.reject(null, "пароль не может совпадать с логином")
    }

    val mail = Option(form.getEmail).filter(_.nonEmpty).flatMap { mail =>
      try {
        Some(new InternetAddress(mail))
      } catch {
        case e: AddressException =>
          errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage)
          None
      }
    }

    val url = Option(form.getUrl).filter(_.nonEmpty).map(URLUtil.fixURL).orNull
    val name = Option(form.getName).filter(_.nonEmpty).map(StringUtil.escapeHtml).orNull
    val town = Option(form.getTown).filter(_.nonEmpty).map(StringUtil.escapeHtml).orNull
    val info = Option(form.getInfo).filter(_.nonEmpty).orNull

    val ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)
    UserPermissionService.checkBlockIP(ipBlockInfo, errors, currentUser.user)

    val user = currentUser.user

    if (Strings.isNullOrEmpty(form.getOldpass)) {
      errors.rejectValue("oldpass", null, "Для изменения регистрации нужен ваш пароль")
    } else if (!UserService.matchPassword(user, form.getOldpass)) {
      errors.rejectValue("oldpass", null, "Неверный пароль")
    }

    val newEmail = mail.flatMap { mail =>
      if (user.email != null && user.email == mail.getAddress.toLowerCase) {
        None
      } else {
        if (userDao.getByEmail(mail.getAddress.toLowerCase, false) != null) {
          errors.rejectValue("email", null, "такой email уже используется")
        }

        Some(mail.getAddress.toLowerCase)
      }
    }

    if (!errors.hasErrors) {
      if (userPermissionService.canEditProfileInfo) {
        userService.updateUser(user, name, url, newEmail, town, newPassword, info, request.getRemoteAddr)
      } else {
        userService.updateEmailPasswd(user, newEmail, newPassword, request.getRemoteAddr)
      }

      // Обновление token-а аутентификации после смены пароля
      newPassword.foreach { newPassword =>
        updateAuthToken(request, response, user, newPassword)
      }

      newEmail match {
        case Some(newEmail) =>
          emailService.sendRegistrationEmail(user.nick, newEmail, isNew = false)

          val msg = s"Обновление регистрации прошло успешно. " +
            s"Ожидайте письма на ${StringUtil.escapeHtml(newEmail)} с кодом активации смены email."

          new ModelAndView("action-done", "message", msg)
        case None =>
          new ModelAndView(new RedirectView("/people/" + user.nick + "/profile"))
      }
    } else {
      val mv = new ModelAndView("edit-reg")

      mv.getModel.put("canLoadUserpic", userPermissionService.canLoadUserpic)
      mv.getModel.put("canEditInfo", userPermissionService.canEditProfileInfo)

      mv
    }
  }

  private def updateAuthToken(request: HttpServletRequest, response: HttpServletResponse, user: User, newPassword: String): Unit = {
    try {
      val token = new UsernamePasswordAuthenticationToken(user.nick, newPassword)
      val details = userDetailsService.loadUserByUsername(user.nick)

      token.setDetails(details)

      val auth = authenticationManager.authenticate(token)

      SecurityContextHolder.getContext.setAuthentication(auth)

      rememberMeServices.loginSuccess(request, response, auth)
    } catch {
      case ex: Exception =>
        logger.error("В этом месте не должно быть исключительных ситуаций. ", ex)
    }
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit = {
    binder.setValidator(validator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }
}