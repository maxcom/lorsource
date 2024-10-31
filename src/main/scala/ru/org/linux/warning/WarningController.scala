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

package ru.org.linux.warning

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.comment.{Comment, CommentReadService}
import ru.org.linux.group.GroupDao
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.{Topic, TopicDao, TopicLinkBuilder, TopicPermissionService}
import ru.org.linux.user.UserService

import java.beans.PropertyEditorSupport
import scala.beans.BeanProperty

class PostWarningRequest(@BeanProperty var topic: Topic, @BeanProperty var comment: Comment,
                         @BeanProperty var text: String)

@Controller
class WarningController(warningService: WarningService, topicDao: TopicDao, commentReadService: CommentReadService,
                        topicPermissionService: TopicPermissionService, groupDao: GroupDao, userService: UserService) {
  @RequestMapping(value = Array("/post-warning"), method = Array(RequestMethod.GET))
  def showForm(@ModelAttribute(value = "request") request: PostWarningRequest): ModelAndView = AuthUtil.AuthorizedOnly { currentUser =>
    val group = groupDao.getGroup(request.topic.groupId)
    val topicAuthor = userService.getUserCached(request.topic.authorUserId)

    topicPermissionService.checkView(group, request.topic, currentUser.user, topicAuthor, showDeleted = false)

    if (topicPermissionService.getPostscore(group, request.topic) == TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
      throw new AccessViolationException("Вы не можете отправить уведомление")
    }

    if (!topicPermissionService.canPostWarning(Some(currentUser), request.topic, Option(request.comment))) {
      throw new AccessViolationException("Вы не можете отправить уведомление")
    }

    // TODO rate limit warning
    // TODO show topic / comment

    val mv = new ModelAndView("post-warning")

    mv
  }

  @RequestMapping(value = Array("/post-warning"), method = Array(RequestMethod.POST))
  def post(@ModelAttribute(value = "request")  request: PostWarningRequest,
           errors: Errors): ModelAndView = AuthUtil.AuthorizedOnly { currentUser =>
    val group = groupDao.getGroup(request.topic.groupId)
    val topicAuthor = userService.getUserCached(request.topic.authorUserId)

    topicPermissionService.checkView(group, request.topic, currentUser.user, topicAuthor, showDeleted = false)

    if (topicPermissionService.getPostscore(group, request.topic) == TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
      throw new AccessViolationException("Вы не можете отправить уведомление")
    }

    if (!topicPermissionService.canPostWarning(Some(currentUser), request.topic, Option(request.comment))) {
      errors.reject(null, "Вы не можете отправить уведомление")
    }

    if (request.topic.deleted) {
      errors.reject(null, "Топик удален")
    }

    if (request.topic.expired) {
      errors.reject(null, "Топик перемещен в архив")
    }

    if (request.comment!=null && request.comment.deleted) {
      errors.reject(null, "Комментарий удален")
    }

    // TODO rate limit warning
    // TODO show topic / comment
    // TODO check text length

    if (errors.hasErrors) {
      val mv = new ModelAndView("post-warning")

      mv
    } else {
      warningService.postWarning(request.topic, Option(request.comment), currentUser.user, request.text)

      val builder = TopicLinkBuilder.baseLink(request.topic)

      val link = (if (request.comment != null) {
        builder.comment(request.comment.id)
      } else {
        builder
      }).build()

      val mv = new ModelAndView("action-done")

      mv.addObject("message", "Уведомление отправлено")
      mv.addObject("link", link)

      mv
    }
  }

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[Topic], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(text: String): Unit = {
        try {
          setValue(topicDao.getById(text.toInt))
        } catch {
          case e: MessageNotFoundException =>
            throw new IllegalArgumentException(e)
        }
      }

      override def getAsText: String = Option(this.getValue).map(_.asInstanceOf[Topic].id.toString).orNull
    })

    binder.registerCustomEditor(classOf[Comment], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(text: String): Unit = {
        if (text.isEmpty || "0" == text) {
          setValue(null)
        } else {
          try {
            setValue(commentReadService.getById(text.toInt))
          } catch {
            case e: MessageNotFoundException =>
              throw new IllegalArgumentException(e)
          }
        }
      }

      override def getAsText: String = Option(this.getValue).map(_.asInstanceOf[Comment].id.toString).orNull
    })
  }
}