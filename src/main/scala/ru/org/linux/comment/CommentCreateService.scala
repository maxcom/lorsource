/*
 * Copyright 1998-2025 Linux.org.ru
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

import com.google.common.base.Preconditions
import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import ru.org.linux.auth.*
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.edithistory.{EditHistoryObjectTypeEnum, EditHistoryRecord, EditHistoryService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.*
import ru.org.linux.util.ExceptionBindingErrorProcessor

import java.beans.PropertyEditorSupport
import scala.collection.mutable
import scala.jdk.OptionConverters.RichOption

object CommentCreateService {
  /**
   * Формирование строки в лог-файл.
   *
   * @param message       сообщение
   * @param remoteAddress IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor IP-адрес через шлюз, с которого был добавлен комментарий
   * @return строка, готовая для добавления в лог-файл
   */
  private def makeLogString(message: String, remoteAddress: String, xForwardedFor: Option[String]) = {
    val logMessage = new StringBuilder()

    logMessage.append(message).append("; ip: ").append(remoteAddress)

    xForwardedFor.foreach { xForwardedFor =>
      logMessage.append(" XFF:").append(xForwardedFor)
    }

    logMessage.toString
  }
}

@Service
class CommentCreateService(commentDao: CommentDao, topicDao: TopicDao, userService: UserService,
                           captcha: CaptchaService, commentPrepareService: CommentPrepareService,
                           floodProtector: FloodProtector, editHistoryService: EditHistoryService,
                           textService: MessageTextService, userEventService: UserEventService, msgbaseDao: MsgbaseDao,
                           ignoreListDao: IgnoreListDao, permissionService: TopicPermissionService,
                           val transactionManager: PlatformTransactionManager)
    extends StrictLogging  with TransactionManagement {
  def requestValidator(binder: WebDataBinder): Unit = {
    binder.setValidator(new CommentRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }

  def initBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[Topic], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(text: String): Unit = {
        try {
          setValue(topicDao.getById(text.split(",")(0).toInt))
        } catch {
          case e: MessageNotFoundException =>
            throw new IllegalArgumentException(e)
        }
      }
    })

    binder.registerCustomEditor(classOf[Comment], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(text: String): Unit = {
        if (text.isEmpty || "0" == text) {
          setValue(null)
        } else {
          try {
            setValue(commentDao.getById(text.toInt))
          } catch {
            case e: MessageNotFoundException =>
              throw new IllegalArgumentException(e)
          }
        }
      }
    })

    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))
  }

  /**
   * Проверка валидности данных запроса.
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param user           пользователь, добавляющий или изменяющий комментарий
   * @param ipBlockInfo    информация о банах
   * @param request        данные запроса от web-клиента
   * @param errors         обработчик ошибок ввода для формы
   */
  def checkPostData(commentRequest: CommentRequest, user: User, ipBlockInfo: IPBlockInfo, request: HttpServletRequest,
                    errors: Errors, editMode: Boolean, sessionAuthorized: Boolean): Unit = {
    if (commentRequest.getMsg == null) {
      errors.rejectValue("msg", null, "комментарий не задан")
      commentRequest.setMsg("")
    }

    if (!commentRequest.isPreviewMode && (!sessionAuthorized || ipBlockInfo.isCaptchaRequired)) {
      captcha.checkCaptcha(request, errors)
    }

    if (!commentRequest.isPreviewMode && !errors.hasErrors) {
      CSRFProtectionService.checkCSRF(request, errors)
    }

    user.checkBlocked(errors)
    user.checkFrozen(errors)
    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user)

    if (!commentRequest.isPreviewMode && !errors.hasErrors && !editMode) {
      floodProtector.checkRateLimit(FloodProtector.AddComment, request.getRemoteAddr, user, errors)
    }
  }

  /**
   * Получить текст комментария.
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param user           пользователь, добавляющий или изменяющий комментарий
   * @param errors         обработчик ошибок ввода для формы
   * @return текст комментария
   */
  def getCommentBody(commentRequest: CommentRequest, user: User, errors: Errors, mode: MarkupType): MessageText = {
    val messageText = MessageText(commentRequest.getMsg, mode)

    val maxLength = if (user.isAnonymous) 4096 else 8192

    if (messageText.text.length > maxLength) {
      errors.rejectValue("msg", null, "Слишком большое сообщение")
    }

    messageText
  }

  /**
   * Получить объект комментария из WEB-запроса.
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param user           пользователь, добавляющий или изменяющий комментарий
   * @param request        данные запроса от web-клиента
   * @return объект комментария из WEB-запроса
   */
  def getComment(commentRequest: CommentRequest, user: User, request: HttpServletRequest): Comment = {
    if (commentRequest.getTopic != null) {
      val replyto = Option(commentRequest.getReplyto).map(_.id)

      val commentId = if (commentRequest.getOriginal == null) {
        0
      } else {
        commentRequest.getOriginal.id
      }

      Comment.buildNew(replyto, commentRequest.getTopic.id, commentId, user.getId, request.getRemoteAddr)
    } else {
      null
    }
  }

  @throws[UserNotFoundException]
  def prepareReplyto(add: CommentRequest, topic: Topic)(implicit currentUser: AnySession): Map[String, AnyRef] = {
    if (add.getReplyto != null) {
      val ignoreList = currentUser.opt.map(user => ignoreListDao.get(user.user.getId)).getOrElse(Set.empty)

      val preparedComment = commentPrepareService.prepareCommentOnly(add.getReplyto, topic, ignoreList)

      Map("onComment" -> preparedComment)
    } else {
      Map.empty
    }
  }

  /**
   * Создание нового комментария.
   *
   * @param comment       объект комментария
   * @param commentBody   текст комментария
   * @param remoteAddress IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor IP-адрес через шлюз, с которого был добавлен комментарий
   * @param userAgent     заголовок User-Agent запроса
   * @return идентификационный номер нового комментария + список пользователей у которых появились события
   */
  @throws[MessageNotFoundException]
  def create(author: User, comment: Comment, commentBody: MessageText, remoteAddress: String,
             xForwardedFor: Option[String], userAgent: Option[String]): (Int, Set[Int]) = transactional() { _ =>
    Preconditions.checkArgument(comment.userid == author.getId)

    val notifyUsers = Set.newBuilder[Int]

    val commentId = commentDao.saveNewMessage(comment, userAgent.toJava)

    msgbaseDao.saveNewMessage(commentBody, commentId)

    if (permissionService.isUserCastAllowed(author)) {
      val mentions = notifyMentions(author, comment, commentBody, commentId)
      notifyUsers.addAll(mentions.map(_.getId))
    }

    val parentCommentOpt: Option[Comment] = if (comment.replyTo != 0) {
      val parentComment = commentDao.getById(comment.replyTo)
      val mention = notifyReply(comment, commentId, parentComment)

      mention.foreach(user => notifyUsers.addOne(user.getId))

      Some(parentComment)
    } else {
      None
    }

    val commentNotified = userEventService.insertCommentWatchNotification(comment, parentCommentOpt, commentId)

    notifyUsers.addAll(commentNotified)

    logger.info(CommentCreateService.makeLogString("Написан комментарий " + commentId, remoteAddress, xForwardedFor))

    (commentId, notifyUsers.result())
  }

  /* оповещение об ответе на коммент */
  private def notifyReply(comment: Comment, commentId: Int, parentComment: Comment): Option[User] = {
    val notifyUser = if (parentComment.userid != comment.userid) {
      Some(userService.getUserCached(parentComment.userid))
        .filterNot(_.isAnonymous)
        .filterNot { parentAuthor =>
          ignoreListDao.get(parentAuthor.getId).contains(comment.userid)
        }
    } else {
      None
    }

    notifyUser.foreach { parentAuthor =>
      userEventService.addReplyEvent(parentAuthor, comment.topicId, commentId)
    }

    notifyUser
  }

  /* кастование пользователей */
  private def notifyMentions(author: User, comment: Comment, commentBody: MessageText, commentId: Int) = {
    val userRefs = textService.mentions(commentBody).filter((p: User) => !userService.isIgnoring(p.getId, author.getId))

    userEventService.addUserRefEvent(userRefs, comment.topicId, commentId)

    userRefs
  }

  /**
   * Редактирование комментария.
   *
   * @param oldComment    данные старого комментария
   * @param newComment    данные нового комментария
   * @param commentBody   текст нового комментария
   * @param remoteAddress IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor IP-адрес через шлюз, с которого был добавлен комментарий
   */
  def edit(oldComment: Comment, newComment: Comment, commentBody: String, remoteAddress: String,
           xForwardedFor: Option[String], editor: User, originalMessageText: MessageText): Unit = transactional() { _ =>
    commentDao.changeTitle(oldComment, newComment.title)
    msgbaseDao.updateMessage(oldComment.id, commentBody)

    /* кастование пользователей */
    val newUserRefs = textService.mentions(MessageText.apply(commentBody, originalMessageText.markup))
    val messageText = msgbaseDao.getMessageText(oldComment.id)
    val oldUserRefs = textService.mentions(messageText)
    val userRefs = mutable.Set[User]()

    /* кастовать только тех, кто добавился. Существующие ранее не кастуются */
    for (user <- newUserRefs) {
      if (!oldUserRefs.contains(user)) {
        userRefs.add(user)
      }
    }

    if (permissionService.isUserCastAllowed(editor)) {
      userEventService.addUserRefEvent(userRefs, oldComment.topicId, oldComment.id)
    }

    /* Обновление времени последнего изменения топика для того, чтобы данные в кеше автоматически обновились  */
    topicDao.updateLastmod(oldComment.topicId, false)

    addEditHistoryItem(editor, oldComment, originalMessageText.text, newComment, commentBody)

    updateLatestEditorInfo(editor, oldComment, newComment)

    logger.info(CommentCreateService.makeLogString("Изменён комментарий " + oldComment.id, remoteAddress, xForwardedFor))
  }

  /**
   * Добавить элемент истории для комментария.
   *
   * @param editor              пользователь, изменивший комментарий
   * @param original            оригинал (старый комментарий)
   * @param originalMessageText старое содержимое комментария
   * @param comment             изменённый комментарий
   * @param messageText         новое содержимое комментария
   */
  private def addEditHistoryItem(editor: User, original: Comment, originalMessageText: String, comment: Comment,
                                 messageText: String): Unit = {
    val editHistoryRecord = EditHistoryRecord(
      msgid = original.id,
      objectType = EditHistoryObjectTypeEnum.COMMENT,
      editor = editor.getId,
      oldtitle = Some(original.title).filterNot(_ == comment.title),
      oldmessage = Some(originalMessageText).filterNot(_ == messageText))

    if (editHistoryRecord.oldtitle.isDefined || editHistoryRecord.oldmessage.isDefined) {
      editHistoryService.insert(editHistoryRecord)
    }
  }

  /**
   * Обновление информации о последнем изменении коммента.
   *
   * @param editor   пользователь, изменивший комментарий
   * @param original оригинал (старый комментарий)
   * @param comment  изменённый комментарий
   */
  private def updateLatestEditorInfo(editor: User, original: Comment, comment: Comment): Unit = {
    val editCount = editHistoryService.editCount(original.id, EditHistoryObjectTypeEnum.COMMENT)

    commentDao.updateLatestEditorInfo(original.id, editor.getId, comment.postdate, editCount)
  }
}