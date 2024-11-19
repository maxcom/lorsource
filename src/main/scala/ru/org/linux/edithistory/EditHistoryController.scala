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
package ru.org.linux.edithistory

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.comment.CommentReadService
import ru.org.linux.group.{GroupDao, GroupPermissionService}
import ru.org.linux.topic.*
import ru.org.linux.user.UserService

@Controller
class EditHistoryController(messageDao: TopicDao, editHistoryService: EditHistoryService,
                            commentService: CommentReadService, topicPermissionService: TopicPermissionService,
                            groupPermissionService: GroupPermissionService, groupDao: GroupDao,
                            userService: UserService, topicPrepareService: TopicPrepareService) {
  @RequestMapping(Array("/news/{group}/{id}/history", "/forum/{group}/{id}/history", "/gallery/{group}/{id}/history",
    "/polls/{group}/{id}/history", "/articles/{group}/{id}/history"))
  def showEditInfo(@PathVariable("id") msgid: Int): ModelAndView = MaybeAuthorized { implicit currentUserOpt =>
    val topic = messageDao.getById(msgid)
    val group = groupDao.getGroup(topic.groupId)

    val preparedMessage = topicPrepareService.prepareTopic(topic, currentUserOpt.userOpt.orNull)

    topicPermissionService.checkView(group, topic, currentUserOpt.userOpt.orNull, preparedMessage.author, showDeleted = false)

    if (!topicPermissionService.canViewHistory(topic, currentUserOpt.userOpt.orNull)) {
      throw new AccessViolationException("Forbidden")
    }

    val editHistories = editHistoryService.prepareEditInfo(topic)

    val modelAndView = new ModelAndView("history")

    modelAndView.getModel.put("message", topic)
    modelAndView.getModel.put("editHistories", editHistories)
    modelAndView.getModel.put("canRestore", groupPermissionService.isEditable(preparedMessage))

    modelAndView
  }

  @RequestMapping(Array("/news/{group}/{id}/{commentid}/history", "/forum/{group}/{id}/{commentid}/history",
    "/gallery/{group}/{id}/{commentid}/history", "/polls/{group}/{id}/{commentid}/history",
    "/articles/{group}/{id}/{commentid}/history"))
  def showCommentEditInfo(@PathVariable("id") msgid: Int,
                          @PathVariable("commentid") commentId: Int): ModelAndView = MaybeAuthorized { currentUserOpt =>
    val topic = messageDao.getById(msgid)
    val group = groupDao.getGroup(topic.groupId)
    val comment = commentService.getById(commentId)
    val editHistories = editHistoryService.prepareEditInfo(comment)
    val topicAuthor = userService.getUserCached(topic.authorUserId)

    topicPermissionService.checkView(group, topic, currentUserOpt.userOpt.orNull, topicAuthor, showDeleted = false)

    val modelAndView = new ModelAndView("history")

    modelAndView.getModel.put("message", topic)
    modelAndView.getModel.put("editHistories", editHistories)
    modelAndView.getModel.put("canRestore", false)

    modelAndView
  }
}