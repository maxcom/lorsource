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
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.{Poll, PollDao, PollNotFoundException}
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.tag.{TagName, TagService}
import ru.org.linux.topic.{Topic, TopicTagService}
import ru.org.linux.user.{User, UserService}

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

    val messageText = msgbaseDao.getMessageText(topic.id)
    var currentMessage = messageText.text
    val markup = messageText.markup
    var currentTitle = topic.title
    var currentUrl = topic.url
    var currentLinktext = topic.linktext
    var currentTags = topicTagService.getTagRefs(topic)
    var currentMinor = topic.minor
    val maybeImage = imageDao.imageForTopic(topic)

    var currentImage = if (maybeImage != null) {
      imageService.prepareImageJava(maybeImage).orElse(null)
    } else {
      null
    }

    var lastId: Integer = null

    var maybePoll: Poll = try {
      pollDao.getPollByTopicId(topic.id)
    } catch {
      case _: PollNotFoundException =>
        null
    }

    for (i <- editInfoDTOs.indices) {
      val dto = editInfoDTOs(i)

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) currentMessage else null,
        if (dto.getOldtitle != null) currentTitle else null,
        if (dto.getOldurl != null) currentUrl else null,
        if (dto.getOldlinktext != null) currentLinktext else null,
        if (dto.getOldtags != null) currentTags else null,
        i == 0,
        false,
        if (dto.getOldminor != null) currentMinor else null,
        if (dto.getOldimage != null && currentImage != null) currentImage else null,
        currentImage == null && dto.getOldimage != null,
        markup,
        if (dto.getOldPoll != null) maybePoll else null,
        lastId))

      if (dto.getOldimage != null) {
        if (dto.getOldimage == 0) {
          currentImage = null
        } else {
          currentImage = imageService.prepareImageJava(imageDao.getImage(dto.getOldimage)).orElse(null)
        }
      }

      if (dto.getOldmessage != null) {
        currentMessage = dto.getOldmessage
        lastId = dto.getId
      }

      if (dto.getOldtitle != null) {
        currentTitle = dto.getOldtitle
      }

      if (dto.getOldurl != null) {
        currentUrl = dto.getOldurl
      }

      if (dto.getOldlinktext != null) {
        currentLinktext = dto.getOldlinktext
      }

      if (dto.getOldtags != null) {
        currentTags = TagService.namesToRefs(TagName.parseAndSanitizeTagsJava(dto.getOldtags))
      }

      if (dto.getOldminor != null) {
        currentMinor = dto.getOldminor
      }

      if (dto.getOldPoll != null) {
        maybePoll = dto.getOldPoll
      }
    }

    if (editInfoDTOs.nonEmpty) {
      if (currentTags.isEmpty) {
        currentTags = null
      }

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(topic.authorUserId),
        topic.postdate,
        currentMessage,
        currentTitle,
        currentUrl,
        currentLinktext,
        currentTags,
        false,
        true,
        null,
        currentImage,
        false,
        markup,
        maybePoll,
        lastId))
    }

    editHistories.result()
  }

  def prepareEditInfo(comment: Comment): Seq[PreparedEditHistory] = {
    val editInfoDTOs = editHistoryDao.getEditInfo(comment.id, EditHistoryObjectTypeEnum.COMMENT)

    val editHistories = Vector.newBuilder[PreparedEditHistory]
    editHistories.sizeHint(editInfoDTOs.size)

    val messageText = msgbaseDao.getMessageText(comment.id)
    val markup = messageText.markup

    var currentMessage = messageText.text
    var currentTitle = comment.title

    for (i <- editInfoDTOs.indices) {
      val dto = editInfoDTOs(i)

      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(dto.getEditor),
        dto.getEditdate,
        if (dto.getOldmessage != null) currentMessage else null,
        if (dto.getOldtitle != null) currentTitle else null,
        null,
        null,
        null,
        i == 0,
        false,
        null,
        null,
        false,
        markup,
        null,
        null))

      if (dto.getOldmessage != null) {
        currentMessage = dto.getOldmessage
      }

      if (dto.getOldtitle != null) {
        currentTitle = dto.getOldtitle
      }
    }

    if (!editInfoDTOs.isEmpty) {
      editHistories.addOne(new PreparedEditHistory(
        textService,
        userService.getUserCached(comment.userid),
        comment.postdate,
        currentMessage,
        currentTitle,
        null,
        null,
        null,
        false,
        true,
        null,
        null,
        false,
        markup,
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
    val editors = getEditors(message, editInfoList)

    userService.getUsersCached(editors).toSet
  }

  def getEditors(message: Topic, editInfoList: collection.Seq[EditHistoryRecord]): Set[Int] = {
    editInfoList.view.filter(_.getEditor != message.authorUserId).map(_.getEditor).toSet
  }

  def editInfoSummary(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Option[EditInfoSummary] = {
    val history = editHistoryDao.getBriefEditInfo(id, objectTypeEnum)

    history.headOption.map(v => EditInfoSummary(history.size, v))
  }

  def getEditHistoryRecord(topic: Topic, recordId: Int): EditHistoryRecord =
    editHistoryDao.getEditRecord(topic.id, recordId, EditHistoryObjectTypeEnum.TOPIC)
}