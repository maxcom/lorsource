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

package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.topic.Topic;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MemoriesDao {
  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert insertTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
    insertTemplate = new SimpleJdbcInsert(ds).withTableName("memories").usingGeneratedKeyColumns("id").usingColumns("userid", "topic");
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int addToMemories(int userid, int topic) {
    List<Integer> res = jdbcTemplate.queryForList(
            "SELECT id FROM memories WHERE userid=? AND topic=? FOR UPDATE",
            Integer.class,
            userid,
            topic
    );

    if (res.isEmpty()) {
      return insertTemplate.executeAndReturnKey(ImmutableMap.<String, Object>of("userid", userid, "topic", topic)).intValue();
    } else {
      return res.get(0);
    }
  }

  /**
   * Get memories id or 0 if not in memories
   *
   * @param user
   * @param topic
   * @return
   */
  public int getId(User user, Topic topic) {
    List<Integer> res = jdbcTemplate.queryForList(
            "SELECT id FROM memories WHERE userid=? AND topic=?",
            Integer.class,
            user.getId(),
            topic.getId()
    );

    if (res.isEmpty()) {
      return 0;
    } else {
      return res.get(0);
    }
  }

  public MemoriesListItem getMemoriesListItem(int id) {
    List<MemoriesListItem> res = jdbcTemplate.query(
            "SELECT * FROM memories WHERE id=?",
            new RowMapper<MemoriesListItem>() {
              @Override
              public MemoriesListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new MemoriesListItem(rs);
              }
            },
            id
    );

    if (res.isEmpty()) {
      return null;
    } else {
      return res.get(0);
    }
  }

  public void delete(int id) {
    jdbcTemplate.update("DELETE FROM memories WHERE id=?", id);
  }
}
