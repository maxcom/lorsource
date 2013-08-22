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

package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Информация о UA пользователей
 */
@Repository
public class UserAgentDao {

  private static final String queryUserAgentById = "SELECT name FROM user_agents WHERE id=?";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * получить UA по его id
   * @param id id UA
   * @return название UA или null если отсутствует
   */
  public String getUserAgentById(int id) {
    if (id == 0) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(queryUserAgentById, String.class, id);
    } catch (EmptyResultDataAccessException exception) {
      return null;
    }
  }
}
