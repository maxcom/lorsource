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

package ru.org.linux.search

import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.StringEscapeUtils
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.bulk.{BulkOperation, DeleteOperation, IndexOperation}
import org.springframework.stereotype.Service
import ru.org.linux.comment.{Comment, CommentReadService}
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.msgbase.MsgbaseDao
import ru.org.linux.section.SectionService
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService, TopicTagService}
import ru.org.linux.user.UserService

import scala.jdk.CollectionConverters.*

object OpenSearchIndexService {
  val MessageIndex = "messages"

  val COLUMN_TOPIC_AWAITS_COMMIT = "topic_awaits_commit"
}

@Service
class OpenSearchIndexService(sectionService: SectionService, groupDao: GroupDao, userService: UserService,
                             topicTagService: TopicTagService, messageTextService: MessageTextService,
                             msgbaseDao: MsgbaseDao, topicDao: TopicDao, commentService: CommentReadService,
                             client: OpenSearchClient, topicPermissionService: TopicPermissionService,
                             siteConfig: SiteConfig) extends StrictLogging {
  import OpenSearchIndexService.*

  private def reindexComments(topic: Topic, comments: Seq[Comment]): Seq[BulkOperation] = {
    comments.map { comment =>
      if (comment.deleted) {
        BulkOperation.of(op => op.delete(DeleteOperation.of(d => d
          .index(MessageIndex)
          .id(comment.id.toString))))
      } else {
        val group = groupDao.getGroup(topic.groupId)
        indexCommentToBulkOp(topic, comment, group)
      }
    }
  }

  def reindexComments(comments: Seq[Int]): Unit = {
    if (comments.contains(0)) {
      logger.warn("Skipping MSGID=0!!!")
    }

    val requests = comments.filterNot(_ == 0).map { msgid =>
      val comment = commentService.getById(msgid)
      lazy val topic = topicDao.getById(comment.topicId)
      lazy val group = groupDao.getGroup(topic.groupId)

      if (comment.deleted || !topicPermissionService.isTopicSearchable(topic, group)) {
        BulkOperation.of(op => op.delete(DeleteOperation.of(d => d
          .index(MessageIndex)
          .id(comment.id.toString))))
      } else {
        indexCommentToBulkOp(topic, comment, group)
      }
    }

    executeBulk(requests)
  }

  private def indexCommentToBulkOp(topic: Topic, comment: Comment, group: Group): BulkOperation = {
    val doc = indexOfComment(topic, comment, group)
    val indexOp = IndexOperation.of[java.util.Map[String, Any]](i => i
      .index(MessageIndex)
      .id(comment.id.toString)
      .document(doc.asJava))
    BulkOperation.of(op => op.index(indexOp))
  }

  def reindexMessage(msgid: Int, withComments: Boolean): Unit = {
    val topic = topicDao.getById(msgid)
    val group = groupDao.getGroup(topic.groupId)

    if (topicPermissionService.isTopicSearchable(topic, group)) {
      val topicDoc = indexOfTopic(topic, group)

      val indexOp = IndexOperation.of[java.util.Map[String, Any]](i => i
        .index(MessageIndex)
        .id(topic.id.toString)
        .document(topicDoc.asJava))

      val operations = Seq(
        BulkOperation.of(op => op.index(indexOp))
      )

      val commentsIndex = if (withComments) {
        reindexComments(topic, commentService.getCommentList(topic, showDeleted = true).comments)
      } else Seq.empty

      executeBulk(operations ++ commentsIndex)
    } else {
      val topicDelete = BulkOperation.of(op => op.delete(DeleteOperation.of(d => d
        .index(MessageIndex)
        .id(topic.id.toString))))

      val commentsDelete = if (withComments) {
        val comments = commentService.getCommentList(topic, showDeleted = true).comments

        comments.map { comment =>
          BulkOperation.of(op => op.delete(DeleteOperation.of(d => d
            .index(MessageIndex)
            .id(comment.id.toString))))
        }
      } else Seq.empty

      executeBulk(topicDelete +: commentsDelete)
    }
  }

  def reindexMonth(year: Int, month: Int):Unit = {
    val topicIds = topicDao.getMessageForMonth(year, month)

    for (topicId <- topicIds.asScala) {
      reindexMessage(topicId, withComments = true)
    }
  }

  private def executeBulk(operations: Seq[BulkOperation]): Unit = {
    if (operations.nonEmpty) {
      val bulkRequest = BulkRequest.of(b => {
        operations.foreach(op => b.operations(op))
        b
      })

      val bulkResponse = client.bulk(bulkRequest)

      if (bulkResponse.errors()) {
        val failures = bulkResponse.items().asScala.toSeq.flatMap(item => Option(item.error()).map(_.reason()))
        logger.warn(s"Bulk index failed: ${failures.mkString(", ")}")
        throw new RuntimeException("Bulk request failed")
      }
    }
  }

  private def indexOfComment(topic: Topic, comment: Comment, group: Group): Map[String, Any] = {
    val section = sectionService.getSection(topic.sectionId)
    val author = userService.getUserCached(comment.userid)
    val topicAuthor = userService.getUserCached(topic.authorUserId)

    val html = messageTextService.renderCommentText(msgbaseDao.getMessageText(comment.id),
      nofollow = !topicPermissionService.followAuthorLinks(author))

    val topicTitle = topic.getTitleUnescaped

    val commentTitle = comment.title

    val title =
      Option(commentTitle)
        .filter(_.nonEmpty)
        .filterNot(_ == topicTitle)
        .filterNot(_.startsWith("Re:"))
        .map(StringEscapeUtils.unescapeHtml4)

    val fields = scala.collection.mutable.Map[String, Any](
      "section" -> section.getUrlName,
      "topic_author" -> topicAuthor.nick,
      "topic_id" -> topic.id,
      "author" -> author.nick,
      "group" -> group.urlName,
      "topic_title" -> topicTitle,
      COLUMN_TOPIC_AWAITS_COMMIT -> topicAwaitsCommit(topic),
      "message" -> html,
      "postdate" -> comment.postdate.toInstant.toString,
      "tag" -> topicTagService.getTags(topic),
      "is_comment" -> true
    )

    title.foreach(t => fields("title") = t)

    fields.toMap
  }

  private def topicAwaitsCommit(msg: Topic) = {
    val section = sectionService.getSection(msg.sectionId)

    section.isPremoderated && !msg.commited
  }

  private def indexOfTopic(topic: Topic, group: Group): Map[String, Any] = {
    val section = sectionService.getSection(topic.sectionId)
    val author = userService.getUserCached(topic.authorUserId)

    val url = s"${siteConfig.getSecureUrlWithoutSlash}${topic.getLink}"

    val html = messageTextService.renderTopic(
      msgbaseDao.getMessageText(topic.id),
      minimizeCut = false,
      nofollow = !topicPermissionService.followInTopic(topic, author),
      canonicalUrl = url)

    Map(
      "section" -> section.getUrlName,
      "topic_author" -> author.nick,
      "topic_id" -> topic.id,
      "author" -> author.nick,
      "group" -> group.urlName,
      "title" -> topic.getTitleUnescaped,
      "topic_title" -> topic.getTitleUnescaped,
      "message" -> html,
      "postdate" -> topic.postdate.toInstant.toString,
      "tag" -> topicTagService.getTags(topic),
      COLUMN_TOPIC_AWAITS_COMMIT -> topicAwaitsCommit(topic),
      "is_comment" -> false
    )
  }
}
