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
package ru.org.linux.edithistory

import cats.data.Chain
import org.springframework.stereotype.Service
import ru.org.linux.comment.Comment
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.poll.{Poll, PollDao, PollNotFoundException}
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.tag.{TagName, TagRef, TagService}
import ru.org.linux.topic.{PreparedImage, Topic, TopicTagService}
import ru.org.linux.user.{User, UserService}

import java.util

@Service
class EditHistoryService(topicTagService: TopicTagService, userService: UserService, textService: MessageTextService,
                         msgbaseDao: MsgbaseDao, editHistoryDao: EditHistoryDao, imageDao: ImageDao,
                         imageService: ImageService, pollDao: PollDao) {

  private case class TopicEditHistoryState(message: String, markup: MarkupType, title: String,
                              url: String, linktext: String, tags: util.List[TagRef],
                              minor: Boolean, image: PreparedImage, lastId: Integer,
                              poll: Poll, first: Boolean) {
    def next(dto: EditHistoryRecord): TopicEditHistoryState = {
      val image = if (dto.getOldimage != null) {
        if (dto.getOldimage == 0) {
          null
        } else {
          imageService.prepareImage(imageDao.getImage(dto.getOldimage)).orNull
        }
      } else {
        this.image
      }

      val (message, lastId) = if (dto.getOldmessage != null) {
        (dto.getOldmessage, Integer.valueOf(dto.getId))
      } else {
        (this.message, this.lastId)
      }

      val title = if (dto.getOldtitle != null) {
        dto.getOldtitle
      } else {
        this.title
      }

      val url = if (dto.getOldurl != null) {
        dto.getOldurl
      } else {
        this.url
      }

      val linktext = if (dto.getOldlinktext != null) {
        dto.getOldlinktext
      } else {
        this.linktext
      }

      val tags = if (dto.getOldtags != null) {
        TagService.namesToRefs(TagName.parseAndSanitizeTagsJava(dto.getOldtags))
      } else {
        this.tags
      }

      val minor = if (dto.getOldminor != null) {
        dto.getOldminor.booleanValue()
      } else {
        this.minor
      }

      val poll = if (dto.getOldPoll != null) {
        dto.getOldPoll
      } else {
        this.poll
      }

      this.copy(image = image, message = message, lastId = lastId, title = title, url = url, linktext = linktext,
        tags = tags, minor = minor, poll = poll, first = false)
    }

    def build(dto: EditHistoryRecord): PreparedEditHistory = {
      new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) this.message else null,
        if (dto.getOldtitle != null) this.title else null,
        if (dto.getOldurl != null) this.url else null,
        if (dto.getOldlinktext != null) this.linktext else null,
        if (dto.getOldtags != null) this.tags else null,
        this.first,
        false,
        if (dto.getOldminor != null) this.minor else null,
        if (dto.getOldimage != null && this.image != null) this.image else null,
        this.image == null && dto.getOldimage != null,
        this.markup,
        if (dto.getOldPoll != null) this.poll else null,
        this.lastId)
    }

    def buildLast(topic: Topic): PreparedEditHistory = {
      new PreparedEditHistory(
        textService,
        userService.getUserCached(topic.authorUserId),
        topic.postdate,
        this.message,
        this.title,
        this.url,
        this.linktext,
        if (!tags.isEmpty) this.tags else null,
        false,
        true,
        null,
        this.image,
        false,
        this.markup,
        this.poll,
        this.lastId)
    }
  }

  private object TopicEditHistoryState {
    def fromTopic(topic: Topic): TopicEditHistoryState = {
      val messageText: MessageText = msgbaseDao.getMessageText(topic.id)
      val maybeImage = imageDao.imageForTopic(topic)

      new TopicEditHistoryState(
        message = messageText.text,
        markup = messageText.markup,
        title = topic.title,
        url = topic.url,
        linktext = topic.linktext,
        tags = topicTagService.getTagRefs(topic),
        minor = topic.minor,
        image = maybeImage.flatMap(imageService.prepareImage).orNull,
        lastId = null,
        poll = try {
          pollDao.getPollByTopicId(topic.id)
        } catch {
          case _: PollNotFoundException =>
            null
        },
        first = true)
    }
  }

  private case class CommentEditHistoryState(message: String, markup: MarkupType, title: String, first: Boolean) {
    def next(dto: EditHistoryRecord): CommentEditHistoryState = {
      val message = if (dto.getOldmessage != null) {
        dto.getOldmessage
      } else {
        this.message
      }

      val title = if (dto.getOldtitle != null) {
        dto.getOldtitle
      } else {
        this.title
      }

      this.copy(first = false, message = message, title = title)
    }

    def build(dto: EditHistoryRecord): PreparedEditHistory = {
      new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) this.message else null,
        if (dto.getOldtitle != null) this.title else null,
        null,
        null,
        null,
        this.first,
        false,
        null,
        null,
        false,
        this.markup,
        null,
        null)
    }

    def buildLast(comment: Comment): PreparedEditHistory = {
      new PreparedEditHistory(
        textService,
        userService.getUserCached(comment.userid),
        comment.postdate,
        this.message,
        this.title,
        null,
        null,
        null,
        false,
        true,
        null,
        null,
        false,
        this.markup,
        null,
        null)
    }
  }

  private object CommentEditHistoryState {
    def fromComment(comment: Comment): CommentEditHistoryState = {
      val messageText = msgbaseDao.getMessageText(comment.id)

      new CommentEditHistoryState(
        markup = messageText.markup,
        message = messageText.text,
        title = comment.title,
        first = true)
    }
  }

  /**
   * Получить историю изменений топика
   */
  def prepareEditInfo(topic: Topic): Seq[PreparedEditHistory] = {
    val editInfoDTOs = editHistoryDao.getEditInfo(topic.id, EditHistoryObjectTypeEnum.TOPIC)

    if (editInfoDTOs.nonEmpty) {
      val initial = (TopicEditHistoryState.fromTopic(topic), Chain.empty[PreparedEditHistory])

      val (current, editHistories) = editInfoDTOs.foldLeft(initial) { case ((current, editHistories), dto) =>
        (current.next(dto), editHistories :+ current.build(dto))
      }

      (editHistories :+ current.buildLast(topic)).toVector
    } else {
      Vector.empty
    }
  }

  def prepareEditInfo(comment: Comment): Seq[PreparedEditHistory] = {
    val editInfoDTOs = editHistoryDao.getEditInfo(comment.id, EditHistoryObjectTypeEnum.COMMENT)

    if (editInfoDTOs.nonEmpty) {
      val initial = (CommentEditHistoryState.fromComment(comment), Chain.empty[PreparedEditHistory])

      val (current, editHistories) = editInfoDTOs.foldLeft(initial) { case ((current, editHistories), dto) =>
        (current.next(dto), editHistories :+ current.build(dto))
      }

      (editHistories :+ current.buildLast(comment)).toVector
    } else {
      Vector.empty
    }
  }

  def getEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): collection.Seq[EditHistoryRecord] =
    editHistoryDao.getEditInfo(id, objectTypeEnum)

  def editCount(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Int = {
    // TODO replace with count() SQL query
    editHistoryDao.getEditInfo(id, objectTypeEnum).size
  }

  def insert(editHistoryRecord: EditHistoryRecord): Unit = editHistoryDao.insert(editHistoryRecord)

  def getEditorUsers(message: Topic, editInfoList: collection.Seq[EditHistoryRecord]): Set[User] = {
    val editors = editInfoList.view.filter(_.getEditor != message.authorUserId).map(_.getEditor).toSet

    userService.getUsersCached(editors).toSet
  }

  def editInfoSummary(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Option[EditInfoSummary] = {
    val history = editHistoryDao.getBriefEditInfo(id, objectTypeEnum)

    history.headOption.map(v => EditInfoSummary(history.size, v))
  }

  def getEditHistoryRecord(topic: Topic, recordId: Int): EditHistoryRecord =
    editHistoryDao.getEditRecord(topic.id, recordId, EditHistoryObjectTypeEnum.TOPIC)
}