/*
 * Copyright 1998-2026 Linux.org.ru
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

import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.site.DefaultProfile
import ru.org.linux.util.ProfileHashtable

import java.util.HashMap
import javax.sql.DataSource

@Repository
class ProfileDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def readProfile(userId: Int): Profile = {
    val profiles = jdbcTemplate.queryAndMap(
      "SELECT settings FROM user_settings WHERE id=?",
      userId
    ) { (rs, _) =>
      Profile(new ProfileHashtable(DefaultProfile.defaultProfile, rs.getObject("settings").asInstanceOf[java.util.Map[String, String]]))
    }

    if (profiles.isEmpty) {
      Profile(new ProfileHashtable(DefaultProfile.defaultProfile, new HashMap[String, String]()))
    } else {
      profiles.head
    }
  }

  def deleteProfile(user: User): Unit =
    jdbcTemplate.update("DELETE FROM user_settings WHERE id=?", user.id)

  def writeProfile(user: User, profile: ProfileBuilder): Unit = {
    val updateSql = "UPDATE user_settings SET settings=? WHERE id=?"
    val insertSql = "INSERT INTO user_settings (id, settings) VALUES (?,?)"

    val updateCreator: PreparedStatementCreator = con => {
      val st = con.prepareStatement(updateSql)
      st.setObject(1, profile.getSettings)
      st.setInt(2, user.id)
      st
    }

    val insertCreator: PreparedStatementCreator = con => {
      val st = con.prepareStatement(insertSql)
      st.setInt(1, user.id)
      st.setObject(2, profile.getSettings)
      st
    }

    if (jdbcTemplate.javaTemplate.update(updateCreator) == 0) {
      jdbcTemplate.javaTemplate.update(insertCreator)
    }
  }
}
