/*
 * Copyright 1998-2012 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class EditHistoryDao {
  private static final String queryEditInfo = "SELECT * FROM edit_info WHERE msgid=? AND object_type = ?::edit_event_type ORDER BY id DESC";

  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert editInsert;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);

      editInsert =
      new SimpleJdbcInsert(dataSource)
        .withTableName("edit_info")
        .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl", "object_type");

  }

  /**
   * Получить информации о редактировании топика.
   *
   * @param id id топика
   * @param objectTypeEnum
   * @return список изменений топика
   */
  public List<EditHistoryDto> getEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    final List<EditHistoryDto> editInfoDTOs = new ArrayList<EditHistoryDto>();
    jdbcTemplate.query(queryEditInfo, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        EditHistoryDto editHistoryDto = new EditHistoryDto();
        editHistoryDto.setId(resultSet.getInt("id"));
        editHistoryDto.setMsgid(resultSet.getInt("msgid"));
        editHistoryDto.setEditor(resultSet.getInt("editor"));
        editHistoryDto.setOldmessage(resultSet.getString("oldmessage"));
        editHistoryDto.setEditdate(resultSet.getTimestamp("editdate"));
        editHistoryDto.setOldtitle(resultSet.getString("oldtitle"));
        editHistoryDto.setOldtags(resultSet.getString("oldtags"));
        editHistoryDto.setObjectType(resultSet.getString("object_type"));
        editInfoDTOs.add(editHistoryDto);
      }
    },
      id,
      objectTypeEnum.toString()
    );
    return editInfoDTOs;
  }

  /**
   *
   * @param editHistoryDto
   */
  public void insert(EditHistoryDto editHistoryDto) {
    editInsert.execute(new BeanPropertySqlParameterSource(editHistoryDto));
  }
}
