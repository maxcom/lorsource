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
      deleted = rs.boolean("deleted"),
      purged = rs.boolean("purged"))

  private def galleryItemFromRs(rs: WrappedResultSet, gallery: Section): GalleryItem =
    val imageid = rs.int("imageid")
    val extension = rs.string("extension")
    val msgid = rs.int("msgid")
    val image = Image(imageid, msgid, s"images/$imageid/original.$extension", deleted = false, purged = false)

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

  /** Активные (не удалённые) изображения топика.
    *
    * Фильтр `AND NOT purged` — defence-in-depth: для активных топиков purged-рядов не существует
    * по контракту чистильщика (OldImageCleaner чистит только удалённые топики или уже
    * `deleted=true` изображения неактивных топиков, которые здесь и так отфильтрованы).
    * Дополнительно блокирует `prepareImage` от попыток читать отсутствующие файлы,
    * если инвариант нарушен.
    */
  def allImagesForTopic(topicId: Int): Seq[Image] =
    springDB.run:
      sql"SELECT id, topic, extension, deleted, purged FROM images WHERE topic=$topicId AND NOT deleted AND NOT purged ORDER BY main desc, id"
        .map(imageFromRs)
        .list
        .apply()

  def getImage(id: Int): Image =
    springDB.run:
      sql"SELECT id, topic, extension, deleted, purged FROM images WHERE id=$id"
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

  /** Изображения удалённых топиков, удалённых более чем `years` лет назад (по `del_info.deldate`).
    * Файлы этих изображений подлежат физическому удалению, независимо от флага `images.deleted`.
    */
  def imagesOfOldDeletedTopics(years: Int): Seq[Image] =
    springDB.run:
      sql"""SELECT i.id, i.topic, i.extension, i.deleted, i.purged
            FROM images i
            JOIN topics t ON t.id = i.topic
            JOIN del_info d ON d.msgid = t.id
            WHERE t.deleted
              AND d.deldate < CURRENT_TIMESTAMP - make_interval(years => $years)
              AND NOT i.purged
            ORDER BY i.id""".map(imageFromRs).list.apply()

  /** Soft-deleted изображения (`images.deleted=true`) активных топиков, у которых
    * `topics.lastmod` старее чем `years` лет. Удалению подлежат только файлы таких картинок.
    */
  def deletedImagesOfOldTopics(years: Int): Seq[Image] =
    springDB.run:
      sql"""SELECT i.id, i.topic, i.extension, i.deleted, i.purged
            FROM images i
            JOIN topics t ON t.id = i.topic
            WHERE i.deleted
              AND NOT t.deleted
              AND t.lastmod < CURRENT_TIMESTAMP - make_interval(years => $years)
              AND NOT i.purged
            ORDER BY i.id""".map(imageFromRs).list.apply()

  /** Размер батча для `IN (...)` в `markPurged`. Ограничивает число bind-параметров в одном
    * prepared statement с запасом под лимит PostgreSQL (65535). Помещает большие батчи
    * (десятки тысяч изображений при первом запуске чистильщика) в безопасный диапазон.
    */
  private val MarkPurgedBatchSize = 1000

  /** Пометить изображения как физически удалённые (файлы отсутствуют на диске).
    * Идемпотентно: повторная очистка пропускает уже помеченные строки.
    *
    * При большом числе `ids` (десятки тысяч на первом запуске) разбивает запрос на
    * батчи по `MarkPurgedBatchSize`, чтобы не превысить лимит числа параметров
    * prepared statement в PostgreSQL (65535). Все батчи выполняются в одной
    * транзакции (внешней, предоставленной вызывающей стороной), поэтому атомарность
    * сохраняется: либо все батчи коммитятся, либо ни один.
    */
  def markPurged(ids: Seq[Int])(using Transaction): Unit =
    if ids.nonEmpty then
      ids.grouped(MarkPurgedBatchSize).foreach { chunk =>
        sql"UPDATE images SET purged='true' WHERE id IN ($chunk)".update.apply()
      }
