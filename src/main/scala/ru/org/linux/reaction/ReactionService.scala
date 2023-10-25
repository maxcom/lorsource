/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.reaction

import akka.actor.ActorRef
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.comment.Comment
import ru.org.linux.markup.MessageTextService
import ru.org.linux.reaction.PreparedReactions.allZeros
import ru.org.linux.reaction.ReactionService.DefinedReactions
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.section.Section
import ru.org.linux.site.DateFormats
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.{IgnoreListDao, ProfileDao, User, UserEventDao, UserService}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

case class PreparedReaction(@BeanProperty count: Int, @BeanProperty topUsers: java.util.List[User],
                            @BooleanBeanProperty hasMore: Boolean, @BooleanBeanProperty clicked: Boolean,
                            @BeanProperty description: String)

case class PreparedReactions(reactions: Map[String, PreparedReaction],
                             @BooleanBeanProperty allowInteract: Boolean) {
  // used in jsp
  def getMap: java.util.Map[String, PreparedReaction] = reactions.asJava

  // empty is a keyword in jsp
  // used in jsp
  def isEmptyMap: Boolean = !reactions.exists(_._2.count > 0)

  // used in jsp
  def isTotal: Boolean = reactions.forall(_._2.count > 0)
}

case class ReactionListItem(@BeanProperty user: User, @BeanProperty reaction: String, date: Option[Date]) {
  // for jsp
  def dateFormatted(tz: DateTimeZone): String = date.map(d => DateFormats.getDefault(tz).print(d.getTime)).getOrElse("")
}

case class PreparedReactionList(reactions: Seq[ReactionListItem]) {
  // used in jsp
  def getList: java.util.List[ReactionListItem] = reactions.asJava
}

object PreparedReactions {
  val emptyDisabled: PreparedReactions = PreparedReactions(Map.empty, allowInteract = false)

  val allZeros: Map[String, PreparedReaction] =
    DefinedReactions.view.map { case (r, d) =>
      r -> PreparedReaction(0, Seq.empty.asJava, hasMore = false, clicked = false, description = d)
    }.to(TreeMap)
}

case class PreparedReactionView(@BeanProperty item: ReactionsLogItem, @BeanProperty title: String,
                                @BeanProperty targetUser: User, @BeanProperty textPreview: String,
                                sectionId: Int, groupUrlName: String) {
  def getLink: String = {
    val topicLink: String = Section.getSectionLink(sectionId) +
      URLEncoder.encode(groupUrlName, StandardCharsets.UTF_8) + '/' + item.topicId

    item.commentId match {
      case Some(commentId) =>
        topicLink + "?cid=" + commentId
      case None =>
        topicLink
    }
  }

  def isComment: Boolean = item.commentId.isDefined
}

object ReactionService {
  // beer: "\uD83C\uDF7A" (fix sort order)
  val DefinedReactions: Map[String, String] = Map(
    "\uD83D\uDC4D" -> "большой палец вверх",
    "\uD83D\uDC4E" -> "большой палец вниз",
    "\uD83D\uDE0A" -> "улыбающееся лицо",
    "\uD83D\uDE31" -> "лицо, кричащее от страха",
    "\uD83E\uDD26" -> "facepalm",
    "\uD83D\uDD25" -> "огонь",
    "\uD83E\uDD14" -> "задумчивое лицо",
    "\uD83E\uDD21" -> "лицо клоуна",
    "\u2615\u2615" -> "два чая этому господину!",
    "\uD83E\uDE97" -> "боян!!!1111")

  val AllowedReactions: Set[String] = DefinedReactions.keySet
}

