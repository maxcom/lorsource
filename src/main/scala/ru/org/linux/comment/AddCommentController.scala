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

package ru.org.linux.comment

import org.apache.pekko.actor.typed.ActorRef
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Encoder, Json}
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.auth.{AccessViolationException, AuthUtil, IPBlockDao, IPBlockInfo}
import ru.org.linux.csrf.CSRFNoAuto
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.topic.{TopicPermissionService, TopicPrepareService}
import ru.org.linux.user.UserService
import ru.org.linux.util.{ServletParameterException, StringUtil}

import javax.validation.Valid
import scala.jdk.CollectionConverters.*

@Controller
class AddCommentController(ipBlockDao: IPBlockDao, commentPrepareService: CommentPrepareService,
                           commentService: CommentCreateService, topicPermissionService: TopicPermissionService,
                           topicPrepareService: TopicPrepareService, searchQueueSender: SearchQueueSender,
                           @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef[RealtimeEventHub.Protocol],
                           textService: MessageTextService, userService: UserService) {

  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  /**
    * Показ формы добавления ответа на комментарий.
    */
  @RequestMapping(value = Array("/add_comment.jsp"), method = Array(RequestMethod.GET))
  def showFormReply(@ModelAttribute("add") @Valid add: CommentRequest, errors: Errors): ModelAndView = MaybeAuthorized { implicit currentUser =>
    if (add.getTopic == null)
      throw new ServletParameterException("тема не задана")

    topicPermissionService.checkCommentsAllowed(add.getTopic, errors)

    val postscore = topicPermissionService.getPostscore(add.getTopic)

    new ModelAndView("add_comment", (commentService.prepareReplyto(add, add.getTopic) + (
      "postscoreInfo" -> TopicPermissionService.getPostScoreInfo(postscore)
    )).asJava)
  }

  /**
    * Показ топика с формой добавления комментария верхнего уровня.
    */
  @RequestMapping(path = Array("/comment-message.jsp"))
  def showFormTopic(@ModelAttribute("add") @Valid add: CommentRequest): ModelAndView = MaybeAuthorized { implicit currentUser =>
    val preparedTopic = topicPrepareService.prepareTopic(add.getTopic)

    if (!topicPermissionService.isCommentsAllowed(preparedTopic.group, add.getTopic))
      throw new AccessViolationException("Это сообщение нельзя комментировать")

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
                 @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = MaybeAuthorized { /* no implicit! */ sessionUserOpt =>
    val postingUser = AuthUtil.postingUser(sessionUserOpt, Option(add.getNick), Option(add.getPassword), errors)
    val user = postingUser.userOpt.getOrElse(userService.getAnonymous)

    commentService.checkPostData(add, user, ipBlockInfo, request, errors, editMode = false,
      sessionAuthorized = sessionUserOpt.authorized)

    val comment = commentService.getComment(add, user, request)

    if (add.getTopic != null) {
      topicPermissionService.checkCommentsAllowed(add.getTopic, errors)(postingUser)
    }

    if (textService.isEmpty(MessageText.apply(add.getMsg, sessionUserOpt.profile.formatMode))) {
      errors.rejectValue("msg", null, "комментарий не может быть пустым")
    }

    val msg = commentService.getCommentBody(add, user, errors, sessionUserOpt.profile.formatMode)

    if (add.isPreviewMode || errors.hasErrors || comment == null) {
      val info = if (add.getTopic != null) {
        val postscore = topicPermissionService.getPostscore(add.getTopic)

        Map("postscoreInfo" -> TopicPermissionService.getPostScoreInfo(postscore),
          "comment" -> commentPrepareService.prepareCommentForEdit(comment, msg))
      } else {
        Map.empty
      }

      add.setMsg(StringUtil.escapeForceHtml(add.getMsg))

      new ModelAndView("add_comment", (commentService.prepareReplyto(add, add.getTopic)(sessionUserOpt) ++ info).asJava)
    } else {
      val (msgid, mentions) = commentService.create(user, comment, msg, remoteAddress = request.getRemoteAddr,
        xForwardedFor = Option(request.getHeader("X-Forwarded-For")), userAgent = Option(request.getHeader("user-agent")))

      searchQueueSender.updateComment(msgid)
      realtimeHubWS ! RealtimeEventHub.NewComment(comment.topicId, msgid)
      realtimeHubWS ! RealtimeEventHub.RefreshEvents(mentions)

      new ModelAndView(new RedirectView(add.getTopic.getLink + "?cid=" + msgid, false, false))
    }
  }

  @RequestMapping(value = Array("/add_comment_ajax"), produces = Array("application/json; charset=UTF-8"),
    method = Array(RequestMethod.POST))
  @ResponseBody
  def addCommentAjax(@ModelAttribute("add") @Valid add: CommentRequest, errors: Errors, request: HttpServletRequest,
                     @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): Json = MaybeAuthorized { /* no implicit! */ sessionUserOpt =>
    val postingUser = AuthUtil.postingUser(sessionUserOpt, Option(add.getNick), Option(add.getPassword), errors)
    val user = postingUser.userOpt.getOrElse(userService.getAnonymous)

    commentService.checkPostData(add, user, ipBlockInfo, request, errors, editMode = false,
      sessionAuthorized = sessionUserOpt.authorized)

    val msg = commentService.getCommentBody(add, user, errors, sessionUserOpt.profile.formatMode)
    val comment = commentService.getComment(add, user, request)

    if (add.getTopic != null) {
      topicPermissionService.checkCommentsAllowed(add.getTopic, errors)(postingUser)
    }

    if (add.isPreviewMode || errors.hasErrors || comment == null) {
      val errorsList = errors.getAllErrors.asScala.map(_.getDefaultMessage).toSeq

      if (comment != null) {
        val preparedComment = commentPrepareService.prepareCommentForEdit(comment, msg)

        CommentPreview(errorsList, Some(preparedComment.processedMessage)).asJson
      } else {
        CommentPreview(errorsList, None).asJson
      }
    } else {
      val (msgid, mentions) = commentService.create(user, comment, msg, remoteAddress = request.getRemoteAddr,
        xForwardedFor = Option(request.getHeader("X-Forwarded-For")), userAgent = Option(request.getHeader("user-agent")))

      searchQueueSender.updateComment(msgid)

      realtimeHubWS ! RealtimeEventHub.NewComment(comment.topicId, msgid)
      realtimeHubWS ! RealtimeEventHub.RefreshEvents(mentions)

      Map("url" -> (add.getTopic.getLink + "?cid=" + msgid)).asJson
    }
  }

  @InitBinder(Array("add"))
  def requestValidator(binder: WebDataBinder): Unit = commentService.requestValidator(binder)

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = commentService.initBinder(binder)
}

case class CommentPreview(errors: Seq[String], preview: Option[String])

object CommentPreview {
  implicit val encoder: Encoder[CommentPreview] = deriveEncoder[CommentPreview]
}