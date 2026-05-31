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
package ru.org.linux.section

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

trait SectionDao:
  def getAllSections: Seq[Section]

@Repository
class SectionDaoImpl(springDB: SpringDB) extends SectionDao:

  override def getAllSections: Seq[Section] =
    springDB.run:
      sql"SELECT id, name, imagepost, imageallowed, vote, moderate, scroll_mode, restrict_topics FROM sections ORDER BY id"
        .map(Section.fromWrappedResultSet)
        .list
        .apply()

end SectionDaoImpl