@Service
class ReactionService(userService: UserService, reactionDao: ReactionDao, topicDao: TopicDao,
                      userEventDao: UserEventDao, @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef,
                      ignoreListDao: IgnoreListDao, profileDao: ProfileDao, msgbaseDao: MsgbaseDao,
                      textService: MessageTextService,
                      val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  def allowInteract(currentUser: Option[User], topic: Topic, comment: Option[Comment]): Boolean = {
    val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)

    currentUser.isDefined &&
      !currentUser.exists(_.isFrozen) &&
      !topic.deleted &&
      !topic.expired &&
      comment.forall(!_.deleted) &&
      currentUser.forall(_.getId != authorId) &&
      (comment.isEmpty || topic.postscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS)
  }

  def prepareReactionList(reactions: Reactions, reactionsLog: Seq[ReactionsLogItem], ignoreList: Set[Int]): PreparedReactionList = {
    val log = reactionsLog.view.map(r => r.originUserId -> r.setDate).toMap

    val epoch = new Date(0)

    PreparedReactionList((reactions.reactions -- ignoreList).map { r =>
      ReactionListItem(userService.getUserCached(r._1), r._2, log.get(r._1))
    }.toSeq.sortBy(_.date.getOrElse(epoch)))
  }

  def prepare(reactions: Reactions, ignoreList: Set[Int], currentUser: Option[User],
              topic: Topic, comment: Option[Comment]): PreparedReactions = {
    PreparedReactions(allZeros ++
      reactions.reactions
        .groupMap(_._2)(_._1)
        .view
        .map { case (r, userIds) =>
          val userIdsSet = userIds.toSet

          val filteredUserIds = userIdsSet -- ignoreList
          val users = userService.getUsersCached(filteredUserIds)
          val clicked = currentUser.map(_.getId).exists(userIdsSet.contains)

          r -> PreparedReaction(filteredUserIds.size, users.sortBy(-_.getScore).take(3).asJava,
            hasMore = users.sizeIs > 3, clicked = clicked, DefinedReactions.getOrElse(r, r))
        }, allowInteract = allowInteract(currentUser, topic, comment))
  }

  private def isNotificationsEnabledFor(userId: Int): Boolean =
    profileDao.readProfile(userId).isReactionNotificationEnabled

  def setCommentReaction(topic: Topic, comment: Comment, user: User, reaction: String,
                         set: Boolean): Int = {
    val r = transactional() { _ =>
      val newCount = reactionDao.setCommentReaction(comment, user, reaction, set)

      topicDao.updateLastmod(comment.topicId, false)

      if (set) {
        val authorsIgnoreList = ignoreListDao.get(comment.userid)

        if (!authorsIgnoreList.contains(user.getId) && comment.userid != User.ANONYMOUS_ID &&
          isNotificationsEnabledFor(comment.userid)) {
          userEventDao.insertReactionNotification(user, topic, Some(comment))
        }
      } else {
        userEventDao.deleteUnreadReactionNotification(user, topic, Some(comment))
      }

      newCount
    }

    realtimeHubWS ! RealtimeEventHub.RefreshEvents(Set(comment.userid))

    r
  }

  def setTopicReaction(topic: Topic, user: User, reaction: String, set: Boolean): Int = {
    val r = transactional() { _ =>
      val newCount = reactionDao.setTopicReaction(topic, user, reaction, set)

      topicDao.updateLastmod(topic.id, false)

      if (set) {
        val authorsIgnoreList = ignoreListDao.get(topic.authorUserId)

        if (!authorsIgnoreList.contains(user.getId) && topic.authorUserId != User.ANONYMOUS_ID &&
          isNotificationsEnabledFor(topic.authorUserId)) {
          userEventDao.insertReactionNotification(user, topic, None)
        }
      } else {
        userEventDao.deleteUnreadReactionNotification(user, topic, None)
      }

      newCount
    }

    realtimeHubWS ! RealtimeEventHub.RefreshEvents(Set(topic.authorUserId))

    r
  }

  def getReactionsView(originUser: User, offset: Int, size: Int,modeTo: Boolean): Seq[PreparedReactionView] = {
    val items = reactionDao.getReactionsView(originUser, offset, size,modeTo)
    val textIds = items.view.map(_.item).map(i => i.commentId.getOrElse(i.topicId)).distinct.toSeq
    val targetUserIds = items.view.map(_.targetUserId).distinct.toSeq

    val texts: Map[Int, MessageText] = msgbaseDao.getMessageText(textIds)
    val targetUsers: Map[Int, User] = userService.getUsersCached(targetUserIds).view.map(u => u.getId -> u).toMap

    items.map { item =>
      val plainText = textService.extractPlainText(texts(item.item.commentId.getOrElse(item.item.topicId)))
      val textPreview = MessageTextService.trimPlainText(plainText, 250, encodeHtml = false)

      PreparedReactionView(
        item = item.item,
        title = item.title,
        targetUser = targetUsers(item.targetUserId),
        textPreview = textPreview,
        sectionId = item.sectionId,
        groupUrlName = item.groupUrlName)
    }
  }
}
