/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.msg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

@Repository
public class MsgDao {
  private static final String QUERY_MESSAGE_TEXT = "SELECT message, markup FROM msgbase WHERE id=?";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Cacheable(value = "Msgs", key = "#msgid")
  public MessageText getMessageText(int msgid) {
    return jdbcTemplate.queryForObject(QUERY_MESSAGE_TEXT, new RowMapper<MessageText>() {
      @Override
      public MessageText mapRow(ResultSet resultSet, int i) throws SQLException {
        String text = resultSet.getString("message");
        String markup = resultSet.getString("markup");
        boolean lorcode = !"PLAIN".equals(markup);

        return new MessageText(text, lorcode);
      }
    }, msgid);
  }

  public Map<Integer, MessageText> getMessageText(Collection<Integer> msgids) {
    if (msgids.isEmpty()) {
      return ImmutableMap.of();
    }

    final Map<Integer, MessageText> out = Maps.newHashMapWithExpectedSize(msgids.size());

    for(Integer msgid : msgids) {
      out.put(msgid, getMessageText(msgid));
    }
    return out;
  }

  @CacheEvict(value = "Msgs", key = "#msgid")
  public void updateMessage(int msgid, String text) {
    jdbcTemplate.update(
        "UPDATE msgbase SET message=? WHERE id=?", text, msgid
    );
  }

  @CacheEvict(value = "Msgs", key = "#msgid")
  public void appendMessage(int msgid, String text) {
    jdbcTemplate.update(
            "UPDATE msgbase SET message=message||? WHERE id=?",
            text,
            msgid
    );
  }

  /**
   * Добавление нового тела сообщения. Не кешируется потому, что новое. Поопадет в кеш при первом чтении.
   * @param msgid
   * @param text
   */
  public void addMessage(int msgid, String text) {
    jdbcTemplate.update("INSERT INTO msgbase(id, message) values(?,?)", msgid, text);
  }
}