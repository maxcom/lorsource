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
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.jasypt.util.text.AES256TextEncryptor
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ResourceLoader
import org.springframework.security.authentication.{AuthenticationManager, BadCredentialsException, UsernamePasswordAuthenticationToken}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.auth.*
import ru.org.linux.email.EmailService
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.{ExceptionBindingErrorProcessor, LorHttpUtils, StringUtil}

import javax.mail.internet.InternetAddress
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.validation.Valid
import scala.jdk.CollectionConverters.*

@Controller
class RegisterController(captcha: CaptchaService, rememberMeServices: RememberMeServices,
                         @Qualifier("authenticationManager") authenticationManager: AuthenticationManager,
                         userDetailsService: UserDetailsServiceImpl, userDao: UserDao, emailService: EmailService,
                         siteConfig: SiteConfig, userService: UserService, invitesDao: UserInvitesDao,
                         resourceLoader: ResourceLoader) extends StrictLogging {
  private val registerRequestValidator = new RegisterRequestValidator(resourceLoader)

  @RequestMapping(value = Array("/register.jsp"), method = Array(RequestMethod.GET))
  def register(@ModelAttribute("form") form: RegisterRequest, response: HttpServletResponse,
               request: HttpServletRequest, @RequestParam(required = false) invite: String): ModelAndView = {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")

    if (invite!=null) {
      val emailOpt = invitesDao.emailFromValidInvite(invite)

      emailOpt match {
        case None =>
          throw new AccessViolationException("Код приглашения не действителен")
        case Some(email) =>
          form.setEmail(email)
      }
      new ModelAndView("register", "invite", invite)
    } else {
      if (userService.canRegister(request.getRemoteAddr)) {
        new ModelAndView("register", "permit", makePermit)
      } else {
        new ModelAndView("no-register")
      }
    }
  }

  private def makePermit: String = {
    val key = siteConfig.getSecret
    val message = s"permit:${DateTime.now().plusHours(1).getMillis}"

    val textEncryptor = new AES256TextEncryptor
    textEncryptor.setPassword(key)
    textEncryptor.encrypt(message)
  }

  private def checkPermit(permit: String): Boolean = {
    val key = siteConfig.getSecret
    val textEncryptor = new AES256TextEncryptor
    textEncryptor.setPassword(key)

    textEncryptor.decrypt(permit).split(":", 2) match {
      case Array("permit", date) =>
        val decodedDate = new DateTime(date.toLong)
        logger.debug(s"Decoded permit date: $decodedDate")
        decodedDate.isAfterNow
      case other =>
        logger.warn(s"Invalid permit - decrypted: $other")
        false
    }
  }

  @RequestMapping(value = Array("/register.jsp"), method = Array(RequestMethod.POST))
  def doRegister(request: HttpServletRequest, @Valid @ModelAttribute("form") form: RegisterRequest,
                 errors: Errors, @RequestParam(required = false) invite: String,
                 @RequestParam(required = false) permit: String): ModelAndView = {
    if (invite==null && permit == null) {
      return new ModelAndView("no-register")
    } else {
      if (invite!=null) {
        val emailOpt = invitesDao.emailFromValidInvite(invite)

        emailOpt match {
          case None =>
            throw new AccessViolationException("Код приглашения не действителен")
          case Some(email) =>
            form.setEmail(email)
        }
      } else if (!checkPermit(permit)) {
        return new ModelAndView("no-register")
      }
    }

    if (!errors.hasErrors) {
      if (invite==null) {
        captcha.checkCaptcha(request, errors)
      }

      if (userDao.isUserExists(form.getNick) || userDao.hasSimilarUsers(form.getNick)) {
        errors.rejectValue("nick", null, "Это имя пользователя уже используется. Пожалуйста выберите другое имя.")
      }

      if (userDao.getByEmail(new InternetAddress(form.getEmail).getAddress.toLowerCase, false) != null) {
        errors.rejectValue("email", null, "пользователь с таким e-mail уже зарегистрирован. " +
          "Если вы забыли параметры своего аккаунта, воспользуйтесь формой восстановления пароля.")
      }
    }

    if (!errors.hasErrors) {
      val mail = new InternetAddress(form.getEmail.toLowerCase)
      val userid = userService.createUser(nick = form.getNick, password = form.getPassword, mail = mail,
        ip = request.getRemoteAddr, invite = Option(invite), userAgent = Option(request.getHeader("user-agent")),
        language = Option(request.getHeader("accept-language")))

      logger.info(s"Зарегистрирован пользователь ${form.getNick} (id=$userid) ${LorHttpUtils.getRequestIP(request)}")

      emailService.sendRegistrationEmail(form.getNick, mail.getAddress, isNew = true)

      new ModelAndView(
        "action-done",
        "message",
        "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.")
    } else {
      val params = Map("invite" -> invite, "permit" -> permit)
      new ModelAndView("register", params.asJava)
    }
  }

  private def activationFormParams(nick: String, activation: String) = {
    val nickSanitized = Option(nick).filter(StringUtil.checkLoginName).orNull
    val activationSanitized = Option(activation).filter(_.forall(_.isLetterOrDigit)).orNull

    Map(
      "nick" -> nickSanitized,
      "activation" -> activationSanitized
    )
  }

  @RequestMapping(value = Array("/activate", "/activate.jsp"), method = Array(RequestMethod.GET))
  def activateForm(@RequestParam(required = false) nick: String,
                   @RequestParam(required = false) activation: String): ModelAndView = {
    new ModelAndView("activate", activationFormParams(nick, activation).asJava)
  }

  @RequestMapping(value = Array("/activate", "/activate.jsp"), method = Array(RequestMethod.POST), params = Array("action"))
  def activateNew(request: HttpServletRequest, response: HttpServletResponse,
                  @RequestParam activation: String, @RequestParam nick: String,
                  @RequestParam passwd: String): ModelAndView = {
    try {
      val details = userDetailsService.loadUserByUsername(nick).asInstanceOf[UserDetailsImpl]

      if (!details.getUser.isActivated) {
        val token = new UsernamePasswordAuthenticationToken(nick, passwd)

        token.setDetails(details)

        val auth = authenticationManager.authenticate(token)
        val userDetails = auth.getDetails.asInstanceOf[UserDetailsImpl]
        val regcode = userDetails.getUser.getActivationCode(siteConfig.getSecret)

        if (regcode.equalsIgnoreCase(activation)) {
          userDao.activateUser(userDetails.getUser)

          val updatedDetails = userDetailsService.loadUserByUsername(nick).asInstanceOf[UserDetailsImpl]
          token.setDetails(updatedDetails)
          val updatedAuth = authenticationManager.authenticate(token)

          SecurityContextHolder.getContext.setAuthentication(updatedAuth)
          rememberMeServices.loginSuccess(request, response, updatedAuth)
          AuthUtil.updateLastLogin(updatedAuth, userDao)

          new ModelAndView(new RedirectView("/"))
        } else {
          val params = activationFormParams(nick, activation) + ("error" -> "Неправильный код активации")
          new ModelAndView("activate", params.asJava)
        }
      } else {
        new ModelAndView(new RedirectView("/"))
      }
    } catch {
      case _: UsernameNotFoundException =>
        val params = activationFormParams(nick, activation) + ("error" -> "Пользователь не найден")
        new ModelAndView("activate", params.asJava)
      case _: BadCredentialsException =>
        val params = activationFormParams(nick, activation) + ("error" -> "Неправильный логин или пароль")
        new ModelAndView("activate", params.asJava)
    }
  }

  @RequestMapping(value = Array("/activate", "/activate.jsp"), method = Array(RequestMethod.POST), params = Array("!action"))
  def activate(@RequestParam activation: String): ModelAndView = AuthorizedOnly { currentUser =>
    val newEmail = userDao.getNewEmail(currentUser.user)

    if (newEmail == null) {
      throw new AccessViolationException("new_email == null?!")
    }

    val regcode = currentUser.user.getActivationCode(siteConfig.getSecret, newEmail)

    if (!regcode.equalsIgnoreCase(activation)) {
      val params = activationFormParams(currentUser.user.getNick, activation) + ("error" -> "Неправильный код активации")

      new ModelAndView("activate", params.asJava)
    } else {
      userDao.acceptNewEmail(currentUser.user, newEmail)

      new ModelAndView(new RedirectView("/people/" + currentUser.user.getNick + "/profile"))
    }
  }

  @ResponseBody
  @RequestMapping(path = Array("check-login"))
  def ajaxLoginCheck(@RequestParam nick: String): Json = {
    (if (nick.isEmpty) {
      "Не задан nick."
    } else if (!StringUtil.checkLoginName(nick)) {
      "Некорректное имя пользователя."
    } else if (nick != null && nick.length > User.MAX_NICK_LENGTH) {
      "Слишком длинное имя пользователя."
    } else if (userDao.isUserExists(nick) || userDao.hasSimilarUsers(nick)) {
      "Это имя пользователя уже используется. Пожалуйста выберите другое имя."
    } else {
      "true"
    }).asJson
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder):Unit = {
    binder.setValidator(registerRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }

  @RequestMapping(value = Array("create-invite"), method = Array(RequestMethod.GET))
  def createInviteForm(): ModelAndView = AuthorizedOnly { currentUser =>
    if (!userService.canInvite(currentUser.user)) {
      throw new AccessViolationException("Вы не можете пригласить нового пользователя")
    }

    new ModelAndView("create-invite")
  }

  @RequestMapping(value = Array("create-invite"), method = Array(RequestMethod.POST))
  def createInvite(@RequestParam email: String): ModelAndView = AuthorizedOnly { currentUser =>
    if (!userService.canInvite(currentUser.user)) {
      throw new AccessViolationException("Вы не можете пригласить нового пользователя")
    }

    if (userDao.getByEmail(email, false) != null) {
      throw new AccessViolationException("Пользователь с этим адресом уже зарегистрирован")
    }

    val parsedEmail = new InternetAddress(email)

    if (!registerRequestValidator.isGoodDomainEmail(parsedEmail)) {
      throw new AccessViolationException("Некорректный email домен")
    }

    val (token, validUntil) = invitesDao.createInvite(currentUser.user, email)

    emailService.sendInviteEmail(currentUser.user, email, token, validUntil)

    new ModelAndView("action-done", "message", s"Приглашение отправлено по адресу $email")
  }
}