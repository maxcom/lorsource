/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.edithistory

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.MaybeAuthorizedCtx
import ru.org.linux.comment.CommentReadService
import ru.org.linux.group.GroupService
import ru.org.linux.rights.EditTopicChecker
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.*
import ru.org.linux.user.UserService

import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller
class EditHistoryController(
    messageDao: TopicDao,
    editHistoryService: EditHistoryService,
    commentService: CommentReadService,
    topicPermissionService: TopicPermissionService,
    groupService: GroupService,
    userService: UserService,
    topicPrepareService: TopicPrepareService):
  @RequestMapping(
    Array(
      "/news/{group}/{id}/history",
      "/forum/{group}/{id}/history",
      "/gallery/{group}/{id}/history",
      "/polls/{group}/{id}/history",
      "/articles/{group}/{id}/history"
    ))
  def showEditInfo(
      @PathVariable("id")
      msgid: Int): ModelAndView =
    MaybeAuthorizedCtx {
      val topic = messageDao.getById(msgid)
      val group = groupService.getGroup(topic.groupId)

      val preparedMessage = topicPrepareService.prepareTopic(topic)

      topicPermissionService.checkView(group, topic, preparedMessage.author, showDeleted = false)

      if !topicPermissionService.canViewHistory(topic) then
        throw new AccessViolationException("Forbidden")

      val editHistories = editHistoryService.prepareEditInfo(topic)

      val modelAndView = new ModelAndView("history")

      modelAndView.getModel.put("message", topic)
      modelAndView.getModel.put("editHistories", editHistories.asJava)
      modelAndView.getModel.put("canRestore", EditTopicChecker.checkContentEdit(preparedMessage).permitted)

      modelAndView
    }

  @RequestMapping(
    Array(
      "/news/{group}/{id}/{commentid}/history",
      "/forum/{group}/{id}/{commentid}/history",
      "/gallery/{group}/{id}/{commentid}/history",
      "/polls/{group}/{id}/{commentid}/history",
      "/articles/{group}/{id}/{commentid}/history"
    ))
  def showCommentEditInfo(
      @PathVariable("id")
      msgid: Int,
      @PathVariable("commentid")
      commentId: Int): ModelAndView =
    MaybeAuthorizedCtx {
      val topic = messageDao.getById(msgid)
      val comment = commentService.getById(commentId)

      if comment.topicId != topic.id then
        throw new MessageNotFoundException(topic, commentId, s"Сообщение #$commentId было удалено или не существует")

      val topicAuthor = userService.getUserCached(topic.authorUserId)

      val group = groupService.getGroup(topic.groupId)
      topicPermissionService.checkView(group, topic, topicAuthor, showDeleted = false)

      if !topicPermissionService.canViewHistory(topic, comment) then
        throw new AccessViolationException("Forbidden")

      val editHistories = editHistoryService.prepareEditInfo(comment)

      val modelAndView = new ModelAndView("history")

      modelAndView.getModel.put("message", topic)
      modelAndView.getModel.put("editHistories", editHistories.asJava)
      modelAndView.getModel.put("canRestore", false)

      modelAndView
    }
