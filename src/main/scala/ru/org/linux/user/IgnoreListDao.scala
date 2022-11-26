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
package ru.org.linux.user

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.auth.AccessViolationException

import java.util
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class IgnoreListDao(ds: DataSource) extends StrictLogging {
  private val jdbcTemplate = new JdbcTemplate(ds)

  @throws[AccessViolationException]
  def addUser(listOwner: User, userToIgnore: User): Unit = {
    if (userToIgnore.isModerator) {
      throw new AccessViolationException ("Нельзя игнорировать модератора")
    }

    jdbcTemplate.update("INSERT INTO ignore_list (userid,ignored) VALUES(?,?) ON CONFLICT DO NOTHING",
      listOwner.getId, userToIgnore.getId)
  }

  def remove(listOwner: User, userToIgnore: User): Unit =
    jdbcTemplate.update ("DELETE FROM ignore_list WHERE userid=? AND ignored=?", listOwner.getId, userToIgnore.getId)

  /**
   * Получить список игнорируемых
   *
   * @param user пользователь который игнорирует
   * @return список игнорируемых
   */
  def get(user: Int): Set[Int] = {
    jdbcTemplate.queryAndMap("SELECT a.ignored FROM ignore_list a WHERE a.userid=?", user) { (resultSet, _) =>
      resultSet.getInt("ignored")
    }.toSet
  }

  def getJava(user: User): util.Set[Integer] = get(user.getId).map(Integer.valueOf).asJava

  def getIgnoreCount(ignoredUser: User): Int =
    jdbcTemplate.queryForObject[Integer](
      "SELECT count(*) as inum FROM ignore_list JOIN users ON ignore_list.userid = users.id" +
        " WHERE ignored=? AND not blocked", ignoredUser.getId).get
}