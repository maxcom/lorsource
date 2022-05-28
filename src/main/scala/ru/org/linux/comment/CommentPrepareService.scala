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

import com.google.common.base.Strings
import org.joda.time.{DateTime, Duration}
import org.springframework.stereotype.Service
import ru.org.linux.markup.MessageTextService
import ru.org.linux.site.ApiDeleteInfo
import ru.org.linux.spring.dao.{DeleteInfoDao, MessageText, MsgbaseDao, UserAgentDao}
import ru.org.linux.topic.{Topic, TopicPermissionService}
import ru.org.linux.user._

import javax.annotation.Nullable
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Service
class CommentPrepareService(textService: MessageTextService, msgbaseDao: MsgbaseDao,
                            topicPermissionService: TopicPermissionService, userService: UserService,
                            deleteInfoDao: DeleteInfoDao, userAgentDao: UserAgentDao, remarkDao: RemarkDao) {

  private def prepareComment(messageText: MessageText, author: User, remark: Option[String], comment: Comment,
                             comments: Option[CommentList], profile: Profile, topic: Topic,
                             hideSet: Set[Int], samePageComments: Set[Int], @Nullable currentUser: User) = {
    val processedMessage = textService.renderCommentText(messageText, !topicPermissionService.followAuthorLinks(author))

    val (answerLink, answerSamepage, answerCount, replyInfo, hasAnswers) = if (comments.isDefined) {
      val replyInfo: Option[ReplyInfo] = if (comment.getReplyTo != 0) {
        val replyNode = comments.get.getNode(comment.getReplyTo)
        val replyDeleted = replyNode == null

        if (replyDeleted) { // ответ на удаленный комментарий
          Some(new ReplyInfo(comment.getReplyTo, true))
        } else {
          val reply = replyNode.getComment
          val samePage = samePageComments.contains(reply.getId)
          val replyAuthor = userService.getUserCached(reply.getUserid).getNick
          Some(new ReplyInfo(reply.getId, replyAuthor, Strings.emptyToNull(reply.getTitle.trim), reply.getPostdate, samePage, false))
        }
      } else {
        None
      }

      val node = comments.get.getNode(comment.getId)
      val replysFiltered = node.childs.asScala.filter(commentNode => !hideSet.contains(commentNode.getComment.getId))

      val answerCount = replysFiltered.size

      if (answerCount > 1) {
        (Some(s"${topic.getLink}/thread/${comment.getId}#comments"), false, answerCount, replyInfo, true)
      } else if (answerCount == 1) {
        val answerSamepage = samePageComments.contains(replysFiltered.head.getComment.getId)
        (Some(s"${topic.getLink}?cid=${replysFiltered.head.getComment.getId}"), answerSamepage, 1, replyInfo, true)
      } else {
        (None, false, 0, replyInfo, false)
      }
    } else {
      (None, false, 0, None, false)
    }

    val userpic: Option[Userpic] = if (profile.isShowPhotos) {
      Some(userService.getUserpic(author, profile.getAvatarMode, misteryMan = false))
    } else {
      None
    }

    val ref = userService.ref(author, currentUser)

    val deleteInfo = loadDeleteInfo(comment)
    val apiDeleteInfo = deleteInfo.map(i => new ApiDeleteInfo(userService.getUserCached(i.userid).getNick, i.getReason))
    val editSummary = loadEditSummary(comment)

    val (postIP, userAgent) = if (currentUser != null && currentUser.isModerator) {
      (Option(comment.getPostIP), userAgentDao.getUserAgentById(comment.getUserAgentId).toScala)
    } else {
      (None, None)
    }

    val undeletable = topicPermissionService.isUndeletable(topic, comment, currentUser, deleteInfo.toJava)
    val deletable = topicPermissionService.isCommentDeletableNow(comment, currentUser, topic, hasAnswers)
    val editable = topicPermissionService.isCommentEditableNow(comment, currentUser, hasAnswers, topic, messageText.markup)

    new PreparedComment(comment, ref, processedMessage, replyInfo.orNull, deletable, editable, remark.orNull,
      userpic.orNull, apiDeleteInfo.orNull, editSummary.orNull, postIP.orNull, userAgent.orNull, comment.getUserAgentId,
      undeletable, answerCount, answerLink.orNull, answerSamepage)
  }

  private def loadDeleteInfo(comment: Comment) = {
    if (comment.isDeleted) {
      deleteInfoDao.getDeleteInfo(comment.getId).toScala
    } else {
      None
    }
  }

  private def loadEditSummary(comment: Comment): Option[EditSummary] = {
    if (comment.getEditCount > 0) {
      Some(new EditSummary(userService.getUserCached(comment.getEditorId).getNick, comment.getEditDate, comment.getEditCount))
    } else {
      None
    }
  }

  private def prepareRSSComment(messageText: MessageText, comment: Comment) = {
    val author = userService.getUserCached(comment.getUserid)
    val processedMessage = textService.renderTextRSS(messageText)
    new PreparedRSSComment(comment, author, processedMessage)
  }

  def prepareCommentForReplyto(comment: Comment, @Nullable currentUser: User, profile: Profile, topic: Topic): PreparedComment = {
    val messageText = msgbaseDao.getMessageText(comment.getId)
    val author = userService.getUserCached(comment.getUserid)

    prepareComment(messageText, author, None, comment, None, profile, topic, Set.empty, Set.empty, currentUser)
  }

  /**
   * Подготовить комментарий для последующего редактирования
   * в комментарии не используется автор и не важно существует ли ответ и автор ответа
   *
   * @param comment Редактируемый комментарий
   * @param message Тело комментария
   * @return подготовленный коментарий
   */
  def prepareCommentForEdit(comment: Comment, message: MessageText): PreparedComment = {
    val author = userService.getUserCached(comment.getUserid)
    val processedMessage = textService.renderCommentText(message, nofollow = false)
    val ref = userService.ref(author, null)

    new PreparedComment(comment, ref, processedMessage, null, // reply
      false, // deletable
      false, // editable
      null, // Remark
      null, // userpic
      null, null, null, null, 0, false, 0, null, false)
  }

  def prepareCommentListRSS(list: java.util.List[Comment]): java.util.List[PreparedRSSComment] = {
    list.asScala.map { comment =>
      val messageText = msgbaseDao.getMessageText(comment.getId)
      prepareRSSComment(messageText, comment)
    }.asJava
  }

  def prepareCommentList(comments: CommentList, list: java.util.List[Comment], topic: Topic,
                         hideSet: java.util.Set[Integer], @Nullable currentUser: User,
                         profile: Profile): java.util.List[PreparedComment] = {
    if (list.isEmpty) {
      Seq.empty.asJava
    } else {
      val texts = msgbaseDao.getMessageText(list.asScala.map(c => Integer.valueOf(c.getId)).asJava)
      val users = userService.getUsersCachedMap(list.asScala.map(_.getUserid))

      val remarks = if (currentUser != null) {
        remarkDao.getRemarks(currentUser, users.values)
      } else {
        Map.empty[Int, Remark]
      }

      val samePageComments = list.asScala.map(_.getId).toSet

      val hideSetScala = hideSet.asScala.view.map(_.toInt).toSet

      list.asScala.map { comment =>
        val text = texts.get(comment.getId)
        val author = users(comment.getUserid)
        val remark = remarks.get(author.getId)

        prepareComment(text, author, remark.map(_.getText), comment, Option(comments), profile, topic, hideSetScala,
          samePageComments, currentUser)
      }.asJava
    }
  }

  def prepareCommentsList(comments: java.util.List[CommentsListItem]): java.util.List[PreparedCommentsListItem] = {
    val users = userService.getUsersCachedMap(comments.asScala.map(_.authorId))

    val texts = msgbaseDao.getMessageText(comments.asScala.map(c => Integer.valueOf(c.commentId)).asJava).asScala

    comments.asScala.map { comment =>
      val author = users(comment.authorId)
      val plainText = textService.extractPlainText(texts(comment.commentId))
      val textPreview = MessageTextService.trimPlainText(plainText, 250, encodeHtml = false)

      PreparedCommentsListItem(comment, author, textPreview)
    }.asJava
  }

  def buildDateJumpSet(comments: java.util.List[Comment], jumpMinDuration: Duration): java.util.Set[Integer] = {
    val commentDates = comments.asScala.view.map { c =>
      c.getId -> new DateTime(c.getPostdate)
    }

    commentDates.zip(commentDates.drop(1)).filter { case (first, second) =>
      new Duration(first._2, second._2).isLongerThan(jumpMinDuration)
    }.map(_._2._1).map(Integer.valueOf).toSet.asJava
  }
}