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
import org.apache.commons.text.StringEscapeUtils
import ru.org.linux.reaction.PreparedReactions
import ru.org.linux.site.ApiDeleteInfo
import ru.org.linux.user.{User, Userpic}

import java.sql.Timestamp
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

object PreparedComment {
  def apply(comment: Comment, author: User, processedMessage: String, reply: Option[ReplyInfo], deletable: Boolean,
            undeletable: Boolean, editable: Boolean, remark: Option[String], userpic: Option[Userpic], answerCount: Int,
            deleteInfo: Option[ApiDeleteInfo], editSummary: Option[EditSummary], userAgent: Option[String],
            answerLink: Option[String], answerSamepage: Boolean, authorReadonly: Boolean, postIP: Option[String],
            @BeanProperty reactions: PreparedReactions): PreparedComment = {
    val encodedTitle = Strings.emptyToNull(comment.title.trim)

    val title = if (encodedTitle != null) {
      StringEscapeUtils.unescapeHtml4(encodedTitle)
    } else {
      null
    }

    PreparedComment(
      id = comment.id,
      title = title,
      deleted = comment.deleted,
      postdate = comment.postdate,
      author = author,
      processedMessage = processedMessage,
      reply = reply.orNull,
      deletable = deletable,
      undeletable = undeletable,
      editable = editable,
      remark = remark.orNull,
      userpic = userpic.orNull,
      deleteInfo = deleteInfo.orNull,
      editSummary = editSummary.orNull,
      postIP = postIP.orNull,
      userAgentId = comment.userAgentId,
      userAgent = userAgent.orNull,
      answerCount = answerCount,
      answerLink = answerLink.orNull,
      answerSamepage = answerSamepage,
      authorReadonly = authorReadonly,
      reactions = reactions)
  }
}

case class PreparedComment(@BeanProperty id: Int, @BeanProperty author: User, @BeanProperty processedMessage: String,
                           @Nullable @BeanProperty reply: ReplyInfo, @BooleanBeanProperty deletable: Boolean,
                           @BooleanBeanProperty editable: Boolean, @Nullable @BeanProperty remark: String,
                           @Nullable @BeanProperty userpic: Userpic, @Nullable @BeanProperty deleteInfo: ApiDeleteInfo,
                           @Nullable @BeanProperty editSummary: EditSummary, @Nullable @BeanProperty postIP: String,
                           @Nullable @BeanProperty userAgent: String, @BeanProperty userAgentId: Int,
                           @BooleanBeanProperty undeletable: Boolean, @BeanProperty answerCount: Int,
                           @BeanProperty @Nullable answerLink: String, @BooleanBeanProperty answerSamepage: Boolean,
                           @BooleanBeanProperty authorReadonly: Boolean, @Nullable @BeanProperty title: String,
                           @BooleanBeanProperty deleted: Boolean, @BeanProperty postdate: Timestamp,
                           @BeanProperty reactions: PreparedReactions)