/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.edithistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class EditHistoryDao {
  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert editInsert;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);

      editInsert =
      new SimpleJdbcInsert(dataSource)
        .withTableName("edit_info")
        .usingColumns(
                "msgid",
                "editor",
                "oldmessage",
                "oldtitle",
                "oldtags",
                "oldlinktext",
                "oldurl",
                "object_type",
                "oldminor",
                "oldimage"
        );

  }

  /**
   * Получить информации о редактировании топика/комментария.
   *
   * @param id id топика
   * @param objectTypeEnum тип: топик или комментарий
   * @return список изменений топика
   */
  public List<EditHistoryRecord> getEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return jdbcTemplate.query(
            "SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC",
            (resultSet, i) -> {
              EditHistoryRecord editHistoryRecord = new EditHistoryRecord();
              editHistoryRecord.setId(resultSet.getInt("id"));
              editHistoryRecord.setMsgid(resultSet.getInt("msgid"));
              editHistoryRecord.setEditor(resultSet.getInt("editor"));
              editHistoryRecord.setOldmessage(resultSet.getString("oldmessage"));
              editHistoryRecord.setEditdate(resultSet.getTimestamp("editdate"));
              editHistoryRecord.setOldtitle(resultSet.getString("oldtitle"));
              editHistoryRecord.setOldtags(resultSet.getString("oldtags"));
              editHistoryRecord.setObjectType(resultSet.getString("object_type"));

              editHistoryRecord.setOldimage(resultSet.getInt("oldimage"));
              if (resultSet.wasNull()) {
                editHistoryRecord.setOldimage(null);
              }

              editHistoryRecord.setOldminor(resultSet.getBoolean("oldminor"));
              if (resultSet.wasNull()) {
                editHistoryRecord.setOldminor(null);
              }

              return editHistoryRecord;
            },
            id,
            objectTypeEnum.toString()
    );
  }

  /**
   * Получить информации о редактировании топика/комментария.
   *
   * @param id id топика
   * @param objectTypeEnum тип: топик или комментарий
   * @return список изменений топика
   */
  public List<BriefEditInfo> getBriefEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return jdbcTemplate.query(
            "SELECT editdate, editor FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC",
            (rs, rowNum) -> new BriefEditInfo(rs.getTimestamp("editdate"), rs.getInt("editor")),
            id,
            objectTypeEnum.toString()
    );
  }

  public void insert(EditHistoryRecord editHistoryRecord) {
    editInsert.execute(new BeanPropertySqlParameterSource(editHistoryRecord));
  }
}
