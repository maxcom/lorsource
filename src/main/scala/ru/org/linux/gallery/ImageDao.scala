/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.gallery

import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.gallery.ImageDao.galleryItemRowMapper
import ru.org.linux.section.Section
import ru.org.linux.section.SectionService
import ru.org.linux.topic.Topic

import javax.annotation.Nullable
import javax.sql.DataSource
import java.sql.ResultSet
import scala.jdk.CollectionConverters.MapHasAsJava

object ImageDao {
  private def imageRowMapper(rs: ResultSet, i: Int): Image = {
    val imageid = rs.getInt("id")
    new Image(imageid, rs.getInt("topic"), "images/" + imageid + "/original." + rs.getString("extension"), rs.getBoolean("deleted"))
  }

  private def galleryItemRowMapper(gallery: Section)(rs: ResultSet, rowNum: Int): GalleryItem = {
    val item = new GalleryItem

    item.setMsgid(rs.getInt("msgid"))
    item.setStat(rs.getInt("stat1"))
    item.setTitle(rs.getString("title"))
    item.setCommitDate(rs.getTimestamp("commitdate"))
    val imageid = rs.getInt("imageid")

    val image = new Image(imageid, rs.getInt("msgid"), "images/" + imageid + "/original." + rs.getString("extension"), deleted = false)

    item.setImage(image)
    item.setUserid(rs.getInt("userid"))
    item.setStat(rs.getInt("stat1"))
    item.setLink(gallery.getSectionLink + rs.getString("urlname") + '/' + rs.getInt("msgid"))

    item
  }
}

@Repository
class ImageDao(private val sectionService: SectionService, dataSource: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(dataSource)

  private val jdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("images")
    .usingColumns("topic", "extension").usingGeneratedKeyColumns("id")

  /**
   * Возвращает последние объекты галереи.
   *
   * @return список GalleryDto объектов
   */
  def getGalleryItems(countItems: Int): Seq[GalleryItem] = {
    val gallery = sectionService.getSection(Section.SECTION_GALLERY)
    val sql =
      s"""SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.extension, images.id AS imageid, t.commitdate
         |FROM
         |  (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate
         |    FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate
         |     AND section=${Section.SECTION_GALLERY} AND NOT topics.deleted AND commitdate IS NOT NULL
         |     ORDER BY commitdate DESC LIMIT ?) as t JOIN images ON t.msgid = images.topic
         |WHERE NOT images.deleted ORDER BY commitdate DESC""".stripMargin

    jdbcTemplate.queryAndMap(sql, countItems)(galleryItemRowMapper(gallery))
  }

  /**
   * Возвращает последние объекты галереи.
   */
  def getGalleryItems(countItems: Int, tagId: Int): Seq[GalleryItem] = {
    val gallery = sectionService.getSection(Section.SECTION_GALLERY)

    val sql =
      s"""SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.extension, images.id AS imageid, t.commitdate
         |FROM
         |  (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate
         |    FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate
         |      AND section=${Section.SECTION_GALLERY} AND NOT topics.deleted AND commitdate IS NOT NULL AND
         |      topics.id IN (SELECT msgid FROM tags WHERE tagid=?) ORDER BY commitdate DESC LIMIT ?) as t
         |  JOIN images ON t.msgid = images.topic
         |WHERE NOT images.deleted""".stripMargin

    jdbcTemplate.queryAndMap(sql, tagId, countItems)(galleryItemRowMapper(gallery))
  }

  @Nullable
  def imageForTopic(topic: Topic): Image = {
    val found = jdbcTemplate.queryAndMap(
        "SELECT id, topic, extension, deleted FROM images WHERE topic=? AND NOT deleted",
        topic.id)(ImageDao.imageRowMapper)

    if (found.isEmpty) {
      null
    } else if (found.size == 1) {
      found.head
    } else {
      throw new RuntimeException(s"Too many images for topic=${topic.id}")
    }
  }

  def getImage(id: Int): Image =
    jdbcTemplate.queryForObjectAndMap("SELECT id, topic, extension, deleted FROM images WHERE id=?",
      id)(ImageDao.imageRowMapper).getOrElse(throw new RuntimeException("Image not found: " + id))

  def saveImage(topicId: Int, extension: String): Int = {
    val dataMap: Map[String, Any] = Map("topic" -> topicId, "extension" -> extension)

    jdbcInsert.executeAndReturnKey(dataMap.asJava).intValue
  }

  def deleteImage(image: Image): Unit = jdbcTemplate.update("UPDATE images SET deleted='true' WHERE id=?", image.id)
}