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
package ru.org.linux.edithistory

import io.circe.parser.*
import io.circe.syntax.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import ru.org.linux.poll.Poll
import ru.org.linux.tag.{TagName, TagService}

import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

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
  oldimage: Option[Int] = None,
  oldPoll: Option[Poll] = None)

@Repository
class EditHistoryDao(dataSource: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(dataSource)
  private val editInsert =
    new SimpleJdbcInsert(dataSource)
      .withTableName("edit_info")
      .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl",
        "object_type", "oldminor", "oldimage", "oldpoll")

  private def parseEditHistoryRecord(resultSet: ResultSet) = {
    EditHistoryRecord(
      id = resultSet.getInt("id"),
      msgid = resultSet.getInt("msgid"),
      editor = resultSet.getInt("editor"),
      editdate = resultSet.getTimestamp("editdate").toInstant,
      oldmessage = Option(resultSet.getString("oldmessage")),
      oldtitle = Option(resultSet.getString("oldtitle")),
      oldtags = Option(resultSet.getString("oldtags")).map(TagName.parseAndSanitizeTags),
      objectType = EditHistoryObjectTypeEnum.valueOf(resultSet.getString("object_type")),
      oldurl = Option(resultSet.getString("oldurl")),
      oldlinktext = Option(resultSet.getString("oldlinktext")),
      oldimage = {
        val oldimage=resultSet.getInt("oldimage")

        if (resultSet.wasNull) None else Some(oldimage)
      },
      oldminor = {
        val oldminor=resultSet.getBoolean("oldminor")

        if (resultSet.wasNull) None else Some(oldminor)
      },
      oldPoll = Option(resultSet.getString("oldpoll")).map { json =>
        parse(json).toTry.flatMap(_.as[Poll].toTry).get
      })
  }

  /**
   * Получить информации о редактировании топика/комментария.
   *
   * @param id             id топика
   * @param objectTypeEnum тип: топик или комментарий
   * @return список изменений топика
   */
  def getEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): collection.Seq[EditHistoryRecord] =
    jdbcTemplate.query("SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC",
      (resultSet, _: Int) => parseEditHistoryRecord(resultSet), id, objectTypeEnum.toString).asScala

  def getEditRecord(msgid: Int, recordId: Int, objectTypeEnum: EditHistoryObjectTypeEnum): EditHistoryRecord =
    jdbcTemplate.queryForObject("SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type AND id=?",
      (resultSet, _: Int) => parseEditHistoryRecord(resultSet), msgid, objectTypeEnum.toString, recordId)

  def getBriefEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): collection.Seq[BriefEditInfo] =
    jdbcTemplate.query("SELECT editdate, editor FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC",
      (rs: ResultSet, _: Int) => BriefEditInfo(rs.getTimestamp("editdate"), rs.getInt("editor")), id, objectTypeEnum.toString).asScala

  def insert(record: EditHistoryRecord): Unit = {
    editInsert.execute(Map[String, Any](
      "msgid" -> record.msgid,
      "editor" -> record.editor,
      "oldmessage" -> record.oldmessage.orNull,
      "oldtitle" -> record.oldtitle.orNull,
      "oldtags" -> record.oldtags.map(TagService.tagsToString).orNull,
      "oldlinktext" -> record.oldlinktext.orNull,
      "oldurl" -> record.oldurl.orNull,
      "object_type" -> record.objectType,
      "oldminor" -> record.oldminor.orNull,
      "oldimage" -> record.oldimage.orNull,
      "oldpoll" -> Option(record.oldPoll).map(_.asJson).orNull
    ).asJava)
  }
}