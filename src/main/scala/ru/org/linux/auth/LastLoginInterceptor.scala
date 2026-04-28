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

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.user.UserService

class LastLoginInterceptor(userService: UserService) extends HandlerInterceptor {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    if (AuthUtil.isSessionAuthorized) {
      userService.updateLastLogin(AuthUtil.getCurrentUser, force = false)
    }

    true
  }
}
