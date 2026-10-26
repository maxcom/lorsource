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

import com.google.common.base.Strings
import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.validation.Errors
import org.springframework.web.util.WebUtils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object CSRFProtectionService extends StrictLogging:
  val CSRF_COOKIE = "CSRF_TOKEN"
  val CSRF_INPUT_NAME = "csrf"

  /** Check if user is authorized for request
    *
    * @param request
    *   request
    * @return
    *   true when ok, false when not authorized
    */
  def checkCSRF(request: HttpServletRequest): Boolean =
    val cookie = WebUtils.getCookie(request, CSRF_COOKIE)
    val cookieValue =
      if cookie != null then
        cookie.getValue
      else
        null

    if Strings.isNullOrEmpty(cookieValue) then
      logger.info("Missing CSRF cookie")
      false
    else
      val inputValue = request.getParameter(CSRF_INPUT_NAME)

      if Strings.isNullOrEmpty(inputValue) then
        logger.info("Missing CSRF input")
        false
      else
        val r = MessageDigest.isEqual(
          cookieValue.trim.getBytes(StandardCharsets.UTF_8),
          inputValue.trim.getBytes(StandardCharsets.UTF_8))

        if !r then
          logger.info(
            s"CSRF cookie does not match: cookie.len=${cookieValue.length} param.len=${inputValue.length}) " +
              s"ip=${request.getRemoteAddr} url=${request.getRequestURI}")

        r

  def checkCSRF(request: HttpServletRequest, errors: Errors): Unit =
    if !checkCSRF(request) then
      errors.reject(null, "сбой добавления, попробуйте еще раз")
