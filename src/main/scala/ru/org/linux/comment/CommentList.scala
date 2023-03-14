/*
 * Copyright 1998-2023 Linux.org.ru
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
import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala

class CommentList(val comments: Seq[Comment], val lastmod: Instant) {
  val root: CommentNode = {
    val tempIndex = new mutable.HashMap[Int, CommentNodeBuilder]()
    val rootBuilder = new CommentNodeBuilder

    for (comment <- comments) {
      val node = new CommentNodeBuilder(comment)

      tempIndex.put(comment.id, node)

      if (comment.replyTo == 0) {
        rootBuilder.addChild(node)
      } else {
        tempIndex.get(comment.replyTo) match {
          case Some(parent) =>
            parent.addChild(node)
          case None =>
           rootBuilder.addChild(node)
        }
      }
    }

    rootBuilder.build
  }

  private val nodeIndex: Map[Int, CommentNode] = {
    val builder = HashMap.newBuilder[Int, CommentNode]

    def buildIndex(root: CommentNode): Unit = {
      if (root.getComment != null) {
        builder.addOne(root.getComment.id -> root)
      }

      for (child <- root.childs.asScala) {
        buildIndex(child)
      }
    }

    buildIndex(root)

    builder.result()
  }

  def getNode(msgid: Int): CommentNode = nodeIndex.getOrElse(msgid,throw new MessageNotFoundException(msgid))

  def getNodeOpt(msgid: Int): Option[CommentNode] = nodeIndex.get(msgid)

  def getCommentPage(comment: Comment, messages: Int): Int = {
    val index = comments.indexOf(comment)
    index / messages
  }
}