/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.DeleteInfo;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Получение информации кем и почему удален топик
 */
@Repository
public class DeleteInfoDao {
  private JdbcTemplate jdbcTemplate;
  private static final String queryDeleteInfo = "SELECT nick,reason,users.id as userid, deldate FROM del_info,users WHERE msgid=? AND users.id=del_info.delby";

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Кто, когда и почему удалил сообщение
   * @param id id проверяемого сообщения
   * @return информация о удаленном сообщении
   */
  public DeleteInfo getDeleteInfo(int id) {
    List<DeleteInfo> list = jdbcTemplate.query(queryDeleteInfo, new RowMapper<DeleteInfo>() {
      @Override
      public DeleteInfo mapRow(ResultSet resultSet, int i) throws SQLException {
        return new DeleteInfo(
                resultSet.getString("nick"),
                resultSet.getInt("userid"),
                resultSet.getString("reason"),
                resultSet.getTimestamp("deldate"));
      }
    }, id);

    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }
}
