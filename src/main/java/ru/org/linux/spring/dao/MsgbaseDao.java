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

package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class MsgbaseDao {
  /**
   * Запрос тела сообщения и признака bbcode для сообщения
   */
  private static final String QUERY_MESSAGE_TEXT = "SELECT message, bbcode FROM msgbase WHERE id=?";

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  public MessageText getMessageText(int msgid) {
    return jdbcTemplate.queryForObject(QUERY_MESSAGE_TEXT, new RowMapper<MessageText>() {
      @Override
      public MessageText mapRow(ResultSet resultSet, int i) throws SQLException {
        String text = resultSet.getString("message");
        boolean lorcode = resultSet.getBoolean("bbcode");
        
        return new MessageText(text, lorcode);
      }
    }, msgid);
  }                  
  
  public void updateMessage(int msgid, String text) {
    namedJdbcTemplate.update(
      "UPDATE msgbase SET message=:message WHERE id=:msgid",
      ImmutableMap.of("message", text, "msgid", msgid)
    );
  }

  public void appendMessage(int msgid, String text) {
    jdbcTemplate.update(
            "UPDATE msgbase SET message=message||? WHERE id=?",
            text,
            msgid
    );
  }
}