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
package ru.org.linux.poll

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.test.{SpringTestSupport, WebHelper}
import ru.org.linux.user.SimpleIntegrationTestConfiguration
import scalikejdbc.*
import sttp.client4.*
import sttp.model.StatusCode

/** Страница неподтвержденного опроса обязана объяснять, почему голосование недоступно (poll-form.tag получает непустой
  * disabledReason от topic.tag/news.tag).
  */
object PollTopicPageWebTest:
  private val PollGroup = 19387 // раздел «Голосования» (section 5, votepoll + premoderated)
  private val TestTitle = "PollTopicPageWebTest неподтвержденный опрос"
  private val TestMsg = "текст неподтвержденного опроса для PollTopicPageWebTest"
  private val VariantA = "Вариант А"
  private val VariantB = "Вариант Б"

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class PollTopicPageWebTest extends FunSuite with WebHelper with SpringTestSupport:
  import PollTopicPageWebTest.*

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private def insertUncommitedPollTopic()(using Transaction): Int =
    val authorId = sql"SELECT id FROM users WHERE nick = 'Shaman007'".map(rs => rs.int(1)).single.apply().get

    val topicId = sql"SELECT nextval('s_msgid')".map(rs => rs.int(1)).single.apply().get

    sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
          VALUES ($PollGroup, $authorId, $TestTitle, '', 'f', CURRENT_TIMESTAMP, $topicId, '', 'f',
                  create_user_agent('PollTopicPageWebTest'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
      .update
      .apply()

    sql"INSERT INTO msgbase (id, message, markup) VALUES ($topicId, $TestMsg, 'MARKDOWN')".update.apply()

    val voteId = sql"SELECT nextval('vote_id')".map(rs => rs.int(1)).single.apply().get
    sql"INSERT INTO polls (id, multiselect, topic) VALUES ($voteId, false, $topicId)".update.apply()

    for label <- Seq(VariantA, VariantB) do
      sql"INSERT INTO polls_variants (id, vote, label) VALUES (nextval('votes_id'), $voteId, $label)".update.apply()

    topicId
  end insertUncommitedPollTopic

  private def deletePollTopicRows(topicId: Int)(using Transaction): Unit =
    sql"DELETE FROM polls_variants WHERE vote IN (SELECT id FROM polls WHERE topic = $topicId)".update.apply()
    sql"DELETE FROM polls WHERE topic = $topicId".update.apply()
    sql"DELETE FROM user_events WHERE message_id = $topicId".update.apply()
    sql"DELETE FROM tags WHERE msgid = $topicId".update.apply()
    sql"DELETE FROM edit_info WHERE msgid = $topicId".update.apply()
    sql"DELETE FROM msgbase WHERE id = $topicId".update.apply()
    sql"DELETE FROM memories WHERE topic = $topicId".update.apply()
    sql"DELETE FROM topics WHERE id = $topicId".update.apply()
  end deletePollTopicRows

  test("uncommited poll page shows voting disabled reason"):
    val topicId = springDB.localTx(insertUncommitedPollTopic())

    try
      val auth = doLogin()

      val response = basicRequest
        .cookie(AuthCookie, auth)
        .get(MainUrl.addPath("polls", "polls", topicId.toString))
        .send(backend)

      assertEquals(response.code, StatusCode.Ok, s"body: ${response.body.merge}")

      val body = response.body.merge
      assert(body.contains("Голосование недоступно"), "ожидается информационный блок о недоступности голосования")
      assert(body.contains("опрос еще не подтвержден модератором"), s"ожидается причина из VoteChecker, got: $body")
    finally
      springDB.localTx(deletePollTopicRows(topicId))

end PollTopicPageWebTest
