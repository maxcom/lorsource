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
package ru.org.linux.topic

import org.springframework.stereotype.Repository
import ru.org.linux.group.Group
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.Section
import scalikejdbc.*

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*
import javax.annotation.Nullable

case class ArchiveStats(
    @BeanProperty
    section: Section,
    @Nullable @BeanProperty
    group: Group,
    @BeanProperty
    year: Int,
    @BeanProperty
    month: Int,
    @BeanProperty
    count: Int):

  @BeanProperty
  def getLink: String =
    if group != null then
      group.getArchiveLink(year, month)
    else
      section.getArchiveLink(year, month)

@Repository
class ArchiveDao(springDB: SpringDB):

  def getArchiveStats(section: Section, group: Option[Group]): java.util.List[ArchiveStats] =
    val result = springDB.run:
      val groupIdClause = group.map(g => sqls"groupid = ${g.id}").getOrElse(sqls"groupid IS NULL")

      sql"SELECT year, month, c FROM monthly_stats WHERE section = ${section.id} AND $groupIdClause ORDER BY year, month"
        .map(rs => ArchiveStats(section, group.orNull, rs.int("year"), rs.int("month"), rs.int("c")))
        .list
        .apply()

    result.asJava

  def getArchiveCount(groupid: Int, year: Int, month: Int): Int =
    springDB.run:
      sql"SELECT c FROM monthly_stats WHERE groupid = $groupid AND year = $year AND month = $month"
        .map(rs => rs.int("c"))
        .single
        .apply()
        .getOrElse(0)

end ArchiveDao
