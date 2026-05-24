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

import scala.collection.mutable

sealed trait CommentNode:
  protected def commentOpt: Option[Comment]
  def childNodes: Vector[ReplyCommentNode]

  def hideIgnored(hideSet: mutable.Set[Int], ignoreList: Set[Int]): Unit =
    commentOpt.foreach { c =>
      if c.isIgnored(ignoreList) then
        hideNode(hideSet)
    }

    if commentOpt.forall(c => !hideSet.contains(c.id)) then
      for child <- childNodes do
        child.hideIgnored(hideSet, ignoreList)

  def foreach(f: Comment => Unit): Unit =
    commentOpt.foreach(f)

    for child <- childNodes do
      child.foreach(f)

  private def hideNode(hideSet: mutable.Set[Int]): Unit = foreach(c => hideSet.add(c.id))

case class RootCommentNode(childNodes: Vector[ReplyCommentNode]) extends CommentNode:
  override def commentOpt: None.type = None

object RootCommentNode:
  private class RootCommentNodeBuilder:
    private val childNodes = Vector.newBuilder[ReplyCommentNodeBuilder]

    def addChild(child: ReplyCommentNodeBuilder): Unit = childNodes.addOne(child)

    def build: RootCommentNode = RootCommentNode(childNodes.result().map(_.build))

  private class ReplyCommentNodeBuilder(comment: Comment):
    private val childNodes = Vector.newBuilder[ReplyCommentNodeBuilder]

    def addChild(child: ReplyCommentNodeBuilder): Unit = childNodes.addOne(child)

    def build: ReplyCommentNode = ReplyCommentNode(comment, childNodes.result().map(_.build))

  def build(comments: Vector[Comment]): RootCommentNode = {
    val tempIndex = new mutable.HashMap[Int, ReplyCommentNodeBuilder]()
    val rootBuilder = new RootCommentNodeBuilder

    for (comment <- comments) {
      val node = new ReplyCommentNodeBuilder(comment)

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

case class ReplyCommentNode(comment: Comment, childNodes: Vector[ReplyCommentNode]) extends CommentNode:
  override def commentOpt: Some[Comment] = Some(comment)
