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

package ru.org.linux.comment

import java.util
import java.util.Optional
import akka.actor.ActorRef

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.{AccessViolationException, AuthUtil, IPBlockDao, IPBlockInfo}
import ru.org.linux.csrf.CSRFNoAuto
import ru.org.linux.markup.{MarkupPermissions, MarkupType, MessageTextService}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.site.Template
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.topic.{TopicPermissionService, TopicPrepareService}
import ru.org.linux.util.{ServletParameterException, StringUtil}

import scala.jdk.CollectionConverters._

@Controller
class AddCommentController(ipBlockDao: IPBlockDao, commentPrepareService: CommentPrepareService,
                           commentService: CommentCreateService, topicPermissionService: TopicPermissionService,
                           topicPrepareService: TopicPrepareService, searchQueueSender: SearchQueueSender,
                           @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef, textService: MessageTextService) {

  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @ModelAttribute("modes")
  def getModes(request: HttpServletRequest): util.Map[String, String] = {
    val tmpl = Template.getTemplate(request)

    MessageTextService.postingModeSelector(AuthUtil.getCurrentUser, tmpl.getFormatMode)
  }

  /**
    * Показ формы добавления ответа на комментарий.
    */
  @RequestMapping(value = Array("/add_comment.jsp"), method = Array(RequestMethod.GET))
  def showFormReply(@ModelAttribute("add") @Valid add: CommentRequest, errors: Errors,
                    request: HttpServletRequest): ModelAndView = {
    if (add.getTopic == null)
      throw new ServletParameterException("тема не задана")

    val tmpl = Template.getTemplate(request)

    if (add.getMode == null) {
      add.setMode(tmpl.getFormatMode)
    }

    topicPermissionService.checkCommentsAllowed(add.getTopic, AuthUtil.getCurrentUser, errors)

    val postscore = topicPermissionService.getPostscore(add.getTopic)

    new ModelAndView("add_comment", (commentService.prepareReplyto(add, AuthUtil.getCurrentUser, tmpl.getProf, add.getTopic).asScala + (
      "postscoreInfo" -> TopicPermissionService.getPostScoreInfo(postscore)
    )).asJava)
  }

  /**
    * Показ топика с формой добавления комментария верхнего уровня.
    */
  @RequestMapping(Array("/comment-message.jsp"))
  def showFormTopic(@ModelAttribute("add") @Valid add: CommentRequest, request: HttpServletRequest): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    val preparedTopic = topicPrepareService.prepareTopic(add.getTopic, AuthUtil.getCurrentUser)

    if (!topicPermissionService.isCommentsAllowed(preparedTopic.getGroup, add.getTopic, AuthUtil.getCurrentUser))
      throw new AccessViolationException("Это сообщение нельзя комментировать")

    if (add.getMode == null) {
      add.setMode(tmpl.getFormatMode)
    }

    new ModelAndView("comment-message", "preparedMessage", preparedTopic)
  }

  /**
    * Добавление комментария.
    *
    * @param add     WEB-форма, содержащая данные
    * @param errors  обработчик ошибок ввода для формы
    * @param request данные запроса от web-клиента
    * @return объект web-модели
    */
  @RequestMapping(value = Array("/add_comment.jsp"), method = Array(RequestMethod.POST))
  @CSRFNoAuto
  def addComment(@ModelAttribute("add") @Valid add: CommentRequest, errors: Errors, request: HttpServletRequest,
                 @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = {
    val user = commentService.getCommentUser(add, errors)
    commentService.checkPostData(add, user, ipBlockInfo, request, errors, false)

    val comment = commentService.getComment(add, user, request)

    if (add.getTopic != null) {
      topicPermissionService.checkCommentsAllowed(add.getTopic, user, errors)
    }

    val tmpl = Template.getTemplate(request)

    if (!MarkupPermissions.allowedFormats(AuthUtil.getCurrentUser).map(_.formId).contains(add.getMode)) {
      errors.rejectValue("mode", null, "Некорректный режим разметки")
      add.setMode(MarkupType.Lorcode.formId)
    }

    if (textService.isEmpty(MessageText.apply(add.getMsg, MarkupType.ofFormId(add.getMode)))) {
      errors.rejectValue("msg", null, "комментарий не может быть пустым")
    }

    val msg = commentService.getCommentBody(add, user, errors)

    if (add.isPreviewMode || errors.hasErrors || comment == null) {
      val info = if (add.getTopic != null) {
        val postscore = topicPermissionService.getPostscore(add.getTopic)

        Map("postscoreInfo" -> TopicPermissionService.getPostScoreInfo(postscore),
          "comment" -> commentPrepareService.prepareCommentForEdit(comment, msg))
      } else {
        Map.empty
      }

      add.setMsg(StringUtil.escapeForceHtml(add.getMsg))

      new ModelAndView("add_comment", (commentService.prepareReplyto(add, AuthUtil.getCurrentUser, tmpl.getProf, add.getTopic).asScala ++ info).asJava)
    } else {
      val (msgid, mentions) = commentService.create(user, comment, msg, request.getRemoteAddr, request.getHeader("X-Forwarded-For"),
        Optional.ofNullable(request.getHeader("user-agent")))

      searchQueueSender.updateComment(msgid)
      realtimeHubWS ! RealtimeEventHub.NewComment(comment.getTopicId, msgid)
      realtimeHubWS ! RealtimeEventHub.RefreshEvents(mentions.asScala.map(_.toInt).toSet)

      new ModelAndView(new RedirectView(add.getTopic.getLink + "?cid=" + msgid))
    }
  }

  @RequestMapping(value = Array("/add_comment_ajax"), produces = Array("application/json; charset=UTF-8"),
    method = Array(RequestMethod.POST))
  @ResponseBody
  def addCommentAjax(@ModelAttribute("add") @Valid add: CommentRequest, errors: Errors, request: HttpServletRequest,
                     @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): util.Map[String, AnyRef] = {
    val user = commentService.getCommentUser(add, errors)

    commentService.checkPostData(add, user, ipBlockInfo, request, errors, false)

    val msg = commentService.getCommentBody(add, user, errors)
    val comment = commentService.getComment(add, user, request)

    if (add.getTopic != null) {
      topicPermissionService.checkCommentsAllowed(add.getTopic, user, errors)
    }

    (if (add.isPreviewMode || errors.hasErrors || comment == null) {
      val errorsList = errors.getAllErrors.asScala.map(_.getDefaultMessage)

      if (comment != null) {
        Map("errors" -> errorsList.asJava,
          "preview" -> commentPrepareService.prepareCommentForEdit(comment, msg))
      } else {
        Map("errors" -> errorsList.asJava)
      }
    } else {
      val (msgid, mentions) = commentService.create(user, comment, msg, request.getRemoteAddr, request.getHeader("X-Forwarded-For"),
        Optional.ofNullable(request.getHeader("user-agent")))

      searchQueueSender.updateComment(msgid)

      realtimeHubWS ! RealtimeEventHub.NewComment(comment.getTopicId, msgid)
      realtimeHubWS ! RealtimeEventHub.RefreshEvents(mentions.asScala.map(_.toInt).toSet)

      Map("url" -> (add.getTopic.getLink + "?cid=" + msgid))
    }).asJava
  }

  @InitBinder(Array("add"))
  def requestValidator(binder: WebDataBinder): Unit = commentService.requestValidator(binder)

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = commentService.initBinder(binder)
}
