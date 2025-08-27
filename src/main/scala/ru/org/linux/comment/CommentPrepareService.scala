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

import com.google.common.base.Strings
import org.joda.time.{DateTime, Duration}
import org.springframework.stereotype.Service
import ru.org.linux.auth.AnySession
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.reaction.{PreparedReactions, ReactionService}
import ru.org.linux.site.ApiDeleteInfo
import ru.org.linux.spring.dao.{DeleteInfoDao, MessageText, MsgbaseDao, UserAgentDao}
import ru.org.linux.topic.{Topic, TopicPermissionService}
import ru.org.linux.user.*
import ru.org.linux.warning.{Warning, WarningService}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Service
class CommentPrepareService(textService: MessageTextService, msgbaseDao: MsgbaseDao,
                            topicPermissionService: TopicPermissionService, userService: UserService,
                            deleteInfoDao: DeleteInfoDao, userAgentDao: UserAgentDao, remarkDao: RemarkDao,
                            groupDao: GroupDao, reactionPrepareService: ReactionService,
                            warningService: WarningService) {

  private def prepareComment(messageText: MessageText, author: User, remark: Option[String], comment: Comment,
                             comments: Option[CommentList], topic: Topic, hideSet: Set[Int], samePageComments: Set[Int],
                             group: Group, ignoreList: Set[Int], filterShow: Boolean, warnings: Seq[Warning])
                            (implicit session: AnySession) = {
    val processedMessage = textService.renderCommentText(messageText, !topicPermissionService.followAuthorLinks(author))

    val (answerLink, answerSamepage, answerCount, replyInfo, hasAnswers) = if (comments.isDefined) {
      val replyInfo: Option[ReplyInfo] = if (comment.replyTo != 0) {
        comments.get.getNodeOpt(comment.replyTo) match {
          case None => // ответ на удаленный комментарий
            Some(new ReplyInfo(comment.replyTo, true))
          case Some(replyNode) =>
            val reply = replyNode.getComment
            val samePage = samePageComments.contains(reply.id)
            val replyAuthor = userService.getUserCached(reply.userid).getNick
            Some(new ReplyInfo(reply.id, replyAuthor, Strings.emptyToNull(reply.title.trim), reply.postdate, samePage, false))
        }
      } else {
        None
      }

      val node = comments.get.getNode(comment.id)
      val replysFiltered = node.childs.asScala.filter(commentNode => !hideSet.contains(commentNode.getComment.id))

      val answerCount = replysFiltered.size

      if (answerCount > 1) {
        val link = if (!filterShow) {
          s"${topic.getLink}/thread/${comment.id}#comments"
        } else {
          s"${topic.getLink}/thread/${comment.id}?filter=show#comments"
        }

        (Some(link), false, answerCount, replyInfo, true)
      } else if (answerCount == 1) {
        val answerSamepage = samePageComments.contains(replysFiltered.head.getComment.id)

        val link = if (!filterShow) {
          s"${topic.getLink}?cid=${replysFiltered.head.getComment.id}"
        } else {
          s"${topic.getLink}?filter=show&cid=${replysFiltered.head.getComment.id}"
        }

        (Some(link), answerSamepage, 1, replyInfo, true)
      } else {
        (None, false, 0, replyInfo, false)
      }
    } else {
      (None, false, 0, None, false)
    }

    val userpic: Option[Userpic] = if (session.profile.showPhotos) {
      Some(userService.getUserpic(author, session.profile.avatarMode, misteryMan = false))
    } else {
      None
    }

    val deleteInfo = loadDeleteInfo(comment)
    val apiDeleteInfo = deleteInfo.map(i => new ApiDeleteInfo(userService.getUserCached(i.userid).getNick, i.getReason))
    val editSummary = loadEditSummary(comment)

    val (postIP, userAgent) = if (session != null && session.moderator) {
      (Option(comment.postIP), userAgentDao.getUserAgentById(comment.userAgentId).toScala)
    } else {
      (None, None)
    }

    val undeletable = topicPermissionService.isUndeletable(topic, comment, deleteInfo)(session)
    val deletable = topicPermissionService.isCommentDeletableNow(comment, topic, hasAnswers)(session)
    val editable = topicPermissionService.isCommentEditableNow(comment, hasAnswers, topic, messageText.markup)(session)
    val warningsAllowed = topicPermissionService.canPostWarning(topic, Some(comment))(session)

    val authorReadonly = !topicPermissionService.isCommentsAllowedByUser(group, topic, Some(author), ignoreFrozen = true)

    val preparedWarnings = warningService.prepareWarning(warnings)

    PreparedComment(comment = comment, author = author, processedMessage = processedMessage, reply = replyInfo,
      deletable = deletable, editable = editable, remark = remark, userpic = userpic, deleteInfo = apiDeleteInfo,
      editSummary = editSummary, postIP = postIP, userAgent = userAgent, undeletable = undeletable,
      answerCount = answerCount, answerLink = answerLink, answerSamepage = answerSamepage,
      authorReadonly = authorReadonly,
      reactions = reactionPrepareService.prepare(comment.reactions, ignoreList,topic, Some(comment)),
      warningsAllowed = warningsAllowed, warnings = preparedWarnings)
  }

  private def loadDeleteInfo(comment: Comment) = {
    if (comment.deleted) {
      deleteInfoDao.getDeleteInfo(comment.id).toScala
    } else {
      None
    }
  }

  private def loadEditSummary(comment: Comment): Option[EditSummary] = {
    if (comment.editCount > 0) {
      Some(new EditSummary(userService.getUserCached(comment.editorId).getNick, comment.editDate, comment.editCount))
    } else {
      None
    }
  }

  def prepareCommentOnly(comment: Comment, topic: Topic, ignoreList: Set[Int])
                        (implicit currentUser: AnySession): PreparedComment = {
    assert(comment.topicId == topic.id)

    val messageText = msgbaseDao.getMessageText(comment.id)
    val author = userService.getUserCached(comment.userid)
    val group = groupDao.getGroup(topic.groupId)

    prepareComment(messageText = messageText, author = author, remark = None, comment = comment, comments = None,
      topic = topic, hideSet = Set.empty, samePageComments = Set.empty, group = group,
      ignoreList = ignoreList, filterShow = false, warnings = Seq.empty)
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
    val author = userService.getUserCached(comment.userid)
    val processedMessage = textService.renderCommentText(message, nofollow = false)

    PreparedComment(comment = comment, author = author, reply = None, editable = false, remark = None,
      userpic = None, deleteInfo = None, editSummary = None, postIP = None, userAgent = None, undeletable = false,
      answerCount = 0, answerLink = None, answerSamepage = false, authorReadonly = false,
      processedMessage = processedMessage, deletable = false, reactions = PreparedReactions.emptyDisabled,
      warningsAllowed = false, warnings = Seq.empty)
  }

  def prepareCommentList(comments: CommentList, list: Seq[Comment], topic: Topic, hideSet: Set[Int],
                         ignoreList: Set[Int], filterShow: Boolean)
                        (implicit currentUser: AnySession): Seq[PreparedComment] = {
    if (list.isEmpty) {
      Seq.empty
    } else {
      val texts = msgbaseDao.getMessageText(list.map(_.id))
      val users = userService.getUsersCachedMap(list.map(_.userid))
      val group = groupDao.getGroup(topic.groupId)

      val allWarnings: Map[Int, Seq[Warning]] = if (!topic.expired && currentUser.moderator) {
        warningService.load(list)
      } else {
        Map.empty
      }

      val remarks = currentUser.opt.map { user =>
        remarkDao.getRemarks(user.user, users.values)
      }.getOrElse(Map.empty[Int, Remark])

      val samePageComments = list.map(_.id).toSet

      list.map { comment =>
        val text = texts(comment.id)
        val author = users(comment.userid)
        val remark = remarks.get(author.getId)
        val warnings = allWarnings.getOrElse(comment.id, Seq.empty)

        prepareComment(text, author, remark.map(_.getText), comment, Option(comments), topic, hideSet,
          samePageComments, group, ignoreList, filterShow, warnings)
      }
    }
  }

  def buildDateJumpSet(comments: Seq[Comment], jumpMinDuration: Duration): java.util.Set[Integer] = {
    val commentDates = comments.view.map { c =>
      c.id -> new DateTime(c.postdate)
    }

    commentDates.zip(commentDates.drop(1)).filter { case (first, second) =>
      new Duration(first._2, second._2).isLongerThan(jumpMinDuration)
    }.map(_._2._1).map(Integer.valueOf).toSet.asJava
  }
}