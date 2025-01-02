/*
 * Copyright 1998-2025 Linux.org.ru
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
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.topic.{PreparedImage, Topic, TopicTagService}
import ru.org.linux.user.{User, UserService}

import java.sql.Timestamp
import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

@Service
class EditHistoryService(topicTagService: TopicTagService, userService: UserService, textService: MessageTextService,
                         msgbaseDao: MsgbaseDao, editHistoryDao: EditHistoryDao, imageDao: ImageDao,
                         imageService: ImageService, pollDao: PollDao) {

  private case class TopicEditHistoryState(message: String, markup: MarkupType, title: String,
                              url: String, linktext: String, tags: util.List[TagRef],
                              minor: Boolean, image: PreparedImage, lastId: Integer,
                              poll: Poll, first: Boolean, additionalImages: Seq[PreparedImage]) {
    def next(dto: EditHistoryRecord): TopicEditHistoryState = {
      val image = dto.oldimage.map { oldimage =>
        if (oldimage == 0) {
          null
        } else {
          imageService.prepareImage(imageDao.getImage(oldimage)).orNull
        }
      }.getOrElse(this.image)

      val additionalImages = dto.oldaddimages.map { additionalImages =>
        additionalImages.map(imageDao.getImage).flatMap(imageService.prepareImage)
      }.getOrElse(this.additionalImages)

      val (message, lastId) = dto.oldmessage.map { oldmessage =>
        (oldmessage, Integer.valueOf(dto.id))
      }.getOrElse((this.message, this.lastId))

      val title = dto.oldtitle.getOrElse(this.title)
      val url = dto.oldurl.getOrElse(this.url)
      val linktext = dto.oldlinktext.getOrElse(this.linktext)
      val tags = dto.oldtags.map(_.map(TagService.tagRef).asJava).getOrElse(this.tags)
      val minor = dto.oldminor.getOrElse(this.minor)
      val poll = dto.oldPoll.getOrElse(this.poll)

      this.copy(image = image, message = message, lastId = lastId, title = title, url = url, linktext = linktext,
        tags = tags, minor = minor, poll = poll, first = false, additionalImages = additionalImages)
    }

    def build(dto: EditHistoryRecord): PreparedEditHistory = {
      val imageDeleted = this.image == null && dto.oldimage.isDefined

      val addedImages = if (dto.oldaddimages.isDefined) {
        this.additionalImages.filterNot(img => dto.oldaddimages.get.contains(img.image.id)).asJava
      } else {
        null
      }

      val removedImages = if (dto.oldaddimages.isDefined) {
        dto.oldaddimages.get.filterNot(this.additionalImages.map(_.image.id).contains).map(imageDao.getImage).flatMap(imageService.prepareImage).asJava
      } else {
        null
      }

      new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.editor),
        new Timestamp(dto.editdate.toEpochMilli),
        if (dto.oldmessage.isDefined) this.message else null,
        if (dto.oldtitle.isDefined) this.title else null,
        if (dto.oldurl.isDefined) this.url else null,
        if (dto.oldlinktext.isDefined) this.linktext else null,
        if (dto.oldtags.isDefined) this.tags else null,
        this.first,
        false,
        if (dto.oldminor.isDefined) this.minor else null,
        if (dto.oldimage.isDefined && this.image != null) this.image else null,
        imageDeleted,
        this.markup,
        if (dto.oldPoll.isDefined) this.poll else null,
        this.lastId,
        addedImages,
        removedImages)
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
        this.lastId,
        this.additionalImages.asJava,
        null)
    }
  }

  private object TopicEditHistoryState {
    def fromTopic(topic: Topic): TopicEditHistoryState = {
      val messageText: MessageText = msgbaseDao.getMessageText(topic.id)
      val images = imageService.allImagesForTopic(topic)
      val maybeImage = images.find(_.main)

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
        first = true,
        additionalImages = images.filterNot(_.main).flatMap(imageService.prepareImage))
    }
  }

  private case class CommentEditHistoryState(message: String, markup: MarkupType, title: String, first: Boolean) {
    def next(dto: EditHistoryRecord): CommentEditHistoryState = {
      val message = dto.oldmessage.getOrElse(this.message)
      val title = dto.oldtitle.getOrElse(this.title)

      this.copy(first = false, message = message, title = title)
    }

    def build(dto: EditHistoryRecord): PreparedEditHistory = {
      new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.editor),
        new Timestamp(dto.editdate.toEpochMilli),
        if (dto.oldmessage.isDefined) this.message else null,
        if (dto.oldtitle.isDefined) this.title else null,
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
        null,
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
        null,
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
    val editors = editInfoList.view.map(_.editor).filterNot(_ == message.authorUserId).toSet

    userService.getUsersCached(editors).toSet
  }

  def editInfoSummary(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Option[EditInfoSummary] = {
    val history = editHistoryDao.getBriefEditInfo(id, objectTypeEnum)

    history.headOption.map(v => EditInfoSummary(history.size, v))
  }

  def getEditHistoryRecord(topic: Topic, recordId: Int): EditHistoryRecord =
    editHistoryDao.getEditRecord(topic.id, recordId, EditHistoryObjectTypeEnum.TOPIC)
}