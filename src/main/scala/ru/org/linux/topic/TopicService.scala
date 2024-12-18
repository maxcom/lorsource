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
package ru.org.linux.topic

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletRequest
import org.joda.time.DateTime
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.BindingResult
import ru.org.linux.auth.AuthorizedSession
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryRecord}
import ru.org.linux.gallery.{ImageDao, ImageService, UploadedImagePreview}
import ru.org.linux.group.{Group, GroupPermissionService}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.{PollDao, PollVariant}
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.tag.{TagName, TagService}
import ru.org.linux.user.*
import ru.org.linux.util.LorHttpUtils

import java.io.File
import scala.jdk.CollectionConverters.SeqHasAsJava

object TopicService {
  private def sendTagEventsNeeded(section: Section, oldMsg: Topic, commit: Boolean): Boolean = {
    val needCommit = section.isPremoderated && !oldMsg.commited

    val fresh = oldMsg.getEffectiveDate.isAfter(DateTime.now.minusMonths(1))

    commit || (!needCommit && fresh)
  }
}

@Service
class TopicService(topicDao: TopicDao, msgbaseDao: MsgbaseDao, sectionService: SectionService,
                   imageService: ImageService, pollDao: PollDao, userEventService: UserEventService,
                   topicTagService: TopicTagService, userService: UserService, userTagService: UserTagService,
                   userDao: UserDao, textService: MessageTextService, editHistoryDao: EditHistoryDao,
                   imageDao: ImageDao, siteConfig: SiteConfig, permissionService: GroupPermissionService,
                   val transactionManager: PlatformTransactionManager) extends TransactionManagement with StrictLogging {

  def addMessage(request: HttpServletRequest, form: AddTopicRequest, message: MessageText, group: Group, user: User,
                 image: Option[UploadedImagePreview], additionalImages: Seq[UploadedImagePreview],
                 previewMsg: Topic): (Int, Set[Int]) = transactional() { _ =>
    val section = sectionService.getSection(group.sectionId)

    if (section.isImagepost && image.isEmpty) {
      throw new ScriptErrorException("scrn is empty?!")
    }

    val msgid = topicDao.saveNewMessage(previewMsg, user, request.getHeader("User-Agent"), group)

    msgbaseDao.saveNewMessage(message, msgid)

    image.foreach { imagePreview =>
      imageService.saveImage(imagePreview, msgid, main = true)
    }

    additionalImages.foreach { imagePreview =>
      imageService.saveImage(imagePreview, msgid, main = false)
    }

    if (section.isPollPostAllowed) {
      pollDao.createPoll(form.poll.toSeq.asJava, form.multiSelect, msgid)
    }

    val tags = TagName.parseAndSanitizeTags(form.tags)

    topicTagService.updateTags(msgid, tags)

    val notified = if (!previewMsg.draft) {
      if (section.isPremoderated) {
        sendEvents(message, msgid, Seq.empty, user.getId)
      } else {
        sendEvents(message, msgid, tags, user.getId)
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
    userRefs = userRefs.filterNot(p => userService.isIgnoring(p.getId, author))

    // оповещение пользователей по тегам
    val userIdListByTags = userTagService.getUserIdListByTags(author, tags)
    val userRefIds = userRefs.view.map(_.getId).filterNot(notifiedUsers.contains).toSet

    // Не оповещать пользователей, которые ранее были оповещены через упоминание
    val tagUsers = userIdListByTags.filterNot(u => userRefIds.contains(u) || notifiedUsers.contains(u))

    userEventService.addUserRefEvent(userRefIds, msgid)
    userEventService.addUserTagEvent(tagUsers, msgid)

    userRefIds ++ tagUsers
  }

  def updateAndCommit(newMsg: Topic, oldMsg: Topic, user: User, newTags: Option[Seq[String]],
                      newText: MessageText, commit: Boolean, changeGroupId: Option[Int], bonus: Int,
                      pollVariants: Seq[PollVariant], multiselect: Boolean,
                      editorBonus: Map[Int, Int], imagePreview: Option[UploadedImagePreview]): (Boolean, Set[Int]) = transactional() { _ =>
    val editHistoryRecord = new EditHistoryRecord

    var modified = topicDao.updateMessage(editHistoryRecord, oldMsg, newMsg, user,newText.text, pollVariants.asJava, multiselect)

    newTags.foreach { newTags =>
      val oldTags = topicTagService.getTags(newMsg)
      val modifiedTags: Boolean = topicTagService.updateTags(newMsg.id, newTags)

      if (modifiedTags) {
        editHistoryRecord.setOldtags(TagService.tagsToString(oldTags))
        modified = true
      }
    }

    imagePreview.foreach { imagePreview =>
      replaceImage(oldMsg, imagePreview, editHistoryRecord)

      modified = true
    }

    if (modified) {
      editHistoryDao.insert(editHistoryRecord)
    }

    val notified = if (!newMsg.draft && !newMsg.expired) {
      val section = sectionService.getSection(oldMsg.sectionId)

      if (newTags.isDefined && TopicService.sendTagEventsNeeded(section, oldMsg, commit)) {
        sendEvents(newText, oldMsg.id, newTags.get, oldMsg.authorUserId)
      } else {
        sendEvents(newText, oldMsg.id, Seq.empty, oldMsg.authorUserId)
      }
    } else {
      Set.empty[Int]
    }


    if (oldMsg.draft && !newMsg.draft) {
      topicDao.publish(newMsg)
    }

    if (commit) {
      changeGroupId.foreach { changeGroupId =>
        if (oldMsg.groupId != changeGroupId) {
          topicDao.changeGroup(oldMsg, changeGroupId)
        }
      }

      doCommit(oldMsg, user, bonus, editorBonus)
    }

    if (modified) {
      logger.info(s"сообщение ${oldMsg.id} исправлено ${user.getNick}")
    }

    (modified, notified)
  }

  private def replaceImage(oldMsg: Topic, imagePreview: UploadedImagePreview, editHistoryRecord: EditHistoryRecord): Unit = {
    val oldImage = imageDao.imageForTopic(oldMsg)

    oldImage.foreach { oldImage =>
      imageDao.deleteImage(oldImage)
    }

    val id = imageDao.saveImage(oldMsg.id, imagePreview.extension, main = true)

    val galleryPath = new File(siteConfig.getUploadPath + "/images")

    imagePreview.moveTo(galleryPath, Integer.toString(id))

    if (oldImage.isDefined) {
      editHistoryRecord.setOldimage(oldImage.get.id)
    } else {
      editHistoryRecord.setOldimage(0)
    }
  }

  private def doCommit(msg: Topic, commiter: User, bonus: Int, editorBonus: Map[Int, Int]): Unit = {
    assert(bonus <= 20 && bonus >= 0, "Некорректное значение bonus")

    if (msg.draft) {
      topicDao.publish(msg)
    }

    topicDao.commit(msg, commiter)

    userDao.changeScore(msg.authorUserId, bonus)

    for ((key, delta) <- editorBonus) {
      userDao.changeScore(key, delta)
    }
  }

  def processUploads(form: ImageTopicRequest, group: Group, errors: BindingResult)
                    (implicit postingUser: AuthorizedSession): (Option[UploadedImagePreview], Seq[UploadedImagePreview]) = {
    val section = sectionService.getSection(group.sectionId)

    val additionalImagesNonNull = Option(form.additionalImage).getOrElse(Array.empty)
    val additionalImagesLimit = permissionService.additionalImageLimit(section)

    val (imagePreview: Option[UploadedImagePreview], additionalImagePreviews: Seq[UploadedImagePreview]) =
      if (permissionService.isImagePostingAllowed(section) &&
        permissionService.isTopicPostingAllowed(group)) {
        val main = imageService.processUpload(Option(form.uploadedImage), form.image, errors)

        val additionalImagePreviews =
          Option(form.additionalUploadedImages)
            .getOrElse(Array.empty)
            .view
            .zipAll(additionalImagesNonNull, null, null)
            .take(additionalImagesLimit)
            .flatMap { case (existing, upload) =>
              imageService.processUpload(Option(existing), upload, errors)
            }.toVector

        (main, additionalImagePreviews)
      } else {
        (None, Seq.empty)
      }

    form.uploadedImage = imagePreview.map(_.mainFile.getName).orNull

    form.additionalUploadedImages = (additionalImagePreviews.map(_.mainFile.getName) ++
      Vector.fill(additionalImagesLimit - additionalImagePreviews.size)(null)).toArray

    if (section.isImagepost && imagePreview.isEmpty) {
      errors.reject(null, "Изображение отсутствует")
    }

    (imagePreview, additionalImagePreviews)
  }
}