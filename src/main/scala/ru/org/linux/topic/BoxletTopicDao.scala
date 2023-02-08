/*
 * Copyright 1998-2023 Linux.org.ru
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

import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.section.{Section, SectionService}

import java.sql.{ResultSet, Timestamp}
import javax.sql.DataSource
import scala.beans.BeanProperty

case class BoxletTopic(@BeanProperty url: String, @BeanProperty title: String, @BeanProperty lastmod: Timestamp,
                       @BeanProperty commentCount: Int, @BeanProperty pages: Int)

@Repository
class BoxletTopicDao(sectionService: SectionService, dataSource: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(dataSource)

  def top10(commentsPerPage: Int): Seq[BoxletTopic] = {
    val sql =
      s"""
         |select topics.id as msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 as c
         |from topics join groups on groups.id = topics.groupid
         |where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and
         |not deleted and not notop and
         |topics.postscore is distinct from ${TopicPermissionService.POSTSCORE_HIDE_COMMENTS}
         |order by c desc, msgid limit 10""".stripMargin

    jdbcTemplate.queryAndMap(sql)(rsToTopic(commentsPerPage))
  }

  def articles(commentsPerPage: Int): Seq[BoxletTopic] = {
    val sql =
      s"""
         |select topics.id as msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 as c
         |from topics join groups on groups.id = topics.groupid
         |where not deleted and not notop and moderate and commitdate is not null and
         |topics.postscore is distinct from ${TopicPermissionService.POSTSCORE_HIDE_COMMENTS} and
         |section=${Section.SECTION_ARTICLES} order by commitdate desc, msgid limit 10""".stripMargin

    jdbcTemplate.queryAndMap(sql)(rsToTopic(commentsPerPage))
  }

  private def rsToTopic(commentsPerPage: Int)(rs: ResultSet, i: Int): BoxletTopic = {
    val commentCount = rs.getInt("c")
    val section = sectionService.getSection(rs.getInt("section"))

    BoxletTopic(
      section.getSectionLink + rs.getString("urlname") + '/' + rs.getInt("msgid"),
      rs.getString("title"),
      rs.getTimestamp("lastmod"),
      commentCount,
      Topic.pageCount(commentCount, commentsPerPage))
  }
}