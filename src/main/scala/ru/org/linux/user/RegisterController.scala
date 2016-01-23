/*
 * Copyright 1998-2016 Linux.org.ru
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

import javax.mail.internet.InternetAddress
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import javax.validation.Valid

import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.security.authentication.{AuthenticationManager, BadCredentialsException, UsernamePasswordAuthenticationToken}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth._
import ru.org.linux.email.EmailService
import ru.org.linux.site.Template
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.{ExceptionBindingErrorProcessor, LorHttpUtils}

@Controller
class RegisterController @Autowired() (captcha: CaptchaService, ipBlockDao: IPBlockDao,
                                       rememberMeServices: RememberMeServices,
                                       @Qualifier("authenticationManager") authenticationManager: AuthenticationManager,
                                       userDetailsService: UserDetailsServiceImpl,
                                       userDao: UserDao,
                                       emailService: EmailService,
                                       siteConfig: SiteConfig
                                      ) extends StrictLogging {
  @RequestMapping(value = Array("/register.jsp"), method = Array(RequestMethod.GET))
  def register(@ModelAttribute("form") form: RegisterRequest, response: HttpServletResponse): ModelAndView = {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
    new ModelAndView("register")
  }

  @RequestMapping(value = Array("/register.jsp"), method = Array(RequestMethod.POST))
  def doRegister(request: HttpServletRequest, @Valid @ModelAttribute("form") form: RegisterRequest,
                 errors: Errors): ModelAndView = {
    val session: HttpSession = request.getSession

    if (!errors.hasErrors) {
      captcha.checkCaptcha(request, errors)

      val tmpl = Template.getTemplate(request)

      ipBlockDao.checkBlockIP(request.getRemoteAddr, errors, tmpl.getCurrentUser)

      if (userDao.isUserExists(form.getNick)) {
        errors.rejectValue("nick", null, "пользователь " + form.getNick + " уже существует")
      }

      if (userDao.getByEmail(new InternetAddress(form.getEmail).getAddress.toLowerCase, false) != null) {
        errors.rejectValue("email", null, "пользователь с таким e-mail уже зарегистрирован. " +
          "Если вы забыли параметры своего аккаунта, воспользуйтесь формой восстановления пароля")
      }
    }

    if (!errors.hasErrors) {
      val mail = new InternetAddress(form.getEmail.toLowerCase)
      val userid = userDao.createUser("", form.getNick, form.getPassword, "", mail, "", request.getRemoteAddr)

      logger.info(s"Зарегистрирован пользователь ${form.getNick} (id=$userid) ${LorHttpUtils.getRequestIP(request)}")

      emailService.sendEmail(form.getNick, mail.getAddress, isNew = true)

      new ModelAndView(
        "action-done",
        "message",
        "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.")
    } else {
      new ModelAndView("register")
    }
  }

  @RequestMapping(value = Array("/activate.jsp"), method = Array(RequestMethod.GET))
  def activateForm: ModelAndView = new ModelAndView("activate")

  @RequestMapping(value = Array("/activate.jsp"), method = Array(RequestMethod.POST), params = Array("action"))
  @throws(classOf[Exception])
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

        if (regcode == activation) {
          userDao.activateUser(userDetails.getUser)

          val updatedDetails = userDetailsService.loadUserByUsername(nick).asInstanceOf[UserDetailsImpl]
          token.setDetails(updatedDetails)
          val updatedAuth = authenticationManager.authenticate(token)

          SecurityContextHolder.getContext.setAuthentication(updatedAuth)
          rememberMeServices.loginSuccess(request, response, updatedAuth)
          AuthUtil.updateLastLogin(updatedAuth, userDao)

          new ModelAndView(new RedirectView("/"))
        } else {
          new ModelAndView("activate", "error", "Неправильный код активации")
        }
      } else {
        new ModelAndView(new RedirectView("/"))
      }
    } catch {
      case e: UsernameNotFoundException ⇒
        new ModelAndView("activate", "error", "Пользователь не найден")
      case e: BadCredentialsException ⇒
        new ModelAndView("activate", "error", "Неправильный логин или пароль")
    }
  }

  @RequestMapping(value = Array("/activate.jsp"), method = Array(RequestMethod.POST), params = Array("!action"))
  def activate(request: HttpServletRequest, @RequestParam activation: String): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("Not authorized!")
    }

    val user = tmpl.getCurrentUser
    val newEmail = userDao.getNewEmail(user)

    if (newEmail == null) {
      throw new AccessViolationException("new_email == null?!")
    }

    val regcode = user.getActivationCode(siteConfig.getSecret, newEmail)

    if (regcode != activation) {
      throw new AccessViolationException("Bad activation code")
    }

    userDao.acceptNewEmail(user, newEmail)

    new ModelAndView(new RedirectView("/people/" + user.getNick + "/profile"))
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder):Unit = {
    binder.setValidator(new RegisterRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }
}