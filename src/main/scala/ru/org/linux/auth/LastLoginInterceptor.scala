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

import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.user.UserDao

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class LastLoginInterceptor(userDao:UserDao) extends HandlerInterceptor {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    if (AuthUtil.isSessionAuthorized) {
      userDao.updateLastlogin(AuthUtil.getCurrentUser, false)
    }

    true
  }
}
