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
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, CorrectorOrModerator}
import ru.org.linux.auth.{AccessViolationException, CurrentUser}
import ru.org.linux.comment.{Comment, CommentPrepareService, CommentReadService}
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.site.{MessageNotFoundException, Template}
import ru.org.linux.topic.{Topic, TopicDao, TopicLinkBuilder, TopicPermissionService, TopicPrepareService}
import ru.org.linux.user.UserService
import ru.org.linux.warning.WarningService.MaxWarningsPerHour
import ru.org.linux.warning.WarningType.idToType

import java.beans.PropertyEditorSupport
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.SeqHasAsJava

class PostWarningRequest(@BeanProperty var topic: Topic, @BeanProperty var comment: Comment,
                         @BeanProperty var text: String, @BeanProperty var warningType: WarningType)

@Controller
class WarningController(warningService: WarningService, topicDao: TopicDao, commentReadService: CommentReadService,
                        topicPermissionService: TopicPermissionService, groupDao: GroupDao, userService: UserService,
                        topicPrepareService: TopicPrepareService, commentPrepareService: CommentPrepareService) {
  @RequestMapping(value = Array("/post-warning"), method = Array(RequestMethod.GET))
  def showForm(@ModelAttribute(value = "request") request: PostWarningRequest,
               errors: Errors): ModelAndView = AuthorizedOnly { currentUser =>
    val group = groupDao.getGroup(request.topic.groupId)

    checkRequest(group, request, errors, currentUser)

    val mv = new ModelAndView("post-warning")

    mv.addObject("warningTypes", warningTypes(request, group).asJava)
    prepareView(request, currentUser, mv)

    mv
  }

  private def warningTypes(request: PostWarningRequest, group: Group) = {
    if (request.comment != null) {
      Seq(RuleWarning)
    } else {
      if (group.isPremoderated) {
        Seq(RuleWarning, SpellingWarning, TagsWarning, GroupWarning)
      } else {
        Seq(RuleWarning, TagsWarning, GroupWarning)
      }
    }
  }

  private def prepareView(request: PostWarningRequest, currentUser: CurrentUser, mv: ModelAndView): Unit = {
    if (request.comment == null) {
      val preparedTopic = topicPrepareService.prepareTopic(request.getTopic, currentUser.user)
      mv.addObject("preparedTopic", preparedTopic)
    } else {
      val tmpl = Template.getTemplate

      val preparedComment = commentPrepareService.prepareCommentOnly(request.comment, Some(currentUser), tmpl.getProf,
        request.topic, Set.empty)

      mv.addObject("preparedComment", preparedComment)
    }
  }

  @RequestMapping(value = Array("/post-warning"), method = Array(RequestMethod.POST))
  def post(@ModelAttribute(value = "request") request: PostWarningRequest,
           errors: Errors): ModelAndView = AuthorizedOnly { currentUser =>
    val group = groupDao.getGroup(request.topic.groupId)

    checkRequest(group, request, errors, currentUser)

    val types = warningTypes(request, group)

    if (request.warningType == null || !types.contains(request.warningType)) {
      errors.reject(null, "Не выбран тип уведомления")
    }

    if (request.text == null || request.text.trim.isEmpty) {
      errors.reject(null, "Сообщение не может быть пустым")
    }

    if (request.text !=null && request.text.length > 256) { // sync with post-warning.jsp
      errors.reject(null, "Сообщение не может быть более 256 символов")
    }

    if (errors.hasErrors) {
      val mv = new ModelAndView("post-warning")

      mv.addObject("warningTypes", types.asJava)
      prepareView(request, currentUser, mv)

      mv
    } else {
      warningService.postWarning(request.topic, Option(request.comment), currentUser.user, request.text, request.warningType)

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

  private def checkRequest(group: Group, request: PostWarningRequest, errors: Errors, currentUser: CurrentUser): Unit = {
    assert(request.topic.groupId == group.id)
    assert(request.comment == null || request.comment.topicId == request.topic.id)

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

    if (request.comment != null && request.comment.deleted) {
      errors.reject(null, "Комментарий удален")
    }

    if (!errors.hasErrors && warningService.lastWarningsCount(currentUser) >= MaxWarningsPerHour) {
      errors.reject(null, s"Вы не можете отправить более ${MaxWarningsPerHour} уведомлений в час")
    }
  }

  @RequestMapping(value = Array("/clear-warning"), method = Array(RequestMethod.POST))
  def clear(@RequestParam(value = "id") id: Int): ModelAndView = CorrectorOrModerator { currentUser =>
    val warning = warningService.get(id)
    val topic = topicDao.getById(warning.topicId)

    warningService.clear(warning, currentUser)

    val builder = TopicLinkBuilder.baseLink(topic)

    val link = warning.commentId.map { commentId =>
      builder.comment(commentId)
    }.getOrElse(builder).build()

    new ModelAndView(new RedirectView(link))
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

    binder.registerCustomEditor(classOf[WarningType], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(text: String): Unit = {
        if (text.isEmpty) {
          setValue(null)
        } else {
          setValue(idToType.getOrElse(text, new IllegalArgumentException("unknown type")))
        }
      }

      override def getAsText: String = Option(this.getValue).map(_.asInstanceOf[WarningType].id).orNull
    })
  }
}