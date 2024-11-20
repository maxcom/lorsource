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
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.gallery.{Image, ImageDao}
import ru.org.linux.group.GroupDao
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.UserService

class GalleryPermissionInterceptor(imageDao: ImageDao, topicDao: TopicDao, groupDao: GroupDao,
                                   topicPermissionService: TopicPermissionService, userService: UserService)
  extends HandlerInterceptor with StrictLogging {

  import GalleryPermissionInterceptor.*

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse,
                         handler: scala.Any): Boolean = MaybeAuthorized { implicit session =>
    val uri = request.getRequestURI.drop(1)

    val (continue, code) = if (uri.startsWith("gallery/preview/")) {
      (session.authorized, 403)
    } else if (uri.startsWith("images/")) {
      logger.debug(s"Checking ${request.getRequestURI}")

      uri match {
        case ImagesPattern(id) =>
          try {
            val image = imageDao.getImage(id.toInt)
            val topic = topicDao.getById(image.topicId)

            (visible(topic, image), 403)
          } catch {
            case _: EmptyResultDataAccessException =>
              (false, 404)
          }
        case other =>
          logger.info(s"Strange URI in images: $other")
          (false, 404)
      }
    } else {
      (true, 200)
    }

    if (!continue) {
      response.sendError(code)
    }

    continue
  }

  private def visible(topic: Topic, image: Image)(implicit session: AnySession): Boolean = {
    try {
      topicPermissionService.checkView(groupDao.getGroup(topic.groupId), topic,
        userService.getUserCached(topic.authorUserId), showDeleted = false)

      if (image.deleted) {
        topicPermissionService.canViewHistory(topic)
      } else {
        true
      }
    } catch {
      case ex: MessageNotFoundException =>
        logger.info(s"topic ${topic.id} non-visible: ${ex.getMessage}")
        false
      case ex: AccessViolationException =>
        logger.info(s"topic ${topic.id} non-visible: ${ex.getMessage}")
        false
    }
  }
}

object GalleryPermissionInterceptor {
  private val ImagesPattern = "^images/(\\d+)/.*".r
}
