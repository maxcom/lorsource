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

import jakarta.servlet.http.HttpServletRequest
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.edithistory.EditHistoryDao
import ru.org.linux.edithistory.EditHistoryRecord
import ru.org.linux.gallery.ImageDao
import ru.org.linux.gallery.ImageService
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.Group
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.PollDao
import ru.org.linux.poll.PollVariant
import ru.org.linux.section.Section
import ru.org.linux.section.SectionService
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.tag.TagName
import ru.org.linux.user.*
import ru.org.linux.util.LorHttpUtils

import java.io.File
import java.sql.Timestamp
import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.transaction.PlatformTransactionManager

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

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
                   userDao: UserDao, deleteInfoDao: DeleteInfoDao, textService: MessageTextService,
                   editHistoryDao: EditHistoryDao, imageDao: ImageDao, siteConfig: SiteConfig,
                   val transactionManager: PlatformTransactionManager) extends TransactionManagement with StrictLogging {

  def addMessage(request: HttpServletRequest, form: AddTopicRequest, message: MessageText, group: Group, user: User,
                 imagePreview: UploadedImagePreview, previewMsg: Topic): (Int, Set[Int]) = transactional() { _ =>
    val section = sectionService.getSection(group.sectionId)

    if (section.isImagepost && imagePreview == null) {
      throw new ScriptErrorException("scrn==null!?")
    }

    val msgid = topicDao.saveNewMessage(previewMsg, user, request.getHeader("User-Agent"), group)

    msgbaseDao.saveNewMessage(message, msgid)

    if (imagePreview != null) {
      imageService.saveScreenshot(imagePreview, msgid)
    }

    if (section.isPollPostAllowed) {
      pollDao.createPoll(form.getPoll.toSeq.asJava, form.isMultiSelect, msgid)
    }

    val tags = TagName.parseAndSanitizeTags(form.getTags)

    topicTagService.updateTags(msgid, tags.asJava)

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

  /**
   * Удаление топика и если удаляет модератор изменить автору score
   *
   * @param message удаляемый топик
   * @param user    удаляющий пользователь
   * @param reason  причина удаления
   * @param bonus   дельта изменения score автора топика
   */
  def deleteWithBonus(message: Topic, user: User, reason: String, bonus: Int): Unit = transactional() { _ =>
    assert(bonus <= 20 && bonus >= 0, "Некорректное значение bonus")

    if (user.isModerator && bonus != 0 && user.getId != message.authorUserId && !message.draft) {
      val deleted = deleteTopic(message.id, user, reason, -bonus)

      if (deleted) {
        userDao.changeScore(message.authorUserId, -bonus)
      }
    } else {
      deleteTopic(message.id, user, reason, 0)
    }
  }

  private def deleteTopic(mid: Int, moderator: User, reason: String, bonus: Int) = {
    val deleted = topicDao.delete(mid)

    if (deleted) {
      deleteInfoDao.insert(mid, moderator, reason, bonus)
      userEventService.processTopicDeleted(Seq(mid))
    }

    deleted
  }

  def deleteByIPAddress(ip: String, startTime: Timestamp, moderator: User,
                        reason: String): java.util.List[Integer] = transactional() { _ =>
    val topicIds = topicDao.getAllByIPForUpdate(ip, startTime).asScala.map(_.toInt)

    massDelete(moderator, topicIds, reason).map(Integer.valueOf).asJava
  }

  /**
   * Массовое удаление всех топиков пользователя.
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @return список удаленных топиков
   * @throws UserNotFoundException генерирует исключение если пользователь отсутствует
   */
  def deleteAllByUser(user: User, moderator: User): java.util.List[Integer] = transactional() { _ =>
    val topics = topicDao.getUserTopicForUpdate(user).asScala.map(_.toInt)

    massDelete(moderator, topics, "Блокировка пользователя с удалением сообщений").map(Integer.valueOf).asJava
  }

  private def massDelete(moderator: User, topics: Iterable[Int], reason: String): collection.Seq[Int] = {
    val deletedTopicsBuilder = Vector.newBuilder[Int]

    for (mid <- topics) {
      val deleted = topicDao.delete(mid)

      if (deleted) {
        deleteInfoDao.insert(mid, moderator, reason, 0)
        deletedTopicsBuilder.addOne(mid)
      }
    }

    val deletedTopics = deletedTopicsBuilder.result()

    userEventService.processTopicDeleted(deletedTopics)

    deletedTopics
  }

  def updateAndCommit(newMsg: Topic, oldMsg: Topic, user: User, newTags: Option[Seq[String]],
                      newText: MessageText, commit: Boolean, changeGroupId: Option[Int], bonus: Int,
                      pollVariants: Seq[PollVariant], multiselect: Boolean,
                      editorBonus: Map[Int, Int], imagePreview: UploadedImagePreview): (Boolean, Set[Int]) = transactional() { _ =>
    val editHistoryRecord = new EditHistoryRecord

    var modified = topicDao.updateMessage(editHistoryRecord, oldMsg, newMsg, user, newTags.map(_.asJava).orNull, newText.text,
      pollVariants.asJava, multiselect)

    if (imagePreview != null) {
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

    if (oldImage != null) {
      imageDao.deleteImage(oldImage)
    }

    val id = imageDao.saveImage(oldMsg.id, imagePreview.extension)

    val galleryPath = new File(siteConfig.getUploadPath + "/images")

    imagePreview.moveTo(galleryPath, Integer.toString(id))

    if (oldImage != null) {
      editHistoryRecord.setOldimage(oldImage.id)
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

    if (editorBonus != null) {
      for ((key, delta) <- editorBonus) {
        userDao.changeScore(key, delta)
      }
    }
  }
}