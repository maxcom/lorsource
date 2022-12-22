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
package ru.org.linux.reaction

import akka.actor.ActorRef
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.comment.Comment
import ru.org.linux.reaction.PreparedReactions.allZeros
import ru.org.linux.reaction.ReactionService.DefinedReactions
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.{IgnoreListDao, User, UserEventDao, UserService}

import javax.annotation.Nullable
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

case class ReactionListItem(@BeanProperty user: User, @BeanProperty reaction: String)
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

object ReactionService {
  // beer: "\uD83C\uDF7A" (fix sort order)
  val DefinedReactions: Map[String, String] = Map(
    "\uD83D\uDC4D" -> "большой палец вверх",
    "\uD83D\uDC4E" -> "большой палец вниз",
    "\uD83D\uDE0A" -> "улыбающееся лицо",
    "\uD83D\uDE31" -> "лицо, кричащее от страха",
    "\uD83E\uDD26" -> "facepalm",
    "\uD83D\uDD25" -> "огонь",
    "\uD83E\uDD14" -> "задумчивое лицо")

  val AllowedReactions: Set[String] = DefinedReactions.keySet
}

@Service
class ReactionService(userService: UserService, reactionDao: ReactionDao, topicDao: TopicDao,
                      userEventDao: UserEventDao, @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef,
                      ignoreListDao: IgnoreListDao,
                      val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  def allowInteract(@Nullable currentUser: User, topic: Topic, comment: Option[Comment]): Boolean = {
    val authorId = comment.map(_.userid).getOrElse(topic.authorUserId)

    currentUser != null &&
      !currentUser.isFrozen &&
      !topic.deleted &&
      !topic.expired &&
      comment.forall(! _.deleted) &&
      currentUser.getId != authorId &&
      (comment.isEmpty || topic.postscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS)
  }

  def prepareReactionList(reactions: Reactions, ignoreList: Set[Int]): PreparedReactionList = {
    PreparedReactionList((reactions.reactions -- ignoreList).map { r =>
      ReactionListItem(userService.getUserCached(r._1), r._2)
    }.toSeq.sortBy(-_.user.getScore))
  }

  def prepare(reactions: Reactions, ignoreList: Set[Int], @Nullable currentUser: User,
              topic: Topic, comment: Option[Comment]): PreparedReactions = {
    PreparedReactions(allZeros ++
      reactions.reactions
        .groupMap(_._2)(_._1)
        .view
        .map { case (r, userIds) =>
          val userIdsSet = userIds.toSet

          val filteredUserIds = userIdsSet -- ignoreList
          val users = userService.getUsersCached(filteredUserIds)
          val clicked = Option(currentUser).map(_.getId).exists(userIdsSet.contains)

          r -> PreparedReaction(filteredUserIds.size, users.sortBy(-_.getScore).take(3).asJava,
            hasMore = users.sizeIs > 3, clicked = clicked, DefinedReactions.getOrElse(r, r))
        }, allowInteract = allowInteract(currentUser, topic, comment))
  }

  def setCommentReaction(topic: Topic, comment: Comment, user: User, reaction: String,
                         set: Boolean): Int = {
    val r = transactional() { _ =>
      val newCount = reactionDao.setCommentReaction(comment, user, reaction, set)

      topicDao.updateLastmod(comment.topicId, false)

      if (set) {
        val authorsIgnoreList = ignoreListDao.get(comment.userid)

        if (!authorsIgnoreList.contains(user.getId) && comment.userid!=User.ANONYMOUS_ID) {
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

        if (!authorsIgnoreList.contains(user.getId) && topic.authorUserId!=User.ANONYMOUS_ID) {
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
}
