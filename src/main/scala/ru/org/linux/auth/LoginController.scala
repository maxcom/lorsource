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
package ru.org.linux.auth

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}
import org.apache.pekko.actor.ActorSystem
import org.springframework.security.authentication.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.{UserDetailsService, UsernameNotFoundException}
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.{ModelAttribute, RequestAttribute, RequestMapping, RequestMethod,
  RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.user.{UserDao, UserService}

import java.util.concurrent.CompletionStage
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.FutureConverters.FutureOps
import scala.util.{Random, Try}

@Controller
class LoginController(
    userDao: UserDao,
    userDetailsService: UserDetailsService,
    userService: UserService,
    rememberMeServices: GenerationBasedTokenRememberMeServices,
    captchaService: CaptchaService,
    loginAttemptCache: LoginAttemptCache,
    authenticationManager: AuthenticationManager,
    actorSystem: ActorSystem)
    extends StrictLogging:

  @RequestMapping(value = Array("/login_process"), method = Array(RequestMethod.POST))
  def loginProcess(
      @ModelAttribute("loginForm")
      form: LoginForm,
      bindingResult: BindingResult,
      request: HttpServletRequest,
      response: HttpServletResponse): CompletionStage[ModelAndView] =
    MaybeAuthorized { session =>
      if session.authorized then
        Future.successful(new ModelAndView(new RedirectView(safeRedirectUrl(form.redirectUrl)))).asJava
      else
        val requireCaptcha =
          session.ipBlockInfo.captchaRequired || loginAttemptCache.requireCaptchaForIp(request.getRemoteAddr) ||
            loginAttemptCache.requireCaptchaForUser(form.nick)

        if requireCaptcha then
          captchaService.checkCaptcha(request, bindingResult)
          if bindingResult.hasErrors then
            return delayResponse(loginErrorView(form, bindingResult, requireCaptcha))

        try
          val auth = authenticate(form.nick, form.passwd)
          val userDetails = auth.getDetails.asInstanceOf[UserDetailsImpl]

          if !userDetails.getUser.activated then
            loginAttemptCache.recordFailedAttempt(request.getRemoteAddr, form.nick)
            bindingResult.reject(
              "login.not_activated",
              "Регистрация не завершена! Инструкция по активации отправлена на указанный при регистрации email.")
            // captcha is required after the first failed attempt (see LoginAttemptCache)
            return delayResponse(loginErrorView(form, bindingResult, requireCaptcha = true))

          SecurityContextHolder.getContext.setAuthentication(auth)
          rememberMeServices.loginSuccess(request, response, auth)

          delayResponse {
            AuthUtil.updateLastLogin(auth, userService)
            new ModelAndView(new RedirectView(safeRedirectUrl(form.redirectUrl)))
          }
        catch
          case e: LockedException =>
            logger.warn("Login of {} failed; remote IP: {}; {}", form.nick, request.getRemoteAddr, e.toString)
            delayResponse {
              new ModelAndView(new RedirectView(s"/people/${form.nick}/profile"))
            }
          case e @ (_: AccountStatusException | _: BadCredentialsException | _: UsernameNotFoundException) =>
            logger.warn("Login of {} failed; remote IP: {}; {}", form.nick, request.getRemoteAddr, e.toString)
            loginAttemptCache.recordFailedAttempt(request.getRemoteAddr, form.nick)
            bindingResult.reject(
              "login.failed",
              "Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.")
            // captcha is required after the first failed attempt (see LoginAttemptCache)
            delayResponse(loginErrorView(form, bindingResult, requireCaptcha = true))
    }

  @RequestMapping(value = Array("/logout"), method = Array(RequestMethod.POST))
  def logout(request: HttpServletRequest, response: HttpServletResponse): ModelAndView =
    val auth = SecurityContextHolder.getContext.getAuthentication
    if auth != null then
      new SecurityContextLogoutHandler().logout(request, response, auth)
    val cookie = new Cookie("remember_me", null)
    cookie.setMaxAge(0)
    cookie.setPath("/")
    response.addCookie(cookie)
    new ModelAndView(new RedirectView("/login.jsp"))

  @RequestMapping(value = Array("/logout_all_sessions"), method = Array(RequestMethod.POST))
  def logoutAllDevices(request: HttpServletRequest, response: HttpServletResponse): ModelAndView =
    if AuthUtil.isSessionAuthorized then
      userDao.unloginAllSessions(AuthUtil.getCurrentUser)
    logout(request, response)

  @RequestMapping(value = Array("/logout", "/logout_all_sessions"), method = Array(RequestMethod.GET))
  def logoutLink: ModelAndView =
    if AuthUtil.isSessionAuthorized then
      new ModelAndView(new RedirectView("/people/" + AuthUtil.getNick + "/profile"))
    else
      new ModelAndView(new RedirectView("/login.jsp"))

  @RequestMapping(value = Array("/login.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def loginForm(
      @RequestParam(value = "from", required = false)
      from: String,
      request: HttpServletRequest): ModelAndView =
    MaybeAuthorized { session =>
      val form = new LoginForm()
      form.redirectUrl = safeRedirectUrl(Option(from).getOrElse(""))

      if session.authorized then
        new ModelAndView(new RedirectView(safeRedirectUrl(form.redirectUrl)))
      else
        val requireCaptcha =
          session.ipBlockInfo.captchaRequired || loginAttemptCache.requireCaptchaForIp(request.getRemoteAddr)
        new ModelAndView(
          "login-form",
          Map("loginForm" -> form, "requireCaptcha" -> requireCaptcha.asInstanceOf[AnyRef]).asJava)
    }

  private def loginErrorView(form: LoginForm, bindingResult: BindingResult, requireCaptcha: Boolean): ModelAndView =
    val mav = new ModelAndView("login-form")
    mav.addObject("loginForm", form)
    mav.addObject(BindingResult.MODEL_KEY_PREFIX + "loginForm", bindingResult)
    mav.addObject("requireCaptcha", requireCaptcha)
    mav

  // Authenticates the user and returns an Authentication whose principal reflects the *current*
  // stored password. DaoAuthenticationProvider may upgrade the password hash (legacy jasypt ->
  // bcrypt) during authentication, but the Authentication it returns still carries the pre-upgrade
  // principal. Since the app is stateless and relies on the password-signed remember-me cookie, the
  // cookie must be signed with the upgraded hash; otherwise it would be rejected on the next request.
  private def authenticate(username: String, password: String): UsernamePasswordAuthenticationToken =
    val token = new UsernamePasswordAuthenticationToken(username, password)
    token.setDetails(userDetailsService.loadUserByUsername(username).asInstanceOf[UserDetailsImpl])

    val auth = authenticationManager.authenticate(token)

    val refreshed = userDetailsService.loadUserByUsername(username).asInstanceOf[UserDetailsImpl]
    val result = UsernamePasswordAuthenticationToken.authenticated(refreshed, null, auth.getAuthorities)
    result.setDetails(refreshed)
    result

  private def safeRedirectUrl(from: String): String = LoginController.safeRedirectUrl(from)

  private def delayResponse[T](resp: => T): CompletionStage[T] =
    val r = Random.nextInt(2000) + 1000

    val p = Promise[T]()

    actorSystem
      .scheduler
      .scheduleOnce(r.millis) {
        p.complete(Try(resp))
      }

    p.future.asJava

object LoginController:
  // Sanitizes the post-login redirect target. `from` is attacker-controllable via the `from`
  // query parameter, so only same-site relative paths are allowed; anything else falls back to "/".
  def safeRedirectUrl(from: String): String =
    if isLocalRedirect(from) then
      from
    else
      "/"

  // Accepts only same-site relative paths: "/" or "/path...".
  // Rejects "//host" and "/\host" (protocol-relative variants that browsers normalize to off-site
  // absolute URLs), and anything not starting with "/".
  private def isLocalRedirect(url: String): Boolean =
    url != null && url.nonEmpty && url.charAt(0) == '/' &&
      (url.length == 1 || (url.charAt(1) != '/' && url.charAt(1) != '\\'))
