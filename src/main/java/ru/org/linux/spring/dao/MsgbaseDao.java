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

package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.markup.MarkupType$;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

@Repository
public class MsgbaseDao {
  /**
   * Запрос тела сообщения и признака bbcode для сообщения
   */
  private static final String QUERY_MESSAGE_TEXT = "SELECT message, markup FROM msgbase WHERE id=?";

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;

  private SimpleJdbcInsert insertMsgbase;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

    insertMsgbase = new SimpleJdbcInsert(dataSource);
    insertMsgbase.setTableName("msgbase");
    insertMsgbase.usingColumns("id", "message", "markup");
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void saveNewMessage(MessageText message, int msgid) {
    insertMsgbase.execute(ImmutableMap.<String, Object>of(
            "id", msgid,
            "message", message.text(),
            "markup", message.markup().id())
    );
  }

  private MessageText messageTextOf(ResultSet resultSet) throws SQLException {
    String text = resultSet.getString("message");
    String markup = resultSet.getString("markup");

    return new MessageText(text, MarkupType$.MODULE$.of(markup));
  }

  public MessageText getMessageText(int msgid) {
    return jdbcTemplate.queryForObject(QUERY_MESSAGE_TEXT, (resultSet, i) -> messageTextOf(resultSet), msgid);
  }

  public Map<Integer, MessageText> getMessageText(Collection<Integer> msgids) {
    if (msgids.isEmpty()) {
      return ImmutableMap.of();
    }

    final Map<Integer, MessageText> out = Maps.newHashMapWithExpectedSize(msgids.size());

    namedJdbcTemplate.query(
            "SELECT message, markup, id FROM msgbase WHERE id IN (:list)",
            ImmutableMap.of("list", msgids),
            resultSet -> {
              out.put(resultSet.getInt("id"), messageTextOf(resultSet));
            });

    return out;
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
