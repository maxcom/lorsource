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

package ru.org.linux.csrf

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.csrf.CsrfException
import ru.org.linux.auth.AccessViolationException

class CsrfAccessDeniedHandler extends AccessDeniedHandler:
  override def handle(
      request: HttpServletRequest,
      response: HttpServletResponse,
      accessDeniedException: AccessDeniedException): Unit =
    if !response.isCommitted then
      val message =
        accessDeniedException match
          case _: CsrfException =>
            CsrfAccessDeniedHandler.CsrfMessage
          case _ =>
            CsrfAccessDeniedHandler.DeniedMessage

      request.setAttribute("exception", AccessViolationException(message))
      response.sendError(HttpServletResponse.SC_FORBIDDEN)

  end handle

object CsrfAccessDeniedHandler:
  private val CsrfMessage = "Неправильный код защиты CSRF. Возможно сессия устарела"
  private val DeniedMessage = "Доступ запрещен"
