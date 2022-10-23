/*
 * Copyright 1998-2022 Linux.org.ru
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
package ru.org.linux.spring.dao

import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.util.Optional
import javax.sql.DataSource
import scala.jdk.OptionConverters.RichOption

/**
 * Информация о UA пользователей
 */
@Repository
class UserAgentDao(dataSource: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(dataSource)

  /**
   * получить UA по его id
   *
   * @param id id UA
   * @return название UA или null если отсутствует
   */
  def getUserAgentById(id: Int): Optional[String] = {
    (if (id == 0) {
      None
    } else {
      jdbcTemplate.queryForObject[String]("SELECT name FROM user_agents WHERE id=?", id)
    }).toJava
  }
}