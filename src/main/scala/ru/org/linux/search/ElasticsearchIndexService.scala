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

package ru.org.linux.search

import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.bulk.{BulkCompatibleRequest, BulkRequest}
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.{MappingDefinition, TermVector}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.StringEscapeUtils
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.comment.{Comment, CommentList, CommentService}
import ru.org.linux.group.GroupDao
import ru.org.linux.markup.MessageTextService
import ru.org.linux.section.SectionService
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService, TopicTagService}
import ru.org.linux.user.UserDao

import scala.jdk.CollectionConverters._
import scala.collection.{Seq => MSeq}

object ElasticsearchIndexService {
  val MessageIndex = "messages"
  val MessageType = "message"

  val MessageIndexType: IndexAndType = IndexAndType(MessageIndex, MessageType)

  val COLUMN_TOPIC_AWAITS_COMMIT = "topic_awaits_commit"

  val Mapping: MappingDefinition = mapping(MessageType).fields(
    keywordField("group"),
    keywordField("section"),
    booleanField("is_comment"),
    dateField("postdate"),
    keywordField("author"),
    keywordField("tag"),
    keywordField("topic_author"),
    longField("topic_id"),
    textField("topic_title").index(false),
    textField("title").analyzer("text_analyzer"),
    textField("message").analyzer("text_analyzer").termVector(TermVector.WithPositionsOffsets),
    booleanField("topic_awaits_commit")
  ).all(false)

  val Analyzers: Seq[CustomAnalyzerDefinition] = Seq(
    CustomAnalyzerDefinition(
      "text_analyzer",
      tokenizer = StandardTokenizer,
      filters = Seq(
        LengthTokenFilter("m_long_word").max(100),
        LowercaseTokenFilter,
        StandardTokenFilter,
        MappingCharFilter("m_ee", "ё" -> "е", "Ё" -> "Е"),
        SnowballTokenFilter("m_my_snow_ru", "Russian"),
        SnowballTokenFilter("m_my_snow_en", "English")))
  )
}

@Service
class ElasticsearchIndexService
(
  sectionService: SectionService,
  groupDao: GroupDao,
  userDao: UserDao,
  topicTagService: TopicTagService,
  messageTextService: MessageTextService,
  msgbaseDao: MsgbaseDao,
  topicDao: TopicDao,
  commentService: CommentService,
  elastic: ElasticClient
) extends StrictLogging {
  import ElasticsearchIndexService._

  private def isTopicSearchable(msg: Topic) =
    !msg.isDeleted && !msg.isDraft && (msg.getPostscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS)

  private def reindexComments(topic: Topic, comments: CommentList): MSeq[BulkCompatibleRequest] = {
    for (comment <- comments.getList.asScala) yield {
      if (comment.isDeleted) {
        delete(comment.getId.toString) from MessageIndexType
      } else {
        val message = messageTextService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
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
      val topicDelete = delete(topic.getId.toString) from MessageIndexType

      val commentsDelete = if (withComments) {
        val comments = commentService.getCommentList(topic, true).getList.asScala

        comments.map {
          comment => delete(comment.getId.toString) from MessageIndexType
        }
      } else Seq.empty

      executeBulk(bulk(topicDelete +: commentsDelete))
    }
  }

  def reindexMonth(year: Int, month: Int):Unit = {
    val topicIds = topicDao.getMessageForMonth(year, month)

    for (topicId <- topicIds.asScala) {
      reindexMessage(topicId, withComments = true)
    }
  }

  def reindexComments(comments: MSeq[Int]): Unit = {
    if (comments.contains(0)) {
      logger.warn("Skipping MSGID=0!!!")
    }

    val requests = for (msgid <- comments if msgid != 0) yield {
      val comment = commentService.getById(msgid)
      val topic = topicDao.getById(comment.getTopicId)

      if (!isTopicSearchable(topic) || comment.isDeleted) {
        delete(comment.getId.toString) from MessageIndexType
      } else {
        val message = messageTextService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
        indexOfComment(topic, comment, message)
      }
    }

    executeBulk(bulk(requests))
  }

  def reindexComments(comments: java.util.List[java.lang.Integer]): Unit = {
    reindexComments(comments.asScala.map(x => x.toInt))
  }

  def reindexTopics(topics: java.util.List[java.lang.Integer]): Unit = {
    for (topic <- topics.asScala.map(x => x.toInt)) {
      reindexMessage(topic, withComments = false)
    }
  }

  def createIndexIfNeeded(): Unit = {
    val indexExistsResult = elastic execute {
      indexExists(MessageIndex)
    } await

    if (!indexExistsResult.result.isExists) {
      elastic execute {
        createIndex(MessageIndex).mappings(Mapping).analysis(Analyzers)
      } await
    }
  }

  private def executeBulk(bulkRequest: BulkRequest): Unit = {
    if (bulkRequest.requests.nonEmpty) {
      val bulkResponse = elastic.execute(bulkRequest).await

      if (bulkResponse.result.failures.exists(_.status != 404)) {
        logger.warn(s"Bulk index failed: ${bulkResponse.result.failures.flatMap(_.error).map(_.reason).mkString(", ")}")
        throw new RuntimeException("Bulk request failed")
      }
    }
  }

  private def indexOfComment(topic: Topic, comment: Comment, message: String): IndexRequest = {
    val section = sectionService.getSection(topic.getSectionId)
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userDao.getUserCached(comment.getUserid)
    val topicAuthor = userDao.getUserCached(topic.getAuthorUserId)

    val topicTitle = topic.getTitleUnescaped

    val commentTitle = comment.getTitle

    val title =
      Option(commentTitle)
        .filter(_.nonEmpty)
        .filterNot(_ == topicTitle)
        .filterNot(_.startsWith("Re:"))
        .map(StringEscapeUtils.unescapeHtml4)

    indexInto(MessageIndexType) id comment.getId.toString fields (
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

  private def indexOfTopic(topic: Topic): IndexRequest = {
    val section = sectionService.getSection(topic.getSectionId)
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userDao.getUserCached(topic.getAuthorUserId)

    indexInto(MessageIndexType) id topic.getId.toString fields(
      "section" -> section.getUrlName,
      "topic_author" -> author.getNick,
      "topic_id" -> topic.getId,
      "author" -> author.getNick,
      "group" -> group.getUrlName,
      "title" -> topic.getTitleUnescaped,
      "topic_title" -> topic.getTitleUnescaped,
      "message" -> messageTextService.extractPlainText(msgbaseDao.getMessageText(topic.getId)),
      "postdate" -> new DateTime(topic.getPostdate),
      "tag" -> topicTagService.getTags(topic),
      COLUMN_TOPIC_AWAITS_COMMIT -> topicAwaitsCommit(topic),
      "is_comment" -> false)
  }
}
