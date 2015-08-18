package ru.org.linux.auth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import ru.org.linux.gallery.ImageDao
import ru.org.linux.group.GroupDao
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.User

import scala.collection.JavaConverters._

class GalleryPermissionInterceptor @Autowired() (imageDao:ImageDao, topicDao:TopicDao, groupDao:GroupDao,
                                                 topicPermissionService:TopicPermissionService)
  extends HandlerInterceptorAdapter with StrictLogging {

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: scala.Any): Boolean = {
    val uri = request.getRequestURI.drop(1)

    val continue = if (uri.startsWith("gallery/")) {
      logger.debug(s"Checking ${request.getRequestURI}")

      val topics = imageDao.imageByFile(uri).asScala.map { image ⇒ topicDao.getById(image.getTopicId) }

      if (topics.nonEmpty) {
        topics.exists(visible(AuthUtil.getCurrentUser))
      } else {
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
      case ex:MessageNotFoundException ⇒
        logger.info(s"topic ${topic.getId} non-visible: ${ex.getMessage}")
        false
      case ex:AccessViolationException ⇒
        logger.info(s"topic ${topic.getId} non-visible: ${ex.getMessage}")
        false
    }
  }
}
