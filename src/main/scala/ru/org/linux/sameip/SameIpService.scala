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

package ru.org.linux.sameip

import org.springframework.stereotype.Service
import ru.org.linux.markup.MessageTextService
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.user.UserService

@Service
class SameIpService(userService: UserService, msgbaseDao: MsgbaseDao, textService: MessageTextService,
                    sameIpDao: SameIpDao) {
  def getPosts(ip: Option[String], userAgent: Option[Int], score: Option[Int], limit: Int): collection.Seq[PreparedPostListItem] = {
    prepareCommentList(sameIpDao.getComments(ip, userAgent, score, limit))
  }

  private def prepareCommentList(items: collection.Seq[PostListItem]): collection.Seq[PreparedPostListItem] = {
    val users = userService.getUsersCachedMap(items.map(_.authorId))
    val texts = msgbaseDao.getMessageText(items.map(i => i.commentId.getOrElse(i.topicId)))

    items.map { item =>
      val plainText = textService.extractPlainText(texts(item.commentId.getOrElse(item.topicId)))
      val textPreview = MessageTextService.trimPlainText(plainText, 250, encodeHtml = false)

      PreparedPostListItem(
        link = item.commentId.map { commentId =>
          s"jump-message.jsp?msgid=${item.topicId}&amp;cid=$commentId"
        }.getOrElse(s"jump-message.jsp?msgid=${item.topicId}"),
        groupTitle = item.groupTitle,
        author = users(item.authorId),
        title = item.title,
        deleted = item.deleted,
        textPreview = textPreview,
        reason = item.reason,
        postdate = item.postdate,
        comment = item.commentId.isDefined)
    }
  }
}
