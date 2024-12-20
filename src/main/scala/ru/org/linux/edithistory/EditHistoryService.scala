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

import org.springframework.stereotype.Service
import ru.org.linux.comment.Comment
import ru.org.linux.edithistory.EditHistoryService.EditHistoryState
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.poll.{Poll, PollDao, PollNotFoundException}
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.tag.{TagName, TagRef, TagService}
import ru.org.linux.topic.{PreparedImage, Topic, TopicTagService}
import ru.org.linux.user.{User, UserService}

import java.util

object EditHistoryService {
  class EditHistoryState(var message: String, val markup: MarkupType, var title: String,
                         var url: String, var linktext: String, var tags: util.List[TagRef],
                         var minor: Boolean, var image: PreparedImage, var lastId: Integer,
                         var poll: Poll)
}

@Service
class EditHistoryService(topicTagService: TopicTagService, userService: UserService, textService: MessageTextService,
                         msgbaseDao: MsgbaseDao, editHistoryDao: EditHistoryDao, imageDao: ImageDao,
                         imageService: ImageService, pollDao: PollDao) {
  /**
   * Получить историю изменений топика
   */
  def prepareEditInfo(topic: Topic): Seq[PreparedEditHistory] = {
    val editInfoDTOs = editHistoryDao.getEditInfo(topic.id, EditHistoryObjectTypeEnum.TOPIC)

    val editHistories = Vector.newBuilder[PreparedEditHistory]
    editHistories.sizeHint(editInfoDTOs.size)

    val current = stateFromTopic(topic)

    for (i <- editInfoDTOs.indices) {
      val dto = editInfoDTOs(i)

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) current.message else null,
        if (dto.getOldtitle != null) current.title else null,
        if (dto.getOldurl != null) current.url else null,
        if (dto.getOldlinktext != null) current.linktext else null,
        if (dto.getOldtags != null) current.tags else null,
        i == 0,
        false,
        if (dto.getOldminor != null) current.minor else null,
        if (dto.getOldimage != null && current.image != null) current.image else null,
        current.image == null && dto.getOldimage != null,
        current.markup,
        if (dto.getOldPoll != null) current.poll else null,
        current.lastId))

      if (dto.getOldimage != null) {
        if (dto.getOldimage == 0) {
          current.image = null
        } else {
          current.image = imageService.prepareImage(imageDao.getImage(dto.getOldimage)).orNull
        }
      }

      if (dto.getOldmessage != null) {
        current.message = dto.getOldmessage
        current.lastId = dto.getId
      }

      if (dto.getOldtitle != null) {
        current.title = dto.getOldtitle
      }

      if (dto.getOldurl != null) {
        current.url = dto.getOldurl
      }

      if (dto.getOldlinktext != null) {
        current.linktext = dto.getOldlinktext
      }

      if (dto.getOldtags != null) {
        current.tags = TagService.namesToRefs(TagName.parseAndSanitizeTagsJava(dto.getOldtags))
      }

      if (dto.getOldminor != null) {
        current.minor = dto.getOldminor
      }

      if (dto.getOldPoll != null) {
        current.poll = dto.getOldPoll
      }
    }

    if (editInfoDTOs.nonEmpty) {
      if (current.tags.isEmpty) {
        current.tags = null
      }

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(topic.authorUserId),
        topic.postdate,
        current.message,
        current.title,
        current.url,
        current.linktext,
        current.tags,
        false,
        true,
        null,
        current.image,
        false,
        current.markup,
        current.poll,
        current.lastId))
    }

    editHistories.result()
  }

  def prepareEditInfo(comment: Comment): Seq[PreparedEditHistory] = {
    val editInfoDTOs = editHistoryDao.getEditInfo(comment.id, EditHistoryObjectTypeEnum.COMMENT)

    val editHistories = Vector.newBuilder[PreparedEditHistory]
    editHistories.sizeHint(editInfoDTOs.size)

    val current = stateFromComment(comment)

    for (i <- editInfoDTOs.indices) {
      val dto = editInfoDTOs(i)

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) current.message else null,
        if (dto.getOldtitle != null) current.title else null,
        null,
        null,
        null,
        i == 0,
        false,
        null,
        null,
        false,
        current.markup,
        null,
        null))

      if (dto.getOldmessage != null) {
        current.message = dto.getOldmessage
      }

      if (dto.getOldtitle != null) {
        current.title = dto.getOldtitle
      }
    }

    if (editInfoDTOs.nonEmpty) {
      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(comment.userid),
        comment.postdate,
        current.message,
        current.title,
        null,
        null,
        null,
        false,
        true,
        null,
        null,
        false,
        current.markup,
        null,
        null))
    }

    editHistories.result()
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

  private def stateFromTopic(topic: Topic): EditHistoryState = {
    val messageText: MessageText = msgbaseDao.getMessageText(topic.id)
    val maybeImage = imageDao.imageForTopic(topic)

    new EditHistoryState(
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
      })
  }

  private def stateFromComment(comment: Comment): EditHistoryState = {
    val messageText = msgbaseDao.getMessageText(comment.id)

    new EditHistoryState(
      markup = messageText.markup,
      message = messageText.text,
      title = comment.title,
      url = null,
      linktext = null,
      tags = null,
      minor = false,
      image = null,
      lastId = null,
      poll = null)
  }
}