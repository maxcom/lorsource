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

import java.sql.{ResultSet, Timestamp}
import java.time.Instant
import java.util
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class Comment(@BeanProperty id: Int, @BeanProperty title: String, @BeanProperty userid: Int,
                   @BeanProperty replyTo: Int, @BeanProperty topicId: Int, @BooleanBeanProperty deleted: Boolean,
                   @BeanProperty postdate: Timestamp, @BeanProperty userAgentId: Int,
                   @Nullable @BeanProperty postIP: String, @BeanProperty editorId: Int,
                   @Nullable @BeanProperty editDate: Timestamp, @BeanProperty editCount: Int) {
  def isIgnored(ignoreList: util.Set[Integer]): Boolean = ignoreList != null && ignoreList.contains(userid)
}

object Comment {
  def apply(rs: ResultSet): Comment = {
    Comment(
      id = rs.getInt("msgid"),
      title = rs.getString("title"),
      topicId = rs.getInt("topic"),
      replyTo = rs.getInt("replyto"),
      deleted = rs.getBoolean("deleted"),
      postdate = rs.getTimestamp("postdate"),
      userid = rs.getInt("userid"),
      userAgentId = rs.getInt("ua_id"),
      postIP = rs.getString("postip"),
      editCount = rs.getInt("edit_count"),
      editorId = rs.getInt("editor_id"),
      editDate = rs.getTimestamp("edit_date"))
  }

  def buildNew(replyto: Integer, topic: Int, msgid: Int, userid: Int, postIP: String): Comment = {
    Comment(
      id = msgid,
      title = "",
      topicId = topic,
      replyTo = if (replyto != null) replyto else 0,
      editCount = 0,
      editDate = null,
      editorId = 0,
      deleted = false,
      postdate = Timestamp.from(Instant.now()),
      userid = userid,
      userAgentId = 0,
      postIP = postIP)
  }
}