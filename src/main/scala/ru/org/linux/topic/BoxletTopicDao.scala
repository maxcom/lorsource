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
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.warning.WarningService.TopicMaxWarnings
import scalikejdbc.*

import java.sql.Timestamp
import scala.beans.BeanProperty

case class BoxletTopic(
    @BeanProperty
    url: String,
    @BeanProperty
    title: String,
    @BeanProperty
    lastmod: Timestamp,
    @BeanProperty
    commentCount: Int,
    @BeanProperty
    pages: Int)

@Repository
class BoxletTopicDao(sectionService: SectionService):

  def top10(commentsPerPage: Int): Seq[BoxletTopic] =
    SpringDB.run:
      sql"""SELECT topics.id AS msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 AS c
           FROM topics JOIN groups ON groups.id = topics.groupid
           WHERE topics.postdate > (CURRENT_TIMESTAMP - '1 month 1 day'::interval)
           AND NOT deleted AND NOT notop AND topics.open_warnings <= $TopicMaxWarnings
           AND topics.postscore IS DISTINCT FROM ${TopicPermissionService.POSTSCORE_HIDE_COMMENTS}
           ORDER BY c DESC, msgid LIMIT 10""".map(rsToTopic(commentsPerPage)).list.apply()

  def articles(commentsPerPage: Int): Seq[BoxletTopic] =
    SpringDB.run:
      sql"""SELECT topics.id AS msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 AS c
           FROM topics JOIN groups ON groups.id = topics.groupid
           WHERE NOT deleted AND NOT notop AND moderate AND commitdate IS NOT NULL
           AND topics.postscore IS DISTINCT FROM ${TopicPermissionService.POSTSCORE_HIDE_COMMENTS}
           AND section = ${Section.Articles} ORDER BY commitdate DESC, msgid LIMIT 10"""
        .map(rsToTopic(commentsPerPage))
        .list
        .apply()

  private def rsToTopic(commentsPerPage: Int)(rs: WrappedResultSet): BoxletTopic =
    val commentCount = rs.int("c")
    val section = sectionService.getSection(rs.int("section"))

    BoxletTopic(
      section.getSectionLink + rs.string("urlname") + '/' + rs.int("msgid"),
      rs.string("title"),
      rs.timestamp("lastmod"),
      commentCount,
      Topic.pageCount(commentCount, commentsPerPage)
    )

end BoxletTopicDao
