/*
 * Copyright 1998-2022 Linux.org.ru
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

import java.sql.ResultSet
import java.util
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class EditHistoryDao(dataSource: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(dataSource)
  private val editInsert =
    new SimpleJdbcInsert(dataSource)
      .withTableName("edit_info")
      .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl",
        "object_type", "oldminor", "oldimage", "oldpoll")

  private def parseEditHistoryRecord(resultSet: ResultSet) = {
    val editHistoryRecord = new EditHistoryRecord
    editHistoryRecord.setId(resultSet.getInt("id"))
    editHistoryRecord.setMsgid(resultSet.getInt("msgid"))
    editHistoryRecord.setEditor(resultSet.getInt("editor"))
    editHistoryRecord.setOldmessage(resultSet.getString("oldmessage"))
    editHistoryRecord.setEditdate(resultSet.getTimestamp("editdate"))
    editHistoryRecord.setOldtitle(resultSet.getString("oldtitle"))
    editHistoryRecord.setOldtags(resultSet.getString("oldtags"))
    editHistoryRecord.setObjectType(resultSet.getString("object_type"))

    editHistoryRecord.setOldimage(resultSet.getInt("oldimage"))
    if (resultSet.wasNull) editHistoryRecord.setOldimage(null)

    editHistoryRecord.setOldminor(resultSet.getBoolean("oldminor"))
    if (resultSet.wasNull) editHistoryRecord.setOldminor(null)

    Option(resultSet.getString("oldpoll")).map { json =>
      parse(json).toTry.flatMap(_.as[Poll].toTry).get
    }.foreach(editHistoryRecord.setOldPoll)

    editHistoryRecord
  }

  /**
   * Получить информации о редактировании топика/комментария.
   *
   * @param id             id топика
   * @param objectTypeEnum тип: топик или комментарий
   * @return список изменений топика
   */
  def getEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): util.List[EditHistoryRecord] = {
    jdbcTemplate.query("SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC", (resultSet: ResultSet, i: Int) => {
      parseEditHistoryRecord(resultSet)
    }, id, objectTypeEnum.toString)
  }

  def getEditRecord(msgid: Int, recordId: Int, objectTypeEnum: EditHistoryObjectTypeEnum): EditHistoryRecord = {
    jdbcTemplate.queryForObject("SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type AND id=?", (resultSet: ResultSet, i: Int) => {
      parseEditHistoryRecord(resultSet)
    }, msgid, objectTypeEnum.toString, recordId)
  }

  def getBriefEditInfo(id: Int, objectTypeEnum: EditHistoryObjectTypeEnum): util.List[BriefEditInfo] = {
    jdbcTemplate.query("SELECT editdate, editor FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC",
      (rs: ResultSet, rowNum: Int) =>
        BriefEditInfo(rs.getTimestamp("editdate"), rs.getInt("editor")), id, objectTypeEnum.toString)
  }

  def insert(record: EditHistoryRecord): Unit = {
    editInsert.execute(Map(
      "msgid" -> record.getMsgid,
      "editor" -> record.getEditor,
      "oldmessage" -> record.getOldmessage,
      "oldtitle" -> record.getOldtitle,
      "oldtags" -> record.getOldtags,
      "oldlinktext" -> record.getOldlinktext,
      "oldurl" -> record.getOldurl,
      "object_type" -> record.getObjectType,
      "oldminor" -> record.getOldminor,
      "oldimage" -> record.getOldimage,
      "oldpoll" -> Option(record.getOldPoll).map(_.asJson).orNull
    ).asJava)
  }
}