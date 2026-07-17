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
package ru.org.linux.gallery

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.section.Section
import ru.org.linux.section.SectionService
import scalikejdbc.*

@Repository
class ImageDao(private val sectionService: SectionService, springDB: SpringDB):

  private def imageFromRs(rs: WrappedResultSet): Image =
    val imageid = rs.int("id")
    val extension = rs.string("extension")
    Image(
      id = imageid,
      topicId = rs.int("topic"),
      original = s"images/$imageid/original.$extension",
      deleted = rs.boolean("deleted"))

  private def galleryItemFromRs(rs: WrappedResultSet, gallery: Section): GalleryItem =
    val imageid = rs.int("imageid")
    val extension = rs.string("extension")
    val msgid = rs.int("msgid")
    val image = Image(imageid, msgid, s"images/$imageid/original.$extension", deleted = false)

    GalleryItem(
      msgid = msgid,
      userid = rs.int("userid"),
      title = rs.string("title"),
      stat = rs.int("stat1"),
      link = gallery.getSectionLink + rs.string("urlname") + '/' + msgid,
      image = image,
      commitDate = rs.timestamp("commitdate")
    )

  def getGalleryItems(countItems: Int): Seq[GalleryItem] =
    val gallery = sectionService.getSection(Section.Gallery)
    springDB.run:
      sql"""SELECT * FROM (
                SELECT DISTINCT ON (t.msgid)
                    t.msgid, t.stat1, t.title, t.userid, t.urlname,
                    images.extension, images.id AS imageid, t.commitdate
                FROM (
                    SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate
                    FROM topics
                    JOIN groups ON topics.groupid = groups.id
                    WHERE topics.moderate
                        AND section = ${Section.Gallery}
                        AND NOT topics.deleted
                        AND commitdate IS NOT NULL
                    ORDER BY commitdate DESC
                    LIMIT $countItems
                ) as t
                JOIN images ON t.msgid = images.topic
                WHERE NOT images.deleted
                ORDER BY t.msgid, images.main DESC, images.id, t.commitdate DESC
            ) AS g
            ORDER BY commitdate DESC""".map(rs => galleryItemFromRs(rs, gallery)).list.apply()

  def getGalleryItems(countItems: Int, tagId: Int): Seq[GalleryItem] =
    val gallery = sectionService.getSection(Section.Gallery)
    springDB.run:
      sql"""SELECT * FROM (
                SELECT DISTINCT ON (t.msgid)
                    t.msgid, t.stat1, t.title, t.userid, t.urlname,
                    images.extension, images.id AS imageid, t.commitdate
                FROM (
                    SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate
                    FROM topics
                    JOIN groups ON topics.groupid = groups.id
                    WHERE topics.moderate
                        AND section = ${Section.Gallery}
                        AND NOT topics.deleted
                        AND commitdate IS NOT NULL
                        AND topics.id IN (SELECT msgid FROM tags WHERE tagid = $tagId)
                    ORDER BY commitdate DESC
                    LIMIT $countItems
                ) as t
                JOIN images ON t.msgid = images.topic
                WHERE NOT images.deleted
                ORDER BY t.msgid, images.main DESC, images.id, t.commitdate DESC
            ) AS g
            ORDER BY commitdate DESC""".map(rs => galleryItemFromRs(rs, gallery)).list.apply()

  def allImagesForTopic(topicId: Int): Seq[Image] =
    springDB.run:
      sql"SELECT id, topic, extension, deleted FROM images WHERE topic=$topicId AND NOT deleted ORDER BY main desc, id"
        .map(imageFromRs)
        .list
        .apply()

  def getImage(id: Int): Image =
    springDB.run:
      sql"SELECT id, topic, extension, deleted FROM images WHERE id=$id"
        .map(imageFromRs)
        .single
        .apply()
        .getOrElse(throw ImageNotFoundException(id))

  def saveImage(topicId: Int, extension: String)(using Transaction): Int =
    sql"INSERT INTO images (topic, extension, main) VALUES ($topicId, $extension, false) RETURNING id"
      .map(rs => rs.int("id"))
      .single
      .apply()
      .get

  def deleteImage(image: Image)(using Transaction): Unit =
    sql"UPDATE images SET deleted='true' WHERE id=${image.id}".update.apply()
