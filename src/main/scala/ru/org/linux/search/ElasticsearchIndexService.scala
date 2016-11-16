/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.comment.{Comment, CommentList, CommentService}
import ru.org.linux.group.GroupDao
import ru.org.linux.section.SectionService
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.topic.{Topic, TopicDao, TopicTagService}
import ru.org.linux.user.UserDao
import ru.org.linux.util.bbcode.LorCodeService

import scala.collection.JavaConverters._

object ElasticsearchIndexService {
  val MessageIndex = "messages"
  val MessageType = "message"

  val MessageIndexType = IndexAndTypes(MessageIndex, MessageType)
  val MessageIndexTypes = IndexesAndTypes(MessageIndexType)

  val COLUMN_TOPIC_AWAITS_COMMIT = "topic_awaits_commit"
}

@Service
class ElasticsearchIndexService
(
  sectionService: SectionService,
  groupDao: GroupDao,
  userDao: UserDao,
  topicTagService: TopicTagService,
  lorCodeService: LorCodeService,
  msgbaseDao: MsgbaseDao,
  topicDao: TopicDao,
  commentService: CommentService,
  elastic: ElasticClient
) extends StrictLogging {
  import ElasticsearchIndexService._

  private def isTopicSearchable(msg: Topic) = !msg.isDeleted && !msg.isDraft

  private def reindexComments(topic: Topic, comments: CommentList):Seq[BulkCompatibleDefinition] = {
    for (comment <- comments.getList.asScala) yield {
      if (comment.isDeleted) {
        delete id comment.getId.toString from MessageIndexType
      } else {
        val message = lorCodeService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
        indexOfComment(topic, comment, message)
      }
    }
  }

  def reindexMessage(msgid: Int, withComments: Boolean):Unit = {
    val topic = topicDao.getById(msgid)

    if (isTopicSearchable(topic)) {
      val topicIndex = indexOfTopic(topic)

      val commentsIndex = if (withComments) {
        reindexComments(topic, commentService.getCommentList(topic, true))
      } else Seq.empty

      executeBulk(bulk(topicIndex +: commentsIndex))
    } else {
      val topicDelete = delete id topic.getId.toString from MessageIndexType

      val commentsDelete = if (withComments) {
        val comments = commentService.getCommentList(topic, true).getList.asScala

        comments.map {
          comment ⇒ delete id comment.getId.toString from MessageIndexType
        }
      } else Seq.empty

      executeBulk(bulk(topicDelete +: commentsDelete))
    }
  }

  def reindexMonth(year:Int, month:Int):Unit = {
    val topicIds = topicDao.getMessageForMonth(year, month)

    for (topicId <- topicIds.asScala) {
      reindexMessage(topicId, withComments = true)
    }
  }

  def reindexComments(comments:Seq[Int]) = {
    if (comments.contains(0)) {
      logger.warn("Skipping MSGID=0!!!")
    }

    val requests = for (msgid <- comments if msgid != 0) yield {
      val comment = commentService.getById(msgid)
      val topic = topicDao.getById(comment.getTopicId)

      if (!isTopicSearchable(topic) || comment.isDeleted) {
        delete id comment.getId.toString from MessageIndexType
      } else {
        val message = lorCodeService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
        indexOfComment(topic, comment, message)
      }
    }

    executeBulk(bulk(requests))
  }

  def createIndexIfNeeded():Unit = {
    val indexExists = elastic execute {
      index exists MessageIndex
    } await

    if (!indexExists.isExists) {
      val mappingSource = IOUtils.toString(getClass.getClassLoader.getResource("es-mapping.json"))

      elastic.java
        .admin()
        .indices()
        .prepareCreate(MessageIndex)
        .setSource(mappingSource)
        .execute()
        .actionGet()
    }
  }

  private def executeBulk(bulkRequest: BulkDefinition):Unit = {
    if (bulkRequest.requests.nonEmpty) {
      val bulkResponse = elastic.execute(bulkRequest).await

      if (bulkResponse.hasFailures) {
        logger.warn(s"Bulk index failed: ${bulkResponse.failureMessage}")
        throw new RuntimeException("Bulk request failed")
      }
    }
  }

  private def indexOfComment(topic: Topic, comment: Comment, message: String):IndexDefinition = {
    val section = sectionService.getSection(topic.getSectionId)
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userDao.getUserCached(comment.getUserid)
    val topicAuthor = userDao.getUserCached(topic.getUid)

    val topicTitle = topic.getTitleUnescaped

    val commentTitle = comment.getTitle

    val title =
      Option(commentTitle)
        .filter(_.nonEmpty)
        .filterNot(_ == topicTitle)
        .filterNot(_.startsWith("Re:"))
        .map(StringEscapeUtils.unescapeHtml4)

    index into MessageIndexType id comment.getId.toString fields (
      Map("section" -> section.getUrlName,
        "topic_author" -> topicAuthor.getNick,
        "topic_id" -> topic.getId,
        "author" -> author.getNick,
        "group" -> group.getUrlName,
        "topic_title" -> topicTitle,
        COLUMN_TOPIC_AWAITS_COMMIT -> topicAwaitsCommit(topic),
        "message" -> message,
        "postdate" -> new DateTime(comment.getPostdate),
        "tag" -> topicTagService.getTags(topic),
        "is_comment" -> true) ++ title.map("title" -> _)
      )
  }

  private def topicAwaitsCommit(msg: Topic) = {
    val section = sectionService.getSection(msg.getSectionId)

    section.isPremoderated && !msg.isCommited
  }

  private def indexOfTopic(topic: Topic): IndexDefinition = {
    val section = sectionService.getSection(topic.getSectionId)
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userDao.getUserCached(topic.getUid)

    index into MessageIndexType id topic.getId.toString fields(
      "section" -> section.getUrlName,
      "topic_author" -> author.getNick,
      "topic_id" -> topic.getId,
      "author" -> author.getNick,
      "group" -> group.getUrlName,
      "title" -> topic.getTitleUnescaped,
      "topic_title" -> topic.getTitleUnescaped,
      "message" -> lorCodeService.extractPlainText(msgbaseDao.getMessageText(topic.getId)),
      "postdate" -> new DateTime(topic.getPostdate),
      "tag" -> topicTagService.getTags(topic),
      COLUMN_TOPIC_AWAITS_COMMIT -> topicAwaitsCommit(topic),
      "is_comment" -> false
      )
  }
}
