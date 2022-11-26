/*
 * Copyright 1998-2016 Linux.org.ru
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

import org.springframework.stereotype.Service
import ru.org.linux.user.{User, UserService}

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

case class PreparedReaction(@BeanProperty count: Int, @BeanProperty topUsers: java.util.List[User],
                            @BooleanBeanProperty hasMore: Boolean)

case class PreparedReactions(reactions: Map[String, PreparedReaction]) {
  // used in jsp
  def getMap: java.util.Map[String, PreparedReaction] = reactions.asJava

  // empty is a keyword in jsp
  // used in jsp
  def isEmptyMap: Boolean = !reactions.exists(_._2.count > 0)
}

object PreparedReactions {
  val empty: PreparedReactions = PreparedReactions(Map.empty)
}
@Service
class ReactionPrepareService(userService: UserService) {
  def prepare(reactions: Reactions, ignoreList: Set[Int]): PreparedReactions = {
    PreparedReactions(reactions.reactions
      .view
      .mapValues { userIds =>
        val filteredUserIds = userIds.toSet -- ignoreList
        val users = userService.getUsersCached(filteredUserIds)

        PreparedReaction(filteredUserIds.size, users.sortBy(-_.getScore).take(3).asJava, users.sizeIs > 3)
      }
      .to(TreeMap)
    )
  }
}
