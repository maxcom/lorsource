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
package ru.org.linux.section

import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import javax.sql.DataSource

trait SectionDao {
  def getAllSections: Seq[Section]
}

@Repository
class SectionDaoImpl(val ds: DataSource) extends SectionDao {
  private val jdbcTemplate = new JdbcTemplate(ds)

  override def getAllSections: Seq[Section] = {
    jdbcTemplate.queryAndMap("SELECT id, name, imagepost, imageallowed, vote, moderate, scroll_mode, restrict_topics FROM sections ORDER BY id") {
      (rs, _) => new Section(rs)
    }.toVector
  }
}