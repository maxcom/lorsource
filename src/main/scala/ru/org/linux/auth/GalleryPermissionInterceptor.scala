/*
 * Copyright 1998-2022 Linux.org.ru
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
import org.springframework.web.servlet.HandlerInterceptor
import ru.org.linux.gallery.ImageDao
import ru.org.linux.group.GroupDao
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.User

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class GalleryPermissionInterceptor(imageDao: ImageDao, topicDao: TopicDao, groupDao: GroupDao,
                                   topicPermissionService: TopicPermissionService)
  extends HandlerInterceptor with StrictLogging {

  private val ImagesPattern = "^images/(\\d+)/.*".r

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: scala.Any): Boolean = {
    val uri = request.getRequestURI.drop(1)

    val continue = if (uri.startsWith("gallery/preview/")) {
      AuthUtil.isSessionAuthorized
    } else if (uri.startsWith("images/")) {
      logger.debug(s"Checking ${request.getRequestURI}")

      uri match {
        case ImagesPattern(id) =>
          val topics = Option(imageDao.getImage(id.toInt)).map { image => topicDao.getById(image.getTopicId) }

          if (topics.nonEmpty) {
            topics.exists(visible(AuthUtil.getCurrentUser))
          } else {
            true
          }
        case other =>
          logger.info(s"Strange URI in images: $other")
          true
      }
    } else {
      true
    }

    if (!continue) {
      response.sendError(403)
    }

    continue
  }

  private def visible(currentUser:User)(topic:Topic):Boolean = {
    try {
      topicPermissionService.checkView(
        groupDao.getGroup(topic.getGroupId),
        topic,
        currentUser,
        false
      )
      true
    } catch {
      case ex: MessageNotFoundException =>
        logger.info(s"topic ${topic.getId} non-visible: ${ex.getMessage}")
        false
      case ex: AccessViolationException =>
        logger.info(s"topic ${topic.getId} non-visible: ${ex.getMessage}")
        false
    }
  }
}
