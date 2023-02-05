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

package ru.org.linux.tag

import java.sql.ResultSet
import javax.sql.DataSource

import com.typesafe.scalalogging.StrictLogging
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.tag.TagDao.*

import scala.jdk.CollectionConverters.*

@Repository
class TagDao(ds: DataSource) extends StrictLogging {
  private val jdbcTemplate = new JdbcTemplate(ds)

  private val tagInsert =
    new SimpleJdbcInsert(ds)
      .withTableName("tags_values")
      .usingColumns("value")
      .usingGeneratedKeyColumns("id")

  private val tagSynonymInsert =
    new SimpleJdbcInsert(ds)
      .withTableName("tags_synonyms")
      .usingColumns("value", "tagid")

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   * @return tag id
   */
  def createTag(tagName: String): Int = {
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")

    val id = tagInsert.executeAndReturnKey(Map("value" -> tagName).asJava).intValue
    logger.debug(s"Создан тег: '$tagName' id=$id")
    id
  }

  /**
   * Создать синоним тега.
   *
   * @param tagName название синонома тега
   * @param id тег на который создаем синоним
   */
  def createTagSynonym(tagName: String, id: Int): Unit = {
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")

    tagSynonymInsert.execute(Map("value" -> tagName, "tagid" -> id).asJava)
    logger.debug(s"Создан синоним: '$tagName' id=$id")
  }

  /**
   * Изменить синоним тега.
   *
   * @param oldId старый тег
   * @param newId новый тег
   */
  def updateTagSynonym(oldId: Int, newId: Int): Unit = {
    jdbcTemplate.update("UPDATE tags_synonyms SET tagid=? WHERE tagid=?", newId, oldId)
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  def changeTag(tagId: Int, tagName: String): Unit = {
    jdbcTemplate.update("UPDATE tags_values set value=? WHERE id=?", tagName, tagId)
  }

  /**
   * Удалить тег.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTag(tagId: Int): Unit = {
    jdbcTemplate.update("DELETE FROM tags_synonyms WHERE tagid=?", tagId)
    jdbcTemplate.update("DELETE FROM tags_values WHERE id=?", tagId)
  }

  /**
   * Удалить синоним тега.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTagSynonym(tagName: String): Unit = {
    jdbcTemplate.update("DELETE FROM tags_synonyms WHERE value=?", tagName)
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
    val query =
      """select value from
        |   (select s.value, counter from tags_synonyms s join tags_values v on s.tagid=v.id where s.value like ?
        |   union all
        |   select value, counter from tags_values where value like ?) j
        | where counter>=? order by counter desc limit ?""".stripMargin

    val wildcard = escapeLikeWildcards(prefix) + "%"

    val tags = jdbcTemplate.queryForSeq[String](query, wildcard, wildcard, minCount, count)

    tags.sorted
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @param skipZero пропускать неиспользуемые теги
   * @return идентификационный номер
   */
  def getTagId(tag: String, skipZero: Boolean = false): Option[Int] = {
    try {
      jdbcTemplate.queryForObject[Integer](
        "SELECT id FROM tags_values WHERE value=?" + (if (skipZero) " AND counter>0" else ""),
        tag
      ).map(_.toInt)
    } catch {
      case _: EmptyResultDataAccessException => None
    }
  }

  /**
   * Получение идентификационного номера тега по названию из таблицы синонимов.
   *
   * @param tag      название тега
   * @return идентификационный номер
   */
  def getTagSynonymId(tag: String): Option[Int] = {
    try {
      jdbcTemplate.queryForObject[Integer](
        "SELECT tagid FROM tags_synonyms WHERE value=?",
        tag
      ).map(_.toInt)
    } catch {
      case _: EmptyResultDataAccessException => None
    }
  }

  def getTagInfo(tagId: Int): TagInfo = {
    jdbcTemplate.queryForObjectAndMap(
      "SELECT counter, value, id  FROM tags_values WHERE id=?", tagId
    )(tagInfoMapper).get
  }

  def getSynonymsFor(tagId: Int): Seq[String] = {
    jdbcTemplate.queryForSeq[String]("SELECT value FROM tags_synonyms WHERE tagid=?", tagId)
  }
}

object TagDao {
  private def escapeLikeWildcards(str: String): String = {
    str.replaceAll("[_%]", "\\\\$0")
  }

  private def tagInfoMapper(rs:ResultSet, rowNum:Int) =
    TagInfo(rs.getString("value"), rs.getInt("counter"), rs.getInt("id"))
}
