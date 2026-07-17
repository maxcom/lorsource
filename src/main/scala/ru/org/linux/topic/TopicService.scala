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
package ru.org.linux.topic

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.apache.pekko.actor.typed.ActorRef
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo}
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryObjectTypeEnum, EditHistoryRecord}
import ru.org.linux.gallery.{Image, ImageDao, ImageService, UploadedImagePreview}
import ru.org.linux.group.{Group, GroupPermissionService}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.msgbase.{MessageText, MsgbaseDao}
import ru.org.linux.poll.{PollDao, PollVariant}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.rights.AddTopicChecker
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.spring.SiteConfig
import ru.org.linux.tag.TagName
import ru.org.linux.user.*
import ru.org.linux.util.LorHttpUtils

import java.io.File
import java.time.{Instant, OffsetDateTime}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object TopicService {
  private def sendTagEventsNeeded(section: Section, oldMsg: Topic, commit: Boolean): Boolean = {
    val needCommit = section.premoderated && !oldMsg.commited

    val fresh = oldMsg.getEffectiveDate.isAfter(OffsetDateTime.now.minusMonths(1).toInstant)

    commit || (!needCommit && fresh)
  }
}

@Service
class TopicService(topicDao: TopicDao, msgbaseDao: MsgbaseDao, sectionService: SectionService,
                   imageService: ImageService, pollDao: PollDao, userEventService: UserEventService,
                   topicTagService: TopicTagService, userService: UserService, userTagService: UserTagService,
                   textService: MessageTextService, editHistoryDao: EditHistoryDao,
                   imageDao: ImageDao, siteConfig: SiteConfig, permissionService: GroupPermissionService,
                   springDB: SpringDB, addTopicChecker: AddTopicChecker,
                   @Qualifier("realtimeHubWS")
                   realtimeHubWS: ActorRef[RealtimeEventHub.Protocol],
                   searchQueueSender: SearchQueueSender) extends StrictLogging {

  def addMessage(request: HttpServletRequest, form: AddTopicRequest, message: MessageText, group: Group, user: User,
                 images: Seq[UploadedImagePreview],
                 previewMsg: Topic): (Int, Set[Int]) = springDB.localTx {
    val section = sectionService.getSection(group.sectionId)

    if (section.imagepost && images.isEmpty) {
      throw new ScriptErrorException("scrn is empty?!")
    }

    val msgid = topicDao.saveNewMessage(previewMsg, user, request.getHeader("User-Agent"), group)

    msgbaseDao.saveNewMessage(message, msgid)

    images.foreach { imagePreview =>
      imageService.saveImage(imagePreview, msgid)
    }

    if (section.isPollPostAllowed) {
      pollDao.createPoll(form.poll.toSeq, form.multiSelect, msgid)
    }

    val tags = TagName.parseAndSanitizeTags(form.tags)

    topicTagService.updateTags(msgid, tags)

    val notified = if (!previewMsg.draft) {
      if (section.premoderated) {
        sendEvents(message, msgid, Seq.empty, user.id)
      } else {
        sendEvents(message, msgid, tags, user.id)
      }
    } else {
      Set.empty[Int]
    }

    val logmessage = s"Написана тема $msgid ${LorHttpUtils.getRequestIP(request)}"

    logger.info(logmessage)

    (msgid, notified)
  }

  /**
   * Отправляет уведомления типа REF (ссылка на пользователя) и TAG (уведомление по тегу)
   *
   * @param message текст сообщения
   * @param msgid   идентификатор сообщения
   * @param author  автор сообщения (ему не будет отправлено уведомление)
   */
  private def sendEvents(message: MessageText, msgid: Int, tags: Seq[String], author: Int): Set[Int] = {
    val notifiedUsers = userEventService.getNotifiedUsers(msgid)

    var userRefs = textService.mentions(message)
    userRefs = userRefs.filterNot(p => userService.isIgnoring(p.id, author))

    // оповещение пользователей по тегам
    val userIdListByTags = userTagService.getUserIdListByTags(author, tags)
    val userRefIds = userRefs.view.map(_.id).filterNot(notifiedUsers.contains).toSet

    // Не оповещать пользователей, которые ранее были оповещены через упоминание
    val tagUsers = userIdListByTags.filterNot(u => userRefIds.contains(u) || notifiedUsers.contains(u))

    userEventService.addUserRefEvent(userRefIds, msgid)
    userEventService.addUserTagEvent(tagUsers, msgid)

    userRefIds ++ tagUsers
  }

  private def modifyTopic(newMsg: Topic, oldMsg: Topic, user: User, newTags: Option[Seq[String]], newText: MessageText,
                          pollVariants: Option[Seq[PollVariant]], multiselect: Boolean,
                          images: Seq[UploadedImagePreview])(using Transaction): Boolean = {
    var editHistoryRecord = EditHistoryRecord(
      msgid = oldMsg.id,
      editor = user.id,
      objectType = EditHistoryObjectTypeEnum.TOPIC)

    val oldText = msgbaseDao.getMessageText(oldMsg.id).text
    val oldImages = imageService.allImagesForTopic(oldMsg)

    var modified = false

    if (!(oldText == newText.text)) {
      msgbaseDao.updateMessage(oldMsg.id, newText.text)

      editHistoryRecord = editHistoryRecord.copy(oldmessage = Some(oldText))
      modified = true
    }

    if (!(oldMsg.title == newMsg.title)) {
      topicDao.updateTitle(oldMsg.id, newMsg.title)

      editHistoryRecord = editHistoryRecord.copy(oldtitle = Some(oldMsg.title))
      modified = true
    }

    if (!TopicDao.equalStrings(oldMsg.linktext, newMsg.linktext)) {
      topicDao.updateLinktext(oldMsg.id, newMsg.linktext)

      editHistoryRecord = editHistoryRecord.copy(oldlinktext = Some(oldMsg.linktext))
      modified = true
    }

    if (!TopicDao.equalStrings(oldMsg.url, newMsg.url)) {
      topicDao.updateUrl(oldMsg.id, newMsg.url)

      editHistoryRecord = editHistoryRecord.copy(oldurl = Some(oldMsg.url))
      modified = true
    }

    if (oldMsg.minor != newMsg.minor) {
      topicDao.setMinor(oldMsg.id, newMsg.minor)

      editHistoryRecord = editHistoryRecord.copy(oldminor = Some(oldMsg.minor))
      modified = true
    }

    newTags.foreach { newTags =>
      val oldTags = topicTagService.getTags(newMsg)
      val modifiedTags = topicTagService.updateTags(newMsg.id, newTags)

      if (modifiedTags) {
        editHistoryRecord = editHistoryRecord.copy(oldtags = Some(oldTags))
        modified = true
      }
    }

    images.foreach { imagePreview =>
      imageService.saveImage(imagePreview, oldMsg.id)
      editHistoryRecord = editHistoryRecord.copy(oldaddimages = Some(oldImages.map(_.id)))

      modified = true
    }

    pollVariants.foreach { newPollVariants =>
      val oldPoll = pollDao.getPollByTopicId(oldMsg.id)

      if (pollDao.updatePoll(oldPoll, newPollVariants, multiselect)) {
        editHistoryRecord = editHistoryRecord.copy(oldPoll = Some(oldPoll))
        modified = true
      }
    }

    if (modified) {
      topicDao.updateLastmod(oldMsg.id)
      editHistoryDao.insert(editHistoryRecord)
    }

    modified
  }

  def updateAndCommit(newMsg: Topic, oldMsg: Topic, user: User, newTags: Option[Seq[String]],
                      newText: MessageText, commit: Boolean, publish: Boolean, changeGroupId: Option[Int], bonus: Int,
                      pollVariants: Option[Seq[PollVariant]], multiselect: Boolean,
                      editorBonus: Map[User, Int], images: Seq[UploadedImagePreview]): Boolean = {
    val (modified, users) = springDB.localTx {
      val modified = modifyTopic(newMsg = newMsg, oldMsg = oldMsg, user = user, newTags = newTags, newText = newText,
        pollVariants = pollVariants, multiselect = multiselect, images = images)

      val notified = if (!newMsg.draft && !newMsg.expired) {
        val section = sectionService.getSection(oldMsg.sectionId)

        if (newTags.isDefined && TopicService.sendTagEventsNeeded(section, oldMsg, commit)) {
          sendEvents(newText, oldMsg.id, newTags.get, oldMsg.authorUserId)
        } else sendEvents(newText, oldMsg.id, Seq.empty, oldMsg.authorUserId)
      } else Set.empty[Int]

      if publish then
        topicDao.publish(newMsg)

      if commit then
        changeGroupId.foreach { changeGroupId =>
          if (oldMsg.groupId != changeGroupId) {
            topicDao.changeGroup(oldMsg, changeGroupId)
          }
        }

        doCommit(oldMsg, user, bonus, editorBonus)

      (modified, notified)
    }

    if modified then
      logger.info(s"сообщение ${oldMsg.id} исправлено ${user.nick}")

    if (modified || commit || publish) && !newMsg.draft then
      RealtimeEventHub.notifyEvents(realtimeHubWS, users)
      searchQueueSender.updateMessage(newMsg.id, true)

    modified
  }

  private def doCommit(msg: Topic, commiter: User, bonus: Int, editorBonus: Map[User, Int])(using Transaction): Unit = {
    assert(bonus <= 20 && bonus >= 0, "Некорректное значение bonus")

    if (msg.draft) {
      topicDao.publish(msg)
    }

    topicDao.commit(msg, commiter)

    userService.changeScore(msg.authorUserId, bonus)

    for ((key, delta) <- editorBonus) {
      userService.changeScore(key.id, delta)
    }
  }

  def processUploads(form: ImageTopicRequest, group: Group, errors: Errors, currentImageCount: Int = 0)
                     (using postingUser: AuthorizedSession): Seq[UploadedImagePreview] = {
    val section = sectionService.getSection(group.sectionId)

    val imagesNonNull = Option(form.images).getOrElse(Array.empty[MultipartFile])
    val imagesLimit = Math.max(0, permissionService.imageLimit(section) - currentImageCount)

    val imagePreviews: Seq[UploadedImagePreview] =
      if (permissionService.isImagePostingAllowed(section) &&
        addTopicChecker.checkTopicPosting(group).permitted) {

        val imagePreviews =
          Option(form.uploadedImages)
            .getOrElse(Array.empty[String])
            .view
            .zipAll(imagesNonNull, null, null)
            .take(imagesLimit)
            .flatMap { case (existing, upload) =>
              imageService.processUpload(Option(existing), upload, errors)
            }.toVector

        imagePreviews
      } else {
        Seq.empty
      }

    form.uploadedImages = (imagePreviews.map(_.mainFile.getName) ++
      Vector.fill(imagesLimit - imagePreviews.size)(null)).toArray

    if (section.imagepost && currentImageCount == 0 && imagePreviews.isEmpty) {
      errors.reject(null, "Для этого раздела требуется как минимум одно изображение")
    }

    imagePreviews
  }

  def getById(id: Int): Topic = topicDao.getById(id)

  def getUncommitedCounts: Seq[(Section, Int)] = {
    topicDao.getUncommitedCounts.view.map { p =>
      sectionService.getSection(p._1) -> p._2
    }.toVector
  }

  def getUncommitedCount(section: Section): Int = topicDao.getUncommitedCount(section.id)

  def moveTopic(msg: Topic, newGrp: Group, moveBy: User): Unit = springDB.localTx {
    topicDao.moveTopic(msg, newGrp)

    if !newGrp.linksAllowed then
      val markup = msgbaseDao.getMessageText(msg.id).markup
      val moveInfo = textService.moveInfo(markup, msg.url, msg.linktext, moveBy, msg.groupUrl)
      msgbaseDao.appendMessage(msg.id, moveInfo)
    end if
  }
}