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

package ru.org.linux.topic

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.tag.TagInfo

import java.sql.ResultSet
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class TopicTagDao(ds: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val namedJdbcTemplate = new NamedParameterJdbcTemplate(ds)

  /**
   * Добавление тега к топику.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  def addTag(msgId: Int, tagId: Int): Unit = transactional() { _ =>
    val inserted = jdbcTemplate.update("INSERT INTO tags VALUES(?,?) ON CONFLICT DO NOTHING", msgId, tagId)

    if (inserted>0) {
      jdbcTemplate.update("UPDATE tags_values SET counter = counter + 1 WHERE id = ?", tagId)
    }
  }

  /**
   * Удаление тега у топика.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  def deleteTag(msgId: Int, tagId: Int): Unit = {
    jdbcTemplate.update("DELETE FROM tags WHERE msgid=? and tagid=?", msgId, tagId)
  }

  /**
   * Получить список тегов топика.
   *
   * @param msgid идентификационный номер топика
   * @return список тегов топика
   */
  def getTags(msgid: Int): Seq[TagInfo] = {
    jdbcTemplate.queryAndMap(
      "SELECT tags_values.value, tags_values.counter, tags_values.id FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value",
      msgid
    ) { (rs, _) => TagInfo(rs.getString("value"), rs.getInt("counter"), rs.getInt("id")) }
  }

  /**
   * Получение количества тегов, которые будут изменены для топиков (величина прироста использования тега).
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   * @return величина прироста использования тега
   */
  def getCountReplacedTags(oldTagId:Int, newTagId:Int):Int = {
    jdbcTemplate.queryForSeq[Integer](
      "SELECT count (tagid) FROM tags WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      oldTagId,
      newTagId
    ).head
  }

  /**
   * Замена тега в топиках другим тегом.
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   */
  def replaceTag(oldTagId:Int, newTagId:Int):Unit = {
    jdbcTemplate.update(
      "UPDATE tags SET tagid=? WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      newTagId,
      oldTagId,
      newTagId
    )
  }

  /**
   * Удаление тега из топиков.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTag(tagId: Int): Unit = {
    jdbcTemplate.update("DELETE FROM tags WHERE tagid=?", tagId)
  }

  /**
   * пересчёт счётчиков использования.
   */
  def reCalculateAllCounters(): Unit = {
    jdbcTemplate.update(
      """update tags_values set counter =
        | (select count(*) from tags join topics on tags.msgid=topics.id
        |   join groups on topics.groupid = groups.id
        |   join sections on sections.id = groups.section
        |   where tags.tagid=tags_values.id and not deleted and (topics.moderate or not sections.moderate))"""
      .stripMargin)
  }

  def getTags(topics: collection.Seq[Int]): collection.Seq[(Int, TagInfo)] = {
    if (topics.isEmpty) {
      Vector.empty
    } else {
      namedJdbcTemplate.query(
        "SELECT msgid, tags_values.value, tags_values.counter, tags_values.id FROM tags, tags_values WHERE tags.msgid in (:list) AND tags_values.id=tags.tagid ORDER BY value",
        Map("list" -> topics.asJava).asJava,
        new RowMapper[(Int, TagInfo)]() {
          def mapRow(resultSet: ResultSet, rowNum: Int): (Int, TagInfo) =
            resultSet.getInt("msgid") -> TagInfo(
              resultSet.getString("value"),
              resultSet.getInt("counter"),
              resultSet.getInt("id")
            )
        }).asScala
    }
  }

  /**
   * Увеличить счётчик использования тега.
   *
   * @param tagId    идентификационный номер тега
   * @param tagCount на какое значение изменить счётчик
   */
  def increaseCounterById(tagId: Int, tagCount: Int):Unit = {
    jdbcTemplate.update("UPDATE tags_values SET counter=counter+? WHERE id=?", tagCount, tagId)
  }

  def processTopicsByTag(tagId: Int, f: Int => Unit): Unit = {
    jdbcTemplate.queryAndProcess("SELECT msgid FROM tags WHERE tags.tagid=?", tagId) { rs => f(rs.getInt(1)) }
  }

  def getTagSectoins(tagId: Int): Seq[Int] = {
    jdbcTemplate.queryForSeq[Int](
    """select distinct section from
        | groups join topics on topics.groupid=groups.id join tags on tags.msgid = topics.id
        | where tagid=? and not deleted""".stripMargin, tagId)
  }
}
