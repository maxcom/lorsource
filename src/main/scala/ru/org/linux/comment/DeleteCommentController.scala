/*
 * Copyright 1998-2017 Linux.org.ru
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

import javax.servlet.http.HttpServletRequest

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.site.{BadParameterException, ScriptErrorException, Template}
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.user.UserErrorException

import scala.collection.JavaConverters._

@Controller
class DeleteCommentController(searchQueueSender: SearchQueueSender, commentService: CommentService,
                              topicDao: TopicDao, prepareService: CommentPrepareService,
                              permissionService: TopicPermissionService,
                              commentDeleteService: CommentDeleteService) {
  @RequestMapping(value = Array("/delete_comment.jsp"), method = Array(RequestMethod.GET))
  def showForm(request: HttpServletRequest, @RequestParam("msgid") msgid: Int): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("нет авторизации")
    }

    val comment = commentService.getById(msgid)
    if (comment.isDeleted) {
      throw new UserErrorException("комментарий уже удален")
    }

    val topic = topicDao.getById(comment.getTopicId)
    if (topic.isDeleted) {
      throw new AccessViolationException("тема удалена")
    }

    val comments = commentService.getCommentList(topic, tmpl.isModeratorSession)
    val cv = new CommentFilter(comments)
    val list = cv.getCommentsSubtree(msgid)

    new ModelAndView("delete_comment", Map[String, Any](
      "msgid" -> msgid,
      "comments" -> comments,
      "topic" -> topic,
      "commentsPrepared" -> prepareService.prepareCommentList(comments, list, request.isSecure, tmpl, topic)
    ).asJava)
  }

  private def findNextComment(comment: Comment): Option[Comment] = {
    val updatedTopic = topicDao.getById(comment.getTopicId)
    val commentList = commentService.getCommentList(updatedTopic, false)

    commentList.getList.asScala.find(_.getId >= comment.getId)
  }

  @RequestMapping(value = Array("/delete_comment.jsp"), method = Array(RequestMethod.POST))
  def deleteComments(@RequestParam("msgid") msgid: Int, @RequestParam("reason") reason: String,
                     @RequestParam(value = "bonus", defaultValue = "0") bonus: Int,
                     @RequestParam(value = "delete_replys", defaultValue = "false") deleteReplys: Boolean,
                     request: HttpServletRequest): ModelAndView = {
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("неправильный размер штрафа")
    }

    val tmpl = Template.getTemplate(request)
    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("нет авторизации")
    }

    val user = tmpl.getCurrentUser
    user.checkBlocked()
    user.checkAnonymous()

    val comment = commentService.getById(msgid)
    if (comment.isDeleted) {
      throw new UserErrorException("комментарий уже удален")
    }

    val topic = topicDao.getById(comment.getTopicId)
    val haveAnswers = commentService.isHaveAnswers(comment)
    if (!permissionService.isCommentDeletableNow(comment, user, topic, haveAnswers)) {
      throw new UserErrorException("комментарий нельзя удалить")
    }

    val deleted: Seq[Integer] = if (user.isModerator) {
      val effectiveBonus = if (user.getId!=comment.getId) {
        bonus
      } else {
        0
      }

      if (deleteReplys) {
        commentDeleteService.deleteWithReplys(topic, comment, reason, user, effectiveBonus).asScala
      } else {
        if (commentDeleteService.deleteComment(msgid, reason, user, effectiveBonus, false)) {
          Seq(msgid)
        } else {
          Seq.empty
        }
      }
    } else {
      if (commentDeleteService.deleteComment(msgid, reason, user, 0, true)) {
        Seq(msgid)
      } else {
        Seq.empty
      }
    }

    val nextComment = findNextComment(comment)
    searchQueueSender.updateComment(deleted.asJava)

    val nextLink = nextComment match {
      case Some(c) ⇒
        topic.getLink + "?cid=" + c.getId
      case None ⇒
        topic.getLink
    }

    val message = if (deleted.nonEmpty) {
      "Удалено успешно"
    } else {
      "Сообщение уже удалено"
    }

    val bigMessage = if (deleted.nonEmpty) {
      Some("bigMessage" -> s"Удаленные комментарии: ${deleted.mkString(", ")}")
    } else {
      None
    }

    new ModelAndView("action-done", (Map(
      "message" -> message,
      "link" -> nextLink
    ) ++ bigMessage).asJava)
  }

  @ExceptionHandler(Array(classOf[ScriptErrorException], classOf[UserErrorException], classOf[AccessViolationException]))
  @ResponseStatus(HttpStatus.FORBIDDEN)
  def handleUserNotFound(ex: Exception): ModelAndView = {
    new ModelAndView("errors/good-penguin", Map(
      "msgTitle" -> s"Ошибка: ${ex.getMessage}",
      "msgHeader" -> ex.getMessage,
      "msgMessage" -> ""
    ).asJava)
  }
}
