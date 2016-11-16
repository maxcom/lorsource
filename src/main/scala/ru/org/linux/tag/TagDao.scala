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

package ru.org.linux.tag

import java.sql.ResultSet
import javax.sql.DataSource

import com.typesafe.scalalogging.StrictLogging
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.tag.TagDao._

import scala.collection.JavaConversions._

@Repository
class TagDao(ds:DataSource) extends StrictLogging {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val simpleJdbcInsert =
    new SimpleJdbcInsert(ds).withTableName("tags_values").usingColumns("value").usingGeneratedKeyColumns("id")

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   * @return tag id
   */
  def createTag(tagName: String): Int = {
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")

    val id = simpleJdbcInsert.executeAndReturnKey(Map("value" -> tagName)).intValue
    logger.debug(s"Создан тег: '$tagName' id=$id")
    id
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  def changeTag(tagId: Int, tagName: String):Unit = {
    jdbcTemplate.update("UPDATE tags_values set value=? WHERE id=?", tagName, tagId)
  }

  /**
   * Удалить тег.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTag(tagId: Int):Unit = {
    jdbcTemplate.update("DELETE FROM tags_values WHERE id=?", tagId)
  }

  /**
   * Получение списка первых букв тегов.
   *
   * @return список первых букв тегов.
   */
  private[tag] def getFirstLetters: Seq[String] = {
    val query =
      "select distinct firstchar from " +
        "(select lower(substr(value,1,1)) as firstchar from tags_values " +
        "where counter > 0 order by firstchar) firstchars"

    val letters = jdbcTemplate.queryForSeq[String](query)

    letters.sorted
  }

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  private[tag] def getTagsByPrefix(prefix: String, minCount: Int): Seq[TagInfo] = {
    jdbcTemplate.queryAndMap(
      "select counter, value, id from tags_values " +
        "where value like ? and counter >= ?  " +
        "order by value",
      escapeLikeWildcards(prefix) + "%", minCount
    ) (tagInfoMapper)
  }

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  private[tag] def getTopTagsByPrefix(prefix: String, minCount: Int, count: Int): Seq[String] = {
    val query = "select value from tags_values " +
      "where value like ? and counter >= ? order by counter DESC LIMIT ?"

    val tags = jdbcTemplate.queryForSeq[String](query,
      escapeLikeWildcards(prefix) + "%", minCount, count)

    tags.sorted
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @param skipZero пропускать неиспользуемые теги
   * @return идентификационный номер
   */
  def getTagId(tag: String, skipZero: Boolean): Option[Int] = {
    try {
      jdbcTemplate.queryForObject[Integer](
        "SELECT id FROM tags_values WHERE value=?" + (if (skipZero) " AND counter>0" else ""),
        tag
      ).map(_.toInt)
    } catch {
      case ex: EmptyResultDataAccessException => None
    }
  }

  def getTagId(tag: String):Option[Int] = getTagId(tag, skipZero = false)

  def getTagInfo(tagId: Int): TagInfo = {
    jdbcTemplate.queryForObjectAndMap(
      "SELECT counter, value, id  FROM tags_values WHERE id=?", tagId
    )(tagInfoMapper).get
  }
}

object TagDao {
  private def escapeLikeWildcards(str: String): String = {
    str.replaceAll("[_%]", "\\\\$0")
  }

  private def tagInfoMapper(rs:ResultSet, rowNum:Int) =
    TagInfo(rs.getString("value"), rs.getInt("counter"), rs.getInt("id"))
}
