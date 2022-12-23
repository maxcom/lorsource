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
import ru.org.linux.reaction.ReactionListItem
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
  def prepareSimple(events: collection.Seq[UserEvent], withText: Boolean): Seq[PreparedUserEvent] = {
    val userIds = (events.map(_.commentAuthor) ++ events.map(_.topicAuthor) ++ events.map(_.originUserId)).filter(_ != 0)
    val users = userService.getUsersCachedMap(userIds)

    val tags = tagService.tagRefs(events.map(_.topicId).distinct).view.mapValues(_.map(_.name)).toMap

    events.view.map(event => prepare(event, withText, users, tags)).toSeq
  }

  private def prepare(event: UserEvent, withText: Boolean, users: Map[Int, User],
                      tags: Map[Int, collection.Seq[String]]): PreparedUserEvent = {
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

    val originAuthor = if (event.originUserId != 0) {
      users.get(event.originUserId)
    } else {
      None
    }

    val group = groupDao.getGroup(event.groupId)

    val topicAuthor = users(event.topicAuthor)

    val reactions = if (event.eventType == UserEventFilterEnum.REACTION) {
      originAuthor.map { u =>
        Seq(ReactionListItem(u, event.reaction))
      }.getOrElse(Seq.empty)
    } else {
      Seq.empty
    }

    PreparedUserEvent(
      event = event,
      messageText = text,
      author = originAuthor.orElse(commentAuthor).getOrElse(topicAuthor),
      bonus = loadBonus(event),
      section = sectionService.getSection(group.getSectionId),
      group = group,
      tags = tags.getOrElse(event.topicId, Seq.empty).take(TopicTagService.MaxTagsInTitle).toSeq,
      lastId = event.id,
      date = event.eventDate,
      commentId = event.cid,
      authors = Set(commentAuthor.getOrElse(topicAuthor)),
      reactions = reactions)
  }

  private def loadBonus(event: UserEvent): Option[Int] = {
    (if ("DEL" == event.eventType.getType) {
      val msgid = if (event.isComment) event.cid else event.topicId

      deleteInfoDao.getDeleteInfo(msgid).toScala
    } else {
      None
    }).map(_.getBonus)
  }

  private def groupFavorites(toGroup: scala.collection.Seq[UserEvent], users: Map[Int, User],
                              tags: Map[Int, collection.Seq[String]]): Iterable[PreparedUserEvent] = {
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

          val topicAuthor = users(event.topicAuthor)

          val originAuthor = if (event.originUserId != 0) {
            users.get(event.originUserId)
          } else {
            None
          }

          Some(PreparedUserEvent(
            event = event,
            messageText = None,
            author = originAuthor.orElse(commentAuthor).getOrElse(topicAuthor),
            bonus = loadBonus(event),
            section = sectionService.getSection(group.getSectionId),
            group = group,
            tags = tags.getOrElse(event.topicId, Seq.empty).take(TopicTagService.MaxTagsInTitle).toSeq,
            lastId = event.id,
            date = event.eventDate,
            commentId = event.cid,
            authors = Set(commentAuthor.getOrElse(topicAuthor)),
            reactions = Seq.empty))
        case Some(existing) =>
          val author = if (event.isComment) {
            users(event.commentAuthor)
          } else {
            users(event.topicAuthor)
          }

          Some(existing.withSimilarFav(event, author))
      }
    }

    grouped.values
  }

  private def groupReactions(toGroup: scala.collection.Seq[UserEvent], users: Map[Int, User],
                             tags: Map[Int, collection.Seq[String]]): Seq[PreparedUserEvent] = {
     toGroup.foldRight(Vector.empty[PreparedUserEvent]) { case (event, acc) =>
       val replaceIdx = {
         val similarIdx = acc.indexWhere { existing =>
           existing.event.cid == event.cid &&
             existing.event.topicId == event.topicId &&
             existing.event.unread == event.unread &&
             (Math.abs(event.eventDate.getTime - existing.event.eventDate.getTime) < 30 * 60 * 1000)
         }

         val lastSimilar = if (acc.lastOption.exists { existing =>
           existing.event.cid == event.cid &&
             existing.event.topicId == event.topicId &&
             existing.event.unread == event.unread
         }) {
           acc.length - 1
         } else {
           -1
         }

         if (similarIdx != -1) similarIdx else lastSimilar
       }

       if (replaceIdx == -1) {
         acc :+ prepare(event, withText = false, users, tags)
       } else {
         acc.updated(replaceIdx, acc(replaceIdx).withSimilarReaction(event, users(event.originUserId)))
       }
     }
  }

  def prepareGrouped(events: collection.Seq[UserEvent], newDesign: Boolean): Seq[PreparedUserEvent] = {
    val userIds = (events.map(_.commentAuthor) ++ events.map(_.topicAuthor) ++ events.map(_.originUserId)).filter(_ != 0)
    val users = userService.getUsersCachedMap(userIds)

    val tags = tagService.tagRefs(events.map(_.topicId).distinct).view.mapValues(_.map(_.name)).toMap

    val (favorites, rest) = events.partition(event => event.eventType == UserEventFilterEnum.FAVORITES)
    val (reactions, other) = rest.partition(event => event.eventType == UserEventFilterEnum.REACTION)

    val groupedFavorities = groupFavorites(favorites, users, tags)

    val groupedReactions = if (newDesign) {
      groupReactions(reactions, users, tags)
    } else {
      reactions.view.map(event => prepare(event, withText = false, users, tags))
    }

    val otherPrepared = other.view.map(event => prepare(event, withText = false, users, tags))

    (groupedFavorities ++ otherPrepared ++ groupedReactions)
      .toSeq.sorted(Ordering.by((_: PreparedUserEvent).date).reverse)
  }
}
