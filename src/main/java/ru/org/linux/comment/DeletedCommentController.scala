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

package ru.org.linux.comment

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.topic.TopicPermissionService.POSTSCORE_HIDE_COMMENTS
import ru.org.linux.topic.{TopicDao, TopicPermissionService}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

@Controller
class DeletedCommentController(deleteInfoDao: DeleteInfoDao, commentReadService: CommentReadService,
                               topicDao: TopicDao, topicPermissionService: TopicPermissionService,
                               commentPrepareService: CommentPrepareService) {
  @RequestMapping(Array("/view-deleted"))
  def viewDeleted(@RequestParam("id") id: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val deleteInfo = deleteInfoDao.getDeleteInfo(id).getOrElse(throw new MessageNotFoundException(id))
    val comment = commentReadService.getById(id)
    val topic = topicDao.getById(comment.topicId)

    if (topicPermissionService.canViewDeletedComment(comment, deleteInfo)) {
      val preparedComment = commentPrepareService.prepareCommentOnly(comment, topic, Set.empty)

      val chain = if (deleteInfo.reason.startsWith("7.1 ") && topic.postscore != POSTSCORE_HIDE_COMMENTS) {
        loadChain(comment, List.empty)
      } else {
        Seq.empty
      }

      val preparedChain = chain.map(comment => commentPrepareService.prepareCommentOnly(comment, topic, Set.empty))

      new ModelAndView("view-deleted", Map(
        "comment" -> preparedComment,
        "chain" -> preparedChain.asJava,
        "topic" -> topic
      ).asJava)
    } else {
      throw new AccessViolationException("Вы не можете посмотреть это удаленное сообщение")
    }
  }

  @tailrec
  private def loadChain(comment: Comment, acc: List[Comment]): Seq[Comment] = {
    if (comment.replyTo != 0) {
      val parent = commentReadService.getById(comment.replyTo)
      val withParent = parent :: acc

      if (parent.deleted) {
        deleteInfoDao.getDeleteInfo(parent.id) match {
          case Some(parentDelInfo) if parentDelInfo.reason.startsWith("7.1 ") =>
            loadChain(parent, withParent)
          case _ =>
            withParent
        }
      } else {
        withParent
      }
    } else {
      acc
    }
  }
}
