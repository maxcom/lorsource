/*
 * Copyright 1998-2015 Linux.org.ru
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
import com.sksamuel.elastic4s.{BulkDefinition, ElasticClient}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringEscapeUtils
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.org.linux.comment.{Comment, CommentList, CommentService}
import ru.org.linux.group.GroupDao
import ru.org.linux.search.SearchQueueListener._
import ru.org.linux.section.SectionService
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.topic.{Topic, TopicDao, TopicTagService}
import ru.org.linux.user.UserDao
import ru.org.linux.util.bbcode.LorCodeService

import scala.collection.JavaConverters._
import scala.concurrent.duration.Deadline

@Component
object SearchQueueListener {
  val MessageIndex = "messages"
  val MessageType = "message"

  val COLUMN_TOPIC_AWAITS_COMMIT = "topic_awaits_commit"
}

@Component
class SearchQueueListener @Autowired() (
  commentService: CommentService,
  msgbaseDao: MsgbaseDao,
  javaElastic: Client,
  topicDao: TopicDao,
  groupDao: GroupDao,
  sectionService: SectionService,
  userDao: UserDao,
  lorCodeService: LorCodeService,
  topicTagService: TopicTagService
) extends StrictLogging {
  private val elastic = ElasticClient.fromClient(javaElastic)

  private var mappingsSet = false

  def handleMessage(msgUpdate: SearchQueueSender.UpdateMessage):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    logger.info(s"Indexing ${msgUpdate.getMsgid}")

    reindexMessage(msgUpdate.getMsgid, msgUpdate.isWithComments)
  }

  private def topicAwaitsCommit(msg: Topic) = {
    val section = sectionService.getSection(msg.getSectionId)

    section.isPremoderated && !msg.isCommited
  }

  private def isTopicSearchable(msg: Topic) = !msg.isDeleted && !msg.isDraft

  private def reindexMessage(msgid: Int, withComments: Boolean):Unit = {
    val msg = topicDao.getById(msgid)

    if (isTopicSearchable(msg)) {
      updateMessage(msg)
      if (withComments) {
        val commentList = commentService.getCommentList(msg, true)
        reindexComments(msg, commentList)
      }
    } else {
      val topicDelete = delete id Integer.toString(msg.getId) from MessageIndex -> MessageType

      val commentsDelete = if (withComments) {
        val comments = commentService.getCommentList(msg, true).getList.asScala

        comments.map {
          comment ⇒ delete id Integer.toString(comment.getId) from MessageIndex -> MessageType
        }
      } else Seq.empty

      executeBulk(bulk(topicDelete +: commentsDelete))
    }
  }

  private def executeBulk(bulkRequest: BulkDefinition):Unit = {
    if (bulkRequest.requests.nonEmpty) {
      val bulkResponse = elastic.execute(bulkRequest).await

      if (bulkResponse.hasFailures) {
        logger.warn(s"Bulk index failed: ${bulkResponse.buildFailureMessage}")
        throw new RuntimeException("Bulk request failed")
      }
    }
  }

  def handleMessage(msgUpdate: SearchQueueSender.UpdateComments):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    logger.info(s"Indexing comments ${msgUpdate.getMsgids}")

    if (msgUpdate.getMsgids.contains(0)) {
      logger.warn("Skipping MSGID=0!!!")
    }

    val requests = for (msgid <- msgUpdate.getMsgids.asScala if msgid!=0) yield {
      val comment = commentService.getById(msgid)
      val topic = topicDao.getById(comment.getTopicId)

      if (!isTopicSearchable(topic) || comment.isDeleted) {
        logger.info(s"Deleting comment ${comment.getId}")
        delete id Integer.toString(comment.getId) from MessageIndex -> MessageType
      } else {
        val message = lorCodeService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
        processComment(topic, comment, message)
      }
    }

    executeBulk(bulk(requests))
  }

  def handleMessage(msgUpdate: SearchQueueSender.UpdateMonth):Unit = {
    if (!mappingsSet) {
      createIndex()
    }

    val month = msgUpdate.getMonth
    val year = msgUpdate.getYear

    logger.info(s"Indexing month $year/$month")

    val startTime = Deadline.now
    val topicIds = topicDao.getMessageForMonth(year, month)
    import scala.collection.JavaConversions._

    for (topicId <- topicIds) {
      reindexMessage(topicId, withComments = true)
    }

    val endTime = Deadline.now - startTime

    logger.info(s"Reindex month $year/$month done, ${endTime.toMillis} millis")
  }

  private def updateMessage(topic: Topic):Unit = {
    val section = sectionService.getSection(topic.getSectionId)
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userDao.getUserCached(topic.getUid)

    elastic.execute {
      index into MessageIndex -> MessageType id Integer.toString(topic.getId) fields (
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
    }.await
  }

  private def reindexComments(topic: Topic, comments: CommentList):Unit = {
    val requests = for (comment <- comments.getList.asScala) yield {
      if (comment.isDeleted) {
        delete id Integer.toString(comment.getId) from MessageIndex -> MessageType
      } else {
        val message = lorCodeService.extractPlainText(msgbaseDao.getMessageText(comment.getId))
        processComment(topic, comment, message)
      }
    }

    executeBulk(bulk(requests))
  }

  private def processComment(topic: Topic, comment: Comment, message: String) = {
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
        .map(StringEscapeUtils.unescapeHtml)

    index into MessageIndex -> MessageType id Integer.toString(comment.getId) fields (
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
      "is_comment" ->true) ++ title.map("title" -> _)
    )
  }

  private def createIndex():Unit = {
    if (!elastic.exists(MessageIndex).await.isExists) {
      val mappingSource = IOUtils.toString(getClass.getClassLoader.getResource("es-mapping.json"))

      elastic.java
        .admin()
        .indices()
        .prepareCreate(MessageIndex)
        .setSource(mappingSource)
        .execute()
        .actionGet()
    }

    mappingsSet = true
  }
}