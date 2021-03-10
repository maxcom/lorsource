/*
 * Copyright 1998-2021 Linux.org.ru
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
package ru.org.linux.telegram

import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.topic.Topic

import javax.sql.DataSource
import scala.jdk.CollectionConverters._

@Repository
class TelegramPostsDao(ds:DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val simpleJdbcInsert =
    new SimpleJdbcInsert(ds)
      .withTableName("telegram_posts")
      .usingColumns("topic_id", "telegram_id")

  def storePost(topic: Topic, telegramId: Int): Unit = {
    simpleJdbcInsert.execute(Map("topic_id" -> topic.getId, "telegram_id" -> telegramId).asJava)
  }

  def hotTopic: Option[Topic] = {
    jdbcTemplate.queryAndMap(
      """
        |select topics.postdate, topics.id as msgid, topics.userid, topics.title,
        |  topics.groupid as guid, topics.url, topics.linktext, topics.ua_id,
        |  urlname, section, topics.sticky, topics.postip,
        |  COALESCE(commitdate, topics.postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, topics.deleted, lastmod, commitby,
        |  commitdate, topics.stat1, postscore, topics.moderate, notop,
        |  topics.resolved, minor, draft, allow_anonymous
        |from topics join groups ON (groups.id=topics.groupid) join sections on (sections.id=groups.section)
        |where topics.id in (
        |  select topic from comments join users on comments.userid=users.id join topics on (comments.topic=topics.id)
        |    where comments.postdate>CURRENT_TIMESTAMP-'6 hour'::interval and score>=100
        |      and topics.id not in (select topic_id from telegram_posts) and not topics.deleted AND not comments.deleted
        |      and not notop and not draft
        |    group by topic
        |    having count (distinct comments.userid)>=15
        |    order by count(distinct comments.userid) desc
        |    limit 1)
        |""".stripMargin) { (resultSet, _) => new Topic(resultSet) }.headOption
  }
}
