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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.common.DeleteReasons
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.site.{BadParameterException, ScriptErrorException}
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.user.{IgnoreListDao, UserErrorException, UserService}

import java.util
import scala.collection.Seq
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

@Controller
class DeleteCommentController(searchQueueSender: SearchQueueSender, commentService: CommentReadService,
                              topicDao: TopicDao, prepareService: CommentPrepareService,
                              permissionService: TopicPermissionService, commentDeleteService: DeleteService,
                              deleteInfoDao: DeleteInfoDao, ignoreListDao: IgnoreListDao,
                              userService: UserService) extends StrictLogging {
  @ModelAttribute("deleteReasons")
  def deleteReasons: util.List[String] = DeleteReasons.DeleteReasons.asJava

  @RequestMapping(value = Array("/delete_comment.jsp"), method = Array(RequestMethod.GET))
  def showForm(@RequestParam("msgid") msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val comment = commentService.getById(msgid)
    if (comment.deleted) {
      throw new UserErrorException("комментарий уже удален")
    }

    val topic = topicDao.getById(comment.topicId)
    if (topic.deleted) {
      throw new AccessViolationException("тема удалена")
    }

    val haveAnswers = commentService.hasAnswers(comment)

    if (!permissionService.isCommentDeletableNow(comment, topic, haveAnswers)) {
      throw new UserErrorException("комментарий нельзя удалить")
    }

    val comments = commentService.getCommentList(topic, currentUser.moderator)
    val list = commentService.getCommentsSubtree(comments, msgid, Set.empty[Int])

    val ignoreList = ignoreListDao.get(currentUser.user.getId)

    new ModelAndView("delete_comment", Map[String, Any](
      "msgid" -> msgid,
      "comments" -> comments,
      "topic" -> topic,
      "commentsPrepared" ->
        prepareService.prepareCommentList(comments, list, topic, Set.empty[Int], ignoreList, filterShow = false).asJava
    ).asJava)
  }

  private def findNextComment(comment: Comment): Option[Comment] = {
    val updatedTopic = topicDao.getById(comment.topicId)
    val commentList = commentService.getCommentList(updatedTopic, showDeleted = false).comments

    commentList.find(_.id >= comment.id)
  }

  @RequestMapping(value = Array("/delete_comment.jsp"), method = Array(RequestMethod.POST))
  def deleteComments(@RequestParam("msgid") msgid: Int, @RequestParam("reason") reason: String,
                     @RequestParam(value = "bonus", defaultValue = "0") bonus: Int,
                     @RequestParam(value = "delete_replys", defaultValue = "false") deleteReplys: Boolean): ModelAndView = AuthorizedOnly { implicit currentUser =>
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("неправильный размер штрафа")
    }

    val comment = commentService.getById(msgid)
    if (comment.deleted) {
      throw new UserErrorException("комментарий уже удален")
    }

    val topic = topicDao.getById(comment.topicId)
    val haveAnswers = commentService.hasAnswers(comment)
    if (!permissionService.isCommentDeletableNow(comment, topic, haveAnswers)) {
      throw new UserErrorException("комментарий нельзя удалить")
    }

    val user = currentUser.user

    val deleted: Seq[Int] = if (currentUser.moderator) {
      val effectiveBonus = if (user.getId != comment.userid) {
        bonus
      } else {
        0
      }

      if (deleteReplys) {
        commentDeleteService.deleteCommentWithReplys(topic, comment, reason, effectiveBonus)
      } else {
        if (commentDeleteService.deleteComment(comment, reason, effectiveBonus, checkForReply = false)) {
          Seq(msgid)
        } else {
          Seq.empty
        }
      }
    } else {
      if (commentDeleteService.deleteComment(comment, reason, 0, checkForReply = true)) {
        Seq(msgid)
      } else {
        Seq.empty
      }
    }

    val nextComment = findNextComment(comment)
    searchQueueSender.updateComment(deleted.map(Integer.valueOf).asJava)

    val nextLink = nextComment match {
      case Some(c) =>
        s"${topic.getLink}?cid=${c.id}"
      case None =>
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

    val author = userService.getUserCached(comment.userid)

    if (currentUser.moderator && currentUser.user != author) {
      logger.info("Comment deleted by moderator {}: {}; {}",
        currentUser.user.getNick, message, bigMessage.map(_._2).getOrElse("<none>"))

      new ModelAndView("comment-deleted-by-moderator", (Map[String, Any](
        "message" -> message,
        "link" -> nextLink,
        "author" -> author,
        "ip" -> comment.postIP,
        "ua" -> comment.userAgentId
      ) ++ bigMessage).asJava)
    } else {
      logger.info("Comment deleted by author {}: {}", currentUser.user.getNick, message)

      new ModelAndView("action-done", (Map(
        "message" -> message,
        "link" -> nextLink
      ) ++ bigMessage).asJava)
    }
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

  @RequestMapping(value = Array("/undelete_comment"), method = Array(RequestMethod.GET))
  def showUndeleteForm(@RequestParam("msgid") msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val comment = commentService.getById(msgid)

    val topic = topicDao.getById(comment.topicId)

    val deleteInfo = deleteInfoDao.getDeleteInfo(msgid).toScala

    if (!permissionService.isUndeletable(topic, comment, deleteInfo)) {
      throw new AccessViolationException("этот комментарий нельзя восстановить")
    }

    val ignoreList = ignoreListDao.get(currentUser.user.getId)

    new ModelAndView("undelete_comment", Map[String, Any](
      "comment" -> prepareService.prepareCommentOnly(comment, topic, ignoreList),
      "topic" -> topic
    ).asJava)
  }

  @RequestMapping(value = Array("/undelete_comment"), method = Array(RequestMethod.POST))
  def undelete(@RequestParam("msgid") msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val comment = commentService.getById(msgid)
    val topic = topicDao.getById(comment.topicId)
    val deleteInfo = deleteInfoDao.getDeleteInfo(msgid).toScala

    if (!permissionService.isUndeletable(topic, comment, deleteInfo)) {
      throw new AccessViolationException("этот комментарий нельзя восстановить")
    }

    commentDeleteService.undeleteComment(comment)

    searchQueueSender.updateComment(msgid)

    logger.info(s"Восстановлен комментарий пользователем ${currentUser.user}: ${topic.getLink + "?cid=" + msgid}")

    new ModelAndView(new RedirectView(topic.getLink + "?cid=" + msgid))
  }
}