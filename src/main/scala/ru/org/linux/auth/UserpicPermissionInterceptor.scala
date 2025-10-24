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

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.user.{UserNotFoundException, UserService}

class UserpicPermissionInterceptor(userService: UserService) extends HandlerInterceptor with StrictLogging {
  private val ImagesPattern = """^photos/(\d+)((?::-?\d+)?\.\w+).*""".r

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: scala.Any): Boolean = {
    val uri = request.getRequestURI.drop(1)

    val continue = uri match {
      case ImagesPattern(userid, suffix) =>
        try {
          val user = userService.getUserCached(userid.toInt)
          val image = s"$userid$suffix"

          if (image == user.getPhoto) {
            true
          } else {
            val currentUser = Option(AuthUtil.getCurrentUser)

            val allowed = currentUser.exists(u => u.getId == user.getId || u.isModerator)

            if (!allowed) {
              if (user.getPhoto!=null && user.getPhoto.nonEmpty) {
                logger.warn(s"Redirect $image for ${currentUser.map(_.getNick).getOrElse("unauthorized user")} to ${user.getPhoto}")
                response.sendRedirect(s"/photos/${user.getPhoto}")
              } else {
                logger.warn(s"Forbidden access $image for ${currentUser.map(_.getNick).getOrElse("unauthorized user")}")
                response.sendRedirect(UserService.DisabledUserpic.url)
              }
            }

            allowed
          }
        } catch {
          case _: UserNotFoundException =>
            logger.warn(s"Invalid image path $uri: user not found")
            response.sendError(404)

            false
        }
      case other =>
        logger.warn(s"Invalid images path $other forbidden")
        response.sendError(404)

        false
    }


    continue
  }
}
