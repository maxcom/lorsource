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
package ru.org.linux.user

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.Topic
import scalikejdbc.*

import java.util as ju
import scala.jdk.OptionConverters.RichOption

@Repository
class MemoriesDao(springDB: SpringDB):

  def addToMemories(user: User, topic: Topic, watch: Boolean): Int =
    springDB.run:
      sql"""INSERT INTO memories (userid, topic, watch) VALUES (${user.id}, ${topic.id}, $watch)
            ON CONFLICT (userid, topic, watch) DO UPDATE SET topic=${topic.id}
            RETURNING id""".map(rs => rs.int("id")).single.apply().get

  def getTopicInfo(topic: Int, currentUserOpt: Option[User]): MemoriesInfo =
    val res = springDB.run:
      sql"SELECT watch, count(*) FROM memories WHERE topic=$topic GROUP BY watch"
        .map { rs =>
          if rs.boolean("watch") then
            MemoriesInfo(watchCount = rs.int("count"), favsCount = 0, watchId = 0, favId = 0)
          else
            MemoriesInfo(watchCount = 0, favsCount = rs.int("count"), watchId = 0, favId = 0)
        }
        .list
        .apply()
        .foldLeft(MemoriesInfo(0, 0, 0, 0)) { (acc, cur) =>
          MemoriesInfo(
            watchCount = acc.watchCount + cur.watchCount,
            favsCount = acc.favsCount + cur.favsCount,
            watchId = 0,
            favId = 0)
        }

    currentUserOpt match
      case Some(currentUser) =>
        val ids = springDB.run:
          sql"SELECT id, watch FROM memories WHERE userid=${currentUser.id} AND topic=$topic"
            .map(rs => rs.int("id") -> rs.boolean("watch"))
            .list
            .apply()

        var watchId = 0
        var favsId = 0
        for (id, w) <- ids do
          if w then
            watchId = id
          else
            favsId = id

        MemoriesInfo(watchCount = res.watchCount, favsCount = res.favsCount, watchId = watchId, favId = favsId)
      case None =>
        res

  def getMemoriesListItem(id: Int): ju.Optional[MemoriesListItem] =
    val result = springDB.run:
      sql"SELECT * FROM memories WHERE id=$id".map(rs => MemoriesListItem(rs.underlying)).single.apply()
    result.toJava

  def delete(id: Int): Unit =
    springDB.run:
      sql"DELETE FROM memories WHERE id=$id".update.apply()

  def isWatchPresetForUser(user: User): Boolean = checkMemoriesPresent(user, watch = true)

  def isFavPresetForUser(user: User): Boolean = checkMemoriesPresent(user, watch = false)

  private def checkMemoriesPresent(user: User, watch: Boolean): Boolean =
    springDB.run:
      sql"""SELECT memories.id FROM memories JOIN topics ON memories.topic=topics.id
            WHERE memories.userid=${user.id} AND watch=$watch AND NOT deleted LIMIT 1"""
        .map(rs => rs.int("id"))
        .single
        .apply()
        .isDefined

  def getWatchCountForUser(user: User): Int =
    springDB.run:
      sql"SELECT count(id) FROM memories WHERE userid=${user.id} AND watch='t'"
        .map(rs => rs.int("count"))
        .single
        .apply()
        .getOrElse(0)
