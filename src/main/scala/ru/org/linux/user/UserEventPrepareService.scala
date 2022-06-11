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

package ru.org.linux.user

import org.springframework.stereotype.Service
import ru.org.linux.group.GroupDao
import ru.org.linux.markup.MessageTextService
import ru.org.linux.section.SectionService
import ru.org.linux.spring.dao.{DeleteInfoDao, MsgbaseDao}
import ru.org.linux.topic.TopicTagService

import scala.jdk.OptionConverters.RichOptional

@Service
class UserEventPrepareService(msgbaseDao: MsgbaseDao, messageTextService: MessageTextService, userService: UserService,
                              deleteInfoDao: DeleteInfoDao, sectionService: SectionService, groupDao: GroupDao,
                              tagService: TopicTagService) {
  /**
   * @param events      список событий
   * @param withText возвращать ли отрендеренное содержимое уведомлений (используется только для RSS)
   */
  def prepare(events: collection.Seq[UserEvent], withText: Boolean): Seq[PreparedUserEvent] = {
    val userIds = (events.map(_.commentAuthor) ++ events.map(_.topicAuthor)).filter(_ != 0)
    val users = userService.getUsersCachedMap(userIds)

    val tags = tagService.tagRefs(events.map(_.topicId).distinct).view.mapValues(_.map(_.name))

    val prepared = events.view.map { event =>
      val msgid = if (event.isComment) event.cid else event.topicId

      val text = if (withText) {
        val messageText = msgbaseDao.getMessageText(msgid)

        Some(messageTextService.renderTextRSS(messageText))
      } else {
        None
      }

      val commentAuthor = if (event.isComment) {
        users.get(event.commentAuthor)
      } else {
        None
      }

      val group = groupDao.getGroup(event.groupId)

      PreparedUserEvent(
        event = event,
        messageText = text,
        topicAuthor = users(event.topicAuthor),
        commentAuthor = commentAuthor,
        bonus = loadBonus(event),
        section = sectionService.getSection(group.getSectionId),
        group = group,
        tags = tags.getOrElse(event.topicId, Seq.empty).take(TopicTagService.MaxTagsInTitle).toSeq,
        lastId = event.id,
        date = event.eventDate,
        commentId = event.getCid)
    }

    prepared
  }.toSeq

  private def loadBonus(event: UserEvent): Option[Int] = {
    (if ("DEL" == event.eventType.getType) {
      val msgid = if (event.isComment) event.cid else event.topicId

      deleteInfoDao.getDeleteInfo(msgid).toScala
    } else {
      None
    }).map(_.getBonus)
  }

  def prepareGrouped(events: collection.Seq[UserEvent]): Seq[PreparedUserEvent] = {
    val userIds = (events.map(_.commentAuthor) ++ events.map(_.topicAuthor)).filter(_ != 0)
    val users = userService.getUsersCachedMap(userIds)

    val tags = tagService.tagRefs(events.map(_.topicId).distinct).view.mapValues(_.map(_.name))

    val (toGroup, other) = events.partition(event => event.eventType == UserEventFilterEnum.FAVORITES)

    // grouped by (topicId, unread)
    val grouped = toGroup.foldRight(Map.empty[(Int, Boolean), PreparedUserEvent]) { case (event, acc) =>
      acc.updatedWith((event.topicId, event.unread)) {
        case None =>
          val commentAuthor = if (event.isComment) {
            users.get(event.commentAuthor)
          } else {
            None
          }

          val group = groupDao.getGroup(event.groupId)

          Some(PreparedUserEvent(
            event = event,
            messageText = None,
            topicAuthor = users(event.topicAuthor),
            commentAuthor = commentAuthor,
            bonus = loadBonus(event),
            section = sectionService.getSection(group.getSectionId),
            group = group,
            tags = tags.getOrElse(event.topicId, Seq.empty).take(TopicTagService.MaxTagsInTitle).toSeq,
            lastId = event.id,
            date = event.eventDate,
            commentId = event.getCid))
        case Some(existing) =>
          Some(existing.withSimilar(event))
      }
    }

    (grouped.values ++ prepare(other, withText = false))
      .toSeq.sorted(Ordering.by((_: PreparedUserEvent).date).reverse)
  }
}
