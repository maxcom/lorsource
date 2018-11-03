/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import ru.org.linux.auth.AccessViolationException;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Repository
public class IgnoreListDao {
  private static final Logger logger = LoggerFactory.getLogger(IgnoreListDao.class);

  private static final String queryIgnoreList = "SELECT a.ignored FROM ignore_list a WHERE a.userid=?";
  private static final String queryIgnoreStat = "SELECT count(*) as inum FROM ignore_list JOIN users ON  ignore_list.userid = users.id WHERE ignored=? AND not blocked";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public void addUser(User listOwner, User userToIgnore) throws AccessViolationException {
    if (userToIgnore.isModerator()) {
      throw new AccessViolationException("Нельзя игнорировать модератора");
    }

    try {
      jdbcTemplate.update(
              "INSERT INTO ignore_list (userid,ignored) VALUES(?,?)",
              listOwner.getId(),
              userToIgnore.getId()
      );
    } catch (DuplicateKeyException ex) {
      logger.debug("User was already in ignore list", ex);
    }
  }

  public void remove(User listOwner, User userToIgnore) {
    jdbcTemplate.update(
            "DELETE FROM ignore_list WHERE userid=? AND ignored=?",
            listOwner.getId(),
            userToIgnore.getId()
    );
  }

  /**
   * Получить список игнорируемых
   * @param user пользователь который игнорирует
   * @return список игнорируемых
   */
  @Nonnull
  public Set<Integer> get(@Nonnull User user) {
    final Builder<Integer> builder = ImmutableSet.builder();
    jdbcTemplate.query(queryIgnoreList, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.add(resultSet.getInt("ignored"));
      }
    }, user.getId());
    return builder.build();
  }

  public int getIgnoreStat(User ignoredUser) {
    return jdbcTemplate.queryForObject(queryIgnoreStat, Integer.class, ignoredUser.getId());
  }
}
