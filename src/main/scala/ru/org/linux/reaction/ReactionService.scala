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

import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.comment.Comment
import ru.org.linux.reaction.PreparedReactions.allZeros
import ru.org.linux.reaction.ReactionService.AllowedReactions
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.{User, UserService}

import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

case class PreparedReaction(@BeanProperty count: Int, @BeanProperty topUsers: java.util.List[User],
                            @BooleanBeanProperty hasMore: Boolean, @BooleanBeanProperty clicked: Boolean)

object PreparedReaction {
  val empty: PreparedReaction = PreparedReaction(0, Seq.empty.asJava, hasMore = false, clicked = false)
}

case class PreparedReactions(reactions: Map[String, PreparedReaction]) {
  // used in jsp
  def getMap: java.util.Map[String, PreparedReaction] = reactions.asJava

  // empty is a keyword in jsp
  // used in jsp
  def isEmptyMap: Boolean = !reactions.exists(_._2.count > 0)
}

object PreparedReactions {
  val empty: PreparedReactions = PreparedReactions(Map.empty)

  val allZeros: Map[String, PreparedReaction] =
    AllowedReactions.view.map(_ -> PreparedReaction.empty).to(TreeMap)
}

object ReactionService {
  val AllowedReactions: Set[String] = Set("\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDE0A", "\uD83D\uDE31",
    "\uD83E\uDD26", "\uD83D\uDD25", "\uD83E\uDD14")
}

@Service
class ReactionService(userService: UserService, reactionDao: ReactionDao, topicDao: TopicDao,
                      val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  def prepare(reactions: Reactions, ignoreList: Set[Int], @Nullable currentUser: User): PreparedReactions = {
    PreparedReactions(allZeros ++
      reactions.reactions
        .view
        .mapValues { userIds =>
          val filteredUserIds = userIds.toSet -- ignoreList
          val users = userService.getUsersCached(filteredUserIds)
          val clicked = Option(currentUser).map(_.getId).exists(userIds.contains)

          PreparedReaction(filteredUserIds.size, users.sortBy(-_.getScore).take(3).asJava,
            hasMore = users.sizeIs > 3, clicked = clicked)
        }
    )
  }

  def setCommentReaction(comment: Comment, user: User, reaction: String, set: Boolean): Unit = transactional() { _ =>
    reactionDao.setCommentReaction(comment, user, reaction, set)
    topicDao.updateLastmod(comment.topicId, false)
  }
}
