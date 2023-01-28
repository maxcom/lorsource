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
package ru.org.linux.user

import javax.sql.DataSource

import com.google.common.collect.ImmutableMap
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.{Propagation, Transactional}
import ru.org.linux.topic.Topic
import scala.compat.java8.OptionConverters._

@Repository
class MemoriesDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val insertTemplate = {
    new SimpleJdbcInsert(ds)
      .withTableName("memories")
      .usingGeneratedKeyColumns("id")
      .usingColumns("userid", "topic", "watch")
  }

  def addToMemories(user: User, topic: Topic, watch: Boolean): Int = try {
    doAddToMemories(user, topic, watch)
  } catch {
    case _: DuplicateKeyException =>
      getId(user, topic.id, watch)
  }

  @Transactional(rollbackFor = Array(classOf[Exception]), propagation = Propagation.REQUIRED)
  private def doAddToMemories(user: User, topic: Topic, watch: Boolean) = {
    val id = getId(user, topic.id, watch)

    if (id == 0) {
      insertTemplate.executeAndReturnKey(ImmutableMap.of("userid", user.getId,
        "topic", topic.id, "watch", watch)).intValue
    } else {
      id
    }
  }

  /**
    * Get memories id or 0 if not in memories
    */
  private def getId(user: User, topic: Int, watch: Boolean): Int = {
    val res = jdbcTemplate.queryForSeq[Int]("SELECT id FROM memories WHERE userid=? AND topic=? AND watch=?",
      user.getId, topic, watch)

    res.headOption.getOrElse(0)
  }

  /**
    * get number of memories/favs for topic
    */
  def getTopicInfo(topic: Int, currentUser: User): MemoriesInfo = {
    val res: MemoriesInfo = jdbcTemplate.queryAndMap("SELECT watch, count(*) FROM memories WHERE topic=? GROUP BY watch", topic){ (rs, _) =>
      if (rs.getBoolean("watch")) {
        MemoriesInfo(watchCount = rs.getInt("count"), favsCount = 0, watchId = 0, favId = 0)
      } else {
        MemoriesInfo(watchCount = 0, favsCount = rs.getInt("count"), watchId = 0, favId = 0)
      }
    }.fold(MemoriesInfo(0, 0, 0, 0)) { (acc, cur) =>
      MemoriesInfo(
        watchCount = acc.watchCount + cur.watchCount,
        favsCount = acc.favsCount + cur.favsCount,
        watchId = 0,
        favId = 0)
    }

    if (currentUser != null) {
      val ids = jdbcTemplate.queryAndMap("SELECT id, watch FROM memories WHERE userid=? AND topic=?", currentUser.getId, topic) {
        (rs, _) => rs.getInt("id") -> rs.getBoolean("watch")
      }

      var watchId = 0
      var favsId = 0

      for (p <- ids) {
        if (p._2) watchId = p._1
        else favsId = p._1
      }

      MemoriesInfo(watchCount = res.watchCount, favsCount = res.favsCount, watchId = watchId, favId = favsId)
    } else {
      res
    }
  }

  def getMemoriesListItem(id: Int): java.util.Optional[MemoriesListItem] = {
    val res = jdbcTemplate.queryAndMap("SELECT * FROM memories WHERE id=?", id) {
      (rs, _) => new MemoriesListItem(rs)
    }

    res.headOption.asJava
  }

  def delete(id: Int): Unit = jdbcTemplate.update("DELETE FROM memories WHERE id=?", id)

  def isWatchPresetForUser(user: User): Boolean = checkMemoriesPresent(user, watch = true)

  def isFavPresetForUser(user: User): Boolean = checkMemoriesPresent(user, watch = false)

  private def checkMemoriesPresent(user: User, watch: Boolean) = {
    val present = jdbcTemplate.queryForSeq[Int]("select memories.id from memories join topics on memories.topic=topics.id " +
      "where memories.userid=? and watch=? and not deleted limit 1", user.getId, watch)

    present.nonEmpty
  }

  /**
    * get number of watch memories for user
    *
    * @param user user
    * @return count memories
    */
  def getWatchCountForUser(user: User): Int = {
    val ret = jdbcTemplate.queryForSeq[Int](
      "select count(id) from memories where userid=? and watch='t'",
      user.getId)

    ret.headOption.getOrElse(0)
  }
}