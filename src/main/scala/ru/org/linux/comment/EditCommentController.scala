/*
 * Copyright 1998-2023 Linux.org.ru
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

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, AuthorizedOpt}
import ru.org.linux.auth.{AuthUtil, IPBlockDao, IPBlockInfo}
import ru.org.linux.csrf.CSRFNoAuto
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.site.Template
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.topic.TopicPermissionService
import ru.org.linux.user.IgnoreListDao
import ru.org.linux.util.ServletParameterException

import java.util
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import scala.compat.java8.OptionConverters.RichOptionForJava8

@Controller
class EditCommentController(commentService: CommentCreateService, msgbaseDao: MsgbaseDao, ipBlockDao: IPBlockDao,
                            topicPermissionService: TopicPermissionService, commentPrepareService: CommentPrepareService,
                            searchQueueSender: SearchQueueSender, textService: MessageTextService,
                            commentReadService: CommentReadService, ignoreListDao: IgnoreListDao) {
  @InitBinder(Array("edit"))
  def requestValidator(binder: WebDataBinder): Unit = commentService.requestValidator(binder)

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = commentService.initBinder(binder)

  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  /**
    * Показ формы изменения комментария.
    */
  @RequestMapping(value = Array("/edit_comment"), method = Array(RequestMethod.GET))
  def editCommentShowHandler(@ModelAttribute("add") @Valid commentRequest: CommentRequest): ModelAndView = AuthorizedOnly { currentUser =>
    val topic = commentRequest.getTopic
    if (topic == null) throw new ServletParameterException("тема не задана")

    val original = commentRequest.getOriginal
    if (original == null) throw new ServletParameterException("Комментарий не задан")

    val comment = commentRequest.getOriginal
    val tmpl = Template.getTemplate

    val messageText = msgbaseDao.getMessageText(original.id)

    val commentEditable = topicPermissionService.isCommentEditableNow(comment, AuthUtil.getCurrentUser,
      commentReadService.hasAnswers(comment), topic, messageText.markup)

    if (commentEditable) {
      commentRequest.setMsg(messageText.text)

      val formParams = new util.HashMap[String, AnyRef]

      val ignoreList = ignoreListDao.getJava(currentUser.user)

      formParams.put("comment", commentPrepareService.prepareCommentOnly(comment, currentUser.user, tmpl.getProf,
        topic, ignoreList))

      topicPermissionService.getEditDeadline(comment).foreach(value => formParams.put("deadline", value.toDate))

      new ModelAndView("edit_comment", formParams)
    } else {
      new ModelAndView(new RedirectView(topic.getLink + "?cid=" + original.id))
    }
  }

  /**
    * Изменение комментария.
    *
    * @param commentRequest WEB-форма, содержащая данные
    * @param errors         обработчик ошибок ввода для формы
    * @param request        данные запроса от web-клиента
    * @return объект web-модели
    */
  @RequestMapping(value = Array("/edit_comment"), method = Array(RequestMethod.POST))
  @CSRFNoAuto
  def editCommentPostHandler(@ModelAttribute("add") @Valid commentRequest: CommentRequest, errors: Errors,
                             request: HttpServletRequest,
                             @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = AuthorizedOnly { currentUser =>
    val user = currentUser.user
    commentService.checkPostData(commentRequest, user, ipBlockInfo, request, errors, true)

    val comment = commentService.getComment(commentRequest, user, request)
    val tmpl = Template.getTemplate

    val formParams = new util.HashMap[String, AnyRef](commentService.prepareReplyto(commentRequest, AuthUtil.getCurrentUser, tmpl.getProf, commentRequest.getTopic))

    val originalMessageText = msgbaseDao.getMessageText(commentRequest.getOriginal.id)

    commentRequest.setMode(originalMessageText.markup.formId)

    if (textService.isEmpty(MessageText.apply(commentRequest.getMsg, MarkupType.ofFormId(commentRequest.getMode)))) {
      errors.rejectValue("msg", null, "комментарий не может быть пустым")
    }

    val msg = commentService.getCommentBody(commentRequest, user, errors)

    if (commentRequest.getTopic != null) {
      val postscore = topicPermissionService.getPostscore(commentRequest.getTopic)
      formParams.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(postscore))
      topicPermissionService.checkCommentsAllowed(commentRequest.getTopic, Some(user).asJava, errors)
      formParams.put("comment", commentPrepareService.prepareCommentForEdit(comment, msg))
    }

    topicPermissionService.checkCommentsEditingAllowed(commentRequest.getOriginal, commentRequest.getTopic,
      AuthUtil.getCurrentUser, errors, originalMessageText.markup)

    if (commentRequest.isPreviewMode || errors.hasErrors || comment == null) {
      val modelAndView = new ModelAndView("edit_comment", formParams)
      modelAndView.addObject("ipBlockInfo", ipBlockInfo)
      val deadline = topicPermissionService.getEditDeadline(commentRequest.getOriginal)
      deadline.foreach(value => formParams.put("deadline", value.toDate))
      modelAndView
    } else {
      commentService.edit(commentRequest.getOriginal, comment, msg.text, request.getRemoteAddr, request.getHeader("X-Forwarded-For"), user, originalMessageText)
      searchQueueSender.updateComment(commentRequest.getOriginal.id)
      val returnUrl = "/jump-message.jsp?msgid=" + commentRequest.getTopic.id + "&cid=" + commentRequest.getOriginal.id
      new ModelAndView(new RedirectView(returnUrl))
    }
  }
}
