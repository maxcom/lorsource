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

import ru.org.linux.site.MessageNotFoundException

import java.time.Instant
import scala.collection.immutable.HashMap

class CommentList(val comments: Vector[Comment], val lastmod: Instant) {
  val root: RootCommentNode = RootCommentNode.build(comments)

  private val nodeIndex: Map[Int, ReplyCommentNode] = {
    val builder = HashMap.newBuilder[Int, ReplyCommentNode]

    def buildIndex(root: CommentNode): Unit = {
      root match
        case _: RootCommentNode =>
        case r: ReplyCommentNode =>
          builder.addOne(r.comment.id -> r)

      for (child <- root.childNodes) {
        buildIndex(child)
      }
    }

    buildIndex(root)

    builder.result()
  }

  def getNode(msgid: Int): ReplyCommentNode = nodeIndex.getOrElse(msgid,throw new MessageNotFoundException(msgid))

  def getNodeOpt(msgid: Int): Option[ReplyCommentNode] = nodeIndex.get(msgid)

  def getCommentPage(comment: Comment, messages: Int): Int = {
    val index = comments.indexOf(comment)
    index / messages
  }
}