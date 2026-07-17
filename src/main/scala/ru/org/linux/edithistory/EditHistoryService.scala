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
package ru.org.linux.edithistory

import cats.data.Chain
import org.springframework.stereotype.Service
import ru.org.linux.comment.Comment
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.msgbase.{MessageText, MsgbaseDao}
import ru.org.linux.poll.{Poll, PollDao, PollNotFoundException}
import ru.org.linux.scalikejdbc.Transaction
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.topic.{PreparedImage, Topic, TopicTagService}
import ru.org.linux.user.{User, UserService}

import java.sql.Timestamp
import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

@Service
class EditHistoryService(
    topicTagService: TopicTagService,
    userService: UserService,
    textService: MessageTextService,
    msgbaseDao: MsgbaseDao,
    editHistoryDao: EditHistoryDao,
    imageDao: ImageDao,
    imageService: ImageService,
    pollDao: PollDao):

  private case class TopicEditHistoryState(
      message: String,
      markup: MarkupType,
      title: String,
      url: String,
      linktext: String,
      tags: util.List[TagRef],
      minor: Boolean,
      lastId: Integer,
      poll: Poll,
      first: Boolean,
      images: Seq[PreparedImage]):
    def next(dto: EditHistoryRecord): TopicEditHistoryState =
      val images = dto
        .oldaddimages
        .map { images =>
          images.map(imageDao.getImage).flatMap(imageService.prepareImage)
        }
        .getOrElse(this.images)

      val (message, lastId) = dto
        .oldmessage
        .map { oldmessage =>
          (oldmessage, Integer.valueOf(dto.id))
        }
        .getOrElse((this.message, this.lastId))

      val title = dto.oldtitle.getOrElse(this.title)
      val url = dto.oldurl.getOrElse(this.url)
      val linktext = dto.oldlinktext.getOrElse(this.linktext)
      val tags = dto.oldtags.map(_.map(TagService.tagRef).asJava).getOrElse(this.tags)
      val minor = dto.oldminor.getOrElse(this.minor)
      val poll = dto.oldPoll.getOrElse(this.poll)

      this.copy(
        message = message,
        lastId = lastId,
        title = title,
        url = url,
        linktext = linktext,
        tags = tags,
        minor = minor,
        poll = poll,
        first = false,
        images = images
      )

    def build(dto: EditHistoryRecord): PreparedEditHistory =
      val (addedImages, removedImages, addedMainImage, removedMainImage) =
        if dto.oldaddimages.isDefined then
          val currentIds = this.images.map(_.image.id).toSet

          val added  = this.images.filterNot(img => dto.oldaddimages.get.contains(img.image.id))
          val removed = dto
            .oldaddimages
            .get
            .filterNot(currentIds.contains)
            .map(imageDao.getImage)
            .flatMap(imageService.prepareImage)

          (added.asJava, removed.asJava, null, null)

        else if dto.legacyMainImage.contains(0) then
          val addedMain =
            this.images.headOption.map(_ => this.images.take(1).toList.asJava).orNull

          (null, null, addedMain, null)

        else if dto.legacyMainImage.isDefined && dto.legacyMainImage.get > 0 then
          val xId = dto.legacyMainImage.get
          val removedMain =
            if this.images.exists(_.image.id == xId) then
              null
            else
              Seq(imageDao.getImage(xId)).flatMap(imageService.prepareImage).asJava

          (null, null, null, removedMain)

        else
          (null, null, null, null)

      val (message, restoreFrom) =
        if dto.oldmessage.isDefined then
          (textService.renderCommentText(MessageText(this.message, this.markup), false), this.lastId)
        else
          (null, null)

      PreparedEditHistory(
        original = false,
        editor = userService.getUserCached(dto.editor),
        message = message,
        current = this.first,
        title =
          if dto.oldtitle.isDefined then
            this.title
          else
            null
        ,
        tags =
          if dto.oldtags.isDefined then
            this.tags
          else
            null
        ,
        url =
          if dto.oldurl.isDefined then
            this.url
          else
            null
        ,
        linktext =
          if dto.oldlinktext.isDefined then
            this.linktext
          else
            null
        ,
        minor =
          if dto.oldminor.isDefined then
            this.minor
          else
            null
        ,
        editDate = new Timestamp(dto.editdate.toEpochMilli),
        poll =
          if dto.oldPoll.isDefined then
            this.poll
          else
            null
        ,
        restoreFrom = restoreFrom,
        addedImages = addedImages,
        removedImages = removedImages,
        addedMainImage = addedMainImage,
        removedMainImage = removedMainImage
      )

    def buildLast(topic: Topic): PreparedEditHistory =
      PreparedEditHistory(
        original = true,
        editor = userService.getUserCached(topic.authorUserId),
        message = textService.renderCommentText(MessageText(this.message, this.markup), false),
        current = false,
        title = this.title,
        tags =
          if !tags.isEmpty then
            this.tags
          else
            null
        ,
        url = this.url,
        linktext = this.linktext,
        minor = null,
        editDate = topic.postdate,
        poll = this.poll,
        restoreFrom = this.lastId,
        addedImages = this.images.asJava,
        removedImages = null,
        addedMainImage = null,
        removedMainImage = null
      )

  private object TopicEditHistoryState:
    def fromTopic(topic: Topic): TopicEditHistoryState =
      val messageText: MessageText = msgbaseDao.getMessageText(topic.id)
      val images = imageService.allImagesForTopic(topic)

      TopicEditHistoryState(
        message = messageText.text,
        markup = messageText.markup,
        title = topic.title,
        url = topic.url,
        linktext = topic.linktext,
        tags = topicTagService.getTagRefs(topic).asJava,
        minor = topic.minor,
        lastId = null,
        poll =
          try
            pollDao.getPollByTopicId(topic.id)
          catch
            case _: PollNotFoundException =>
              null
        ,
        first = true,
        images = images.flatMap(imageService.prepareImage)
      )

  private case class CommentEditHistoryState(message: String, markup: MarkupType, title: String, first: Boolean):
    def next(dto: EditHistoryRecord): CommentEditHistoryState =
      val message = dto.oldmessage.getOrElse(this.message)
      val title = dto.oldtitle.getOrElse(this.title)

      this.copy(first = false, message = message, title = title)

    def build(dto: EditHistoryRecord): PreparedEditHistory =
      val message =
        if dto.oldmessage.isDefined then
          textService.renderCommentText(MessageText(this.message, this.markup), false)
        else
          null

      PreparedEditHistory(
        original = false,
        editor = userService.getUserCached(dto.editor),
        message = message,
        current = this.first,
        title =
          if dto.oldtitle.isDefined then
            this.title
          else
            null
        ,
        tags = null,
        url = null,
        linktext = null,
        minor = null,
        editDate = new Timestamp(dto.editdate.toEpochMilli),
        poll = null,
        restoreFrom = null,
        addedImages = null,
        removedImages = null,
        addedMainImage = null,
        removedMainImage = null
      )

    def buildLast(comment: Comment): PreparedEditHistory =
      PreparedEditHistory(
        original = true,
        editor = userService.getUserCached(comment.userid),
        message = textService.renderCommentText(MessageText(this.message, this.markup), false),
        current = false,
        title = this.title,
        tags = null,
        url = null,
        linktext = null,
        minor = null,
        editDate = comment.postdate,
        poll = null,
        restoreFrom = null,
        addedImages = null,
        removedImages = null,
        addedMainImage = null,
        removedMainImage = null
      )

  private object CommentEditHistoryState:
    def fromComment(comment: Comment): CommentEditHistoryState =
      val messageText = msgbaseDao.getMessageText(comment.id)

      CommentEditHistoryState(
        markup = messageText.markup,
        message = messageText.text,
        title = comment.title,
        first = true)

  /** Получить историю изменений топика
    */
  def prepareEditInfo(topic: Topic): Seq[PreparedEditHistory] =
    val editInfoDTOs = editHistoryDao.getEditInfo(topic.id, EditHistoryObjectTypeEnum.TOPIC)

    if editInfoDTOs.nonEmpty then
      val initial = (TopicEditHistoryState.fromTopic(topic), Chain.empty[PreparedEditHistory])

      val (current, editHistories) =
        editInfoDTOs.foldLeft(initial) { case ((current, editHistories), dto) =>
          (current.next(dto), editHistories :+ current.build(dto))
        }

      (editHistories :+ current.buildLast(topic)).toVector
    else
      Vector.empty

  def prepareEditInfo(comment: Comment): Seq[PreparedEditHistory] =
    val editInfoDTOs = editHistoryDao.getEditInfo(comment.id, EditHistoryObjectTypeEnum.COMMENT)

    if editInfoDTOs.nonEmpty then
      val initial = (CommentEditHistoryState.fromComment(comment), Chain.empty[PreparedEditHistory])

      val (current, editHistories) =
        editInfoDTOs.foldLeft(initial) { case ((current, editHistories), dto) =>
          (current.next(dto), editHistories :+ current.build(dto))
        }

      (editHistories :+ current.buildLast(comment)).toVector
    else
      Vector.empty

  def getEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Seq[EditHistoryRecord] =
    editHistoryDao.getEditInfo(id, objectTypeEnum)

  def editCount(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Int =
    // TODO replace with count() SQL query
    editHistoryDao.getEditInfo(id, objectTypeEnum).size

  def insert(editHistoryRecord: EditHistoryRecord)(using Transaction): Unit = editHistoryDao.insert(editHistoryRecord)

  def getEditorUsers(message: Topic, editInfoList: Seq[EditHistoryRecord]): Set[User] =
    val editors = editInfoList.view.map(_.editor).filterNot(_ == message.authorUserId).toSet

    userService.getUsersCached(editors).toSet

  def editInfoSummary(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Option[EditInfoSummary] =
    val history = editHistoryDao.getBriefEditInfo(id, objectTypeEnum)

    history.headOption.map(v => EditInfoSummary(history.size, v))

  def getEditHistoryRecord(topic: Topic, recordId: Int): EditHistoryRecord =
    editHistoryDao.getEditRecord(topic.id, recordId, EditHistoryObjectTypeEnum.TOPIC)
