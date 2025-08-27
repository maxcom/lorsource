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

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import jakarta.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.filter.GenericFilterBean
import ru.org.linux.auth.CommonContextFilter.{BadTimezones, Russian}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.site.Template
import ru.org.linux.spring.SiteConfig

import java.util.Locale

object CommonContextFilter {
  private val Russian = Locale.forLanguageTag("ru")
  private val BadTimezones: Set[String] = Set("Factory", "Etc/Unknown")
}

class CommonContextFilter extends GenericFilterBean with InitializingBean {
  override def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    val ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext)
    val request = req.asInstanceOf[HttpServletRequest]
    val response = res.asInstanceOf[HttpServletResponse]
    val currentUser = AuthUtil.getCurrentUser

    val cookies = getCookies(request)
    val timezoneName = cookies.get("tz").filter(_.nonEmpty).filterNot(BadTimezones.contains)

    val timezone = (try {
      timezoneName.map(DateTimeZone.forID)
    } catch {
      case ex: IllegalArgumentException =>
        logger.info(s"Wrong timezone: $timezoneName (${ex.toString})")
        None
    }).getOrElse(DateTimeZone.getDefault)

    request.setAttribute("timezone", timezone)

    request.setAttribute("configuration", ctx.getBean(classOf[SiteConfig]))
    request.setAttribute("template", new Template)
    request.setAttribute("currentUser", currentUser)
    request.setAttribute("enableAjaxLogin", currentUser==null)

    request.setCharacterEncoding("utf-8")
    res.setLocale(Russian)

    csrfManipulation(request, response)

    response.addHeader("Cache-Control", "private")

    chain.doFilter(req, res)
  }

  private def csrfManipulation(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val cookies = getCookies(request)

    cookies.get(CSRFProtectionService.CSRF_COOKIE) match {
      case None =>
        CSRFProtectionService.generateCSRFCookie(request, response)
      case Some(value) =>
        request.setAttribute(CSRFProtectionService.CSRF_ATTRIBUTE, value.trim)
    }
  }

  private def getCookies(request: HttpServletRequest): Map[String, String] = {
    Option(request.getCookies).map { cookies =>
      cookies.view.map(c => c.getName -> c.getValue).toMap
    }.getOrElse(Map.empty)
  }
}
