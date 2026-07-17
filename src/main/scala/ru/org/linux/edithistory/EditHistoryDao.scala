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
package ru.org.linux.edithistory

import io.circe.parser.*
import io.circe.syntax.*
import org.springframework.stereotype.Repository
import ru.org.linux.poll.Poll
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.tag.{TagName, TagService}
import scalikejdbc.*

import java.sql.PreparedStatement
import java.time.Instant

case class EditHistoryRecord(
    id: Int = 0,
    msgid: Int,
    editor: Int,
    objectType: EditHistoryObjectTypeEnum,
    editdate: Instant = Instant.now,
    oldmessage: Option[String] = None,
    oldtitle: Option[String] = None,
    oldtags: Option[Seq[String]] = None,
    oldlinktext: Option[String] = None,
    oldurl: Option[String] = None,
    oldminor: Option[Boolean] = None,
    oldPoll: Option[Poll] = None,
    oldaddimages: Option[Seq[Int]] = None,
    /** Легаси-поле `edit_info.oldimage`. Сохраняется как есть,
      * без слияния с `oldaddimages`, чтобы `EditHistoryService` мог различать.
      */
    legacyMainImage: Option[Int] = None)

@Repository
class EditHistoryDao(springDB: SpringDB):

  private def parseEditHistoryRecord(rs: WrappedResultSet): EditHistoryRecord =
    val oldaddimages = rs
      .arrayOpt("oldaddimages")
      .map(_.getArray.asInstanceOf[Array[Integer]].toSeq.map(_.toInt))

    EditHistoryRecord(
      id = rs.int("id"),
      msgid = rs.int("msgid"),
      editor = rs.int("editor"),
      editdate = rs.timestamp("editdate").toInstant,
      oldmessage = rs.stringOpt("oldmessage"),
      oldtitle = rs.stringOpt("oldtitle"),
      oldtags = rs.stringOpt("oldtags").map(TagName.parseAndSanitizeTags),
      objectType = EditHistoryObjectTypeEnum.valueOf(rs.string("object_type")),
      oldurl = rs.stringOpt("oldurl"),
      oldlinktext = rs.stringOpt("oldlinktext"),
      oldminor = rs.booleanOpt("oldminor"),
      oldPoll = rs
        .stringOpt("oldpoll")
        .map { json =>
          parse(json).toTry.flatMap(_.as[Poll].toTry).get
        },
      oldaddimages = oldaddimages,
      legacyMainImage = rs.intOpt("oldimage")
    )

  /** Получить информации о редактировании топика/комментария.
    *
    * @param id
    *   id топика
    * @param objectTypeEnum
    *   тип: топик или комментарий
    * @return
    *   список изменений топика
    */
  def getEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Seq[EditHistoryRecord] =
    springDB.run:
      sql"SELECT * FROM edit_info WHERE msgid=$id AND object_type = ${objectTypeEnum
          .toString}::edit_event_type ORDER BY id DESC".map(parseEditHistoryRecord).list.apply()

  def getEditRecord(msgid: Int, recordId: Int, objectTypeEnum: EditHistoryObjectTypeEnum): EditHistoryRecord =
    springDB.run:
      sql"SELECT * FROM edit_info WHERE msgid=$msgid AND object_type = ${objectTypeEnum
          .toString}::edit_event_type AND id=$recordId"
        .map(parseEditHistoryRecord)
        .single
        .apply()
        .getOrElse(throw new RuntimeException(s"Edit record $recordId not found for msgid=$msgid"))

  def getBriefEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): Seq[BriefEditInfo] =
    springDB.run:
      sql"SELECT editdate, editor FROM edit_info WHERE msgid=$id AND object_type = ${objectTypeEnum
          .toString}::edit_event_type ORDER BY id DESC"
        .map(rs => BriefEditInfo(rs.underlying.getTimestamp("editdate"), rs.int("editor")))
        .list
        .apply()

  def insert(record: EditHistoryRecord)(using Transaction): Unit =
    val oldPollStr = record.oldPoll.map(_.asJson.noSpaces)
    val oldaddimagesBinder: Option[ParameterBinder] = record
      .oldaddimages
      .map: seq =>
        val arr = seq.map(_.asInstanceOf[Integer]).toArray.asInstanceOf[Array[Object]]
        ParameterBinder(
          value = arr,
          binder =
            (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("integer", arr)))
    sql"""INSERT INTO edit_info (msgid, editor, oldmessage, oldtitle, oldtags, oldlinktext, oldurl,
          object_type, oldminor, oldpoll, oldaddimages)
          VALUES (${record.msgid}, ${record.editor}, ${record.oldmessage}, ${record.oldtitle},
          ${record.oldtags.map(TagService.tagsToString)}, ${record.oldlinktext}, ${record.oldurl},
          ${record.objectType.toString}::edit_event_type, ${record.oldminor},
          $oldPollStr, $oldaddimagesBinder)""".update.apply()
