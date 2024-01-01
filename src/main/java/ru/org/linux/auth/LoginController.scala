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
package ru.org.linux.auth

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Encoder, Json}
import io.circe.syntax.*
import org.springframework.security.authentication.{AccountStatusException, AuthenticationManager, BadCredentialsException, LockedException, UsernamePasswordAuthenticationToken}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.{UserDetailsService, UsernameNotFoundException}
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.user.UserDao

import java.util.concurrent.CompletionStage
import javax.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}
import scala.compat.java8.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.concurrent.duration.*
import scala.util.{Random, Try}

case class LoginStatus(success: Boolean, username: String)

object LoginStatus {
  implicit val encoder: Encoder[LoginStatus] = Encoder.forProduct1("loggedIn")(_.success)
}

@Controller
class LoginController(userDao: UserDao, userDetailsService: UserDetailsService,
                      rememberMeServices: GenerationBasedTokenRememberMeServices,
                      authenticationManager: AuthenticationManager, actorSystem: ActorSystem) extends StrictLogging {
  @RequestMapping(value = Array("/login_process"), method = Array(RequestMethod.POST))
  def loginProcess(@RequestParam("nick") username: String, @RequestParam("passwd") password: String,
                   request: HttpServletRequest, response: HttpServletResponse): CompletionStage[ModelAndView] = {
    val token = new UsernamePasswordAuthenticationToken(username, password)
    try {
      val details = userDetailsService.loadUserByUsername(username).asInstanceOf[UserDetailsImpl]
      token.setDetails(details)
      val auth = authenticationManager.authenticate(token)
      val userDetails = auth.getDetails.asInstanceOf[UserDetailsImpl]

      if (!userDetails.getUser.isActivated) {
        delayResponse { new ModelAndView(new RedirectView("/login.jsp?error=not_activated")) }
      } else {
        SecurityContextHolder.getContext.setAuthentication(auth)
        rememberMeServices.loginSuccess(request, response, auth)

        delayResponse {
          AuthUtil.updateLastLogin(auth, userDao)
          new ModelAndView(new RedirectView("/"))
        }
      }
    } catch {
      case e@(_: AccountStatusException | _: BadCredentialsException | _: UsernameNotFoundException) =>
        logger.warn("Login of " + username + " failed; remote IP: " + request.getRemoteAddr + "; " + e.toString)

        request.setAttribute("enableAjaxLogin", false)

        delayResponse {
          new ModelAndView(new RedirectView("/login.jsp?error=true"))
        }
    }
  }

  @RequestMapping(value = Array("/ajax_login_process"), method = Array(RequestMethod.POST))
  @ResponseBody
  def loginAjax(@RequestParam("nick") username: String, @RequestParam("passwd") password: String,
                request: HttpServletRequest, response: HttpServletResponse): CompletionStage[Json] = {
    val token = new UsernamePasswordAuthenticationToken(username, password)
    try {
      val details = userDetailsService.loadUserByUsername(username).asInstanceOf[UserDetailsImpl]
      token.setDetails(details)
      val auth = authenticationManager.authenticate(token)
      val userDetails = auth.getDetails.asInstanceOf[UserDetailsImpl]

      if (!userDetails.getUser.isActivated) {
        delayResponse { LoginStatus(success = false, "User not activated").asJson }
      } else {
        SecurityContextHolder.getContext.setAuthentication(auth)
        rememberMeServices.loginSuccess(request, response, auth)

        delayResponse {
          AuthUtil.updateLastLogin(auth, userDao)
          LoginStatus(auth.isAuthenticated, auth.getName).asJson
        }
      }
    } catch {
      case e@(_: LockedException | _: BadCredentialsException | _: UsernameNotFoundException) =>
        logger.warn("Login of " + username + " failed; remote IP: " + request.getRemoteAddr + "; " + e.toString)
        delayResponse { LoginStatus(success = false, "Bad credentials").asJson }
    }
  }

  @RequestMapping(value = Array("/logout"), method = Array(RequestMethod.POST))
  def logout(request: HttpServletRequest, response: HttpServletResponse): ModelAndView = {
    val auth = SecurityContextHolder.getContext.getAuthentication
    if (auth != null) new SecurityContextLogoutHandler().logout(request, response, auth)
    val cookie = new Cookie("remember_me", null)
    cookie.setMaxAge(0)
    cookie.setPath("/")
    response.addCookie(cookie)
    new ModelAndView(new RedirectView("/login.jsp"))
  }

  @RequestMapping(value = Array("/logout_all_sessions"), method = Array(RequestMethod.POST))
  def logoutAllDevices(request: HttpServletRequest, response: HttpServletResponse): ModelAndView = {
    if (AuthUtil.isSessionAuthorized) userDao.unloginAllSessions(AuthUtil.getCurrentUser)
    logout(request, response)
  }

  @RequestMapping(value = Array("/logout", "/logout_all_sessions"), method = Array(RequestMethod.GET))
  def logoutLink: ModelAndView =
    if (AuthUtil.isSessionAuthorized)
      new ModelAndView(new RedirectView("/people/" + AuthUtil.getNick + "/profile"))
    else
      new ModelAndView(new RedirectView("/login.jsp"))

  @RequestMapping(value = Array("/login.jsp"), method = Array(RequestMethod.GET))
  def loginForm(request: HttpServletRequest): ModelAndView = {
    request.setAttribute("enableAjaxLogin", false)

    new ModelAndView("login-form")
  }

  private def delayResponse[T](resp : => T): CompletionStage[T] = {
    val r = Random.nextInt(2000) + 1000 // 1 to 3 seconds

    val p = Promise[T]()

    actorSystem.scheduler.scheduleOnce(r.millis) {
      p.complete(Try(resp))
    }

    p.future.toJava
  }
}