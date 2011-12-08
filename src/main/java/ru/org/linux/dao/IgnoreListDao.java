/*
 * Copyright 1998-2011 Linux.org.ru
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

package ru.org.linux.dao;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.AccessViolationException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Реализация класса IgnoreListDao.
 */
@Repository
public class IgnoreListDao {
  private static final Log log = LogFactory.getLog(IgnoreListDao.class);

  private static final String queryIgnoreList = "SELECT a.ignored FROM ignore_list a WHERE a.userid=?";
  private static final String queryIgnoreStat = "SELECT count(*) as inum FROM ignore_list JOIN users ON  ignore_list.userid = users.id WHERE ignored=? AND not blocked";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Добавить пользователя в Игнор-лист.
   *
   * @param listOwner    в чей список добавить
   * @param userToIgnore кого добавить
   * @throws AccessViolationException
   */
  public void addUser(UserDto listOwner, UserDto userToIgnore) throws AccessViolationException {
    if (userToIgnore.isModerator()) {
      throw new AccessViolationException("Нельзя игнорировать модератора");
    }

    jdbcTemplate.update(
        "INSERT INTO ignore_list (userid,ignored) VALUES(?,?)",
        listOwner.getId(),
        userToIgnore.getId()
    );
  }

  /**
   * Удалить пользователя из Игнор-листа.
   *
   * @param listOwner    из чьего списка удалить
   * @param userToIgnore кого удалить
   */
  public void remove(UserDto listOwner, UserDto userToIgnore) {
    jdbcTemplate.update(
        "DELETE FROM ignore_list WHERE userid=? AND ignored=?",
        listOwner.getId(),
        userToIgnore.getId()
    );
  }

  /**
   * Получить список игнорируемых
   *
   * @param user пользователь который игнорирует
   * @return список игнорируемых
   */
  public Set<Integer> get(UserDto user) {
    final ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    jdbcTemplate.query(queryIgnoreList, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.add(resultSet.getInt("ignored"));
      }
    }, user.getId());
    return builder.build();
  }

  /**
   * Получить количество игнорирующих.
   *
   * @param ignoredUser пользователь, которого игнорируют.
   * @return
   */
  public int getIgnoreStat(UserDto ignoredUser) {
    return jdbcTemplate.queryForInt(queryIgnoreStat, ignoredUser.getId());
  }
}
