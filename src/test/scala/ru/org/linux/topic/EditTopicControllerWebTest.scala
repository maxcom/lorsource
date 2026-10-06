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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.test.{SpringTestSupport, WebHelper}
import ru.org.linux.user.SimpleIntegrationTestConfiguration
import scalikejdbc.*
import sttp.client4.*
import sttp.model.StatusCode

/** Правка опроса — это правка контента: редактор с правом правки только тегов (postscore = POSTSCORE_NO_COMMENTS у
  * неподтвержденного топика-опроса) не может менять варианты и мультивыбор через POST /edit.jsp.
  */
object EditTopicControllerWebTest:
  private val PollGroup = 19387 // раздел «Голосования» (section 5, votepoll + premoderated)
  private val PostScoreNoComments = 10001
  private val TestTitle = "EditTopicControllerWebTest опрос"
  private val TestMsg = "текст топика-опроса для EditTopicControllerWebTest"
  private val VariantA = "Вариант А"
  private val VariantB = "Вариант Б"

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class EditTopicControllerWebTest extends FunSuite with WebHelper with SpringTestSupport:
  import EditTopicControllerWebTest.*

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private case class PollTopic(id: Int, variantIds: List[Int])

  private def insertPollTopic()(using Transaction): PollTopic =
    val authorId = sql"SELECT id FROM users WHERE nick = 'Shaman007'".map(rs => rs.int(1)).single.apply().get

    val topicId = sql"SELECT nextval('s_msgid')".map(rs => rs.int(1)).single.apply().get

    sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous, postscore)
          VALUES ($PollGroup, $authorId, $TestTitle, '', 'f', CURRENT_TIMESTAMP, $topicId, '', 'f',
                  create_user_agent('EditTopicControllerWebTest'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f', $PostScoreNoComments)"""
      .update
      .apply()

    sql"INSERT INTO msgbase (id, message, markup) VALUES ($topicId, $TestMsg, 'MARKDOWN')".update.apply()

    val voteId = sql"SELECT nextval('vote_id')".map(rs => rs.int(1)).single.apply().get
    sql"INSERT INTO polls (id, multiselect, topic) VALUES ($voteId, false, $topicId)".update.apply()

    val variantIds = Seq(VariantA, VariantB).map { label =>
      sql"INSERT INTO polls_variants (id, vote, label) VALUES (nextval('votes_id'), $voteId, $label)".update.apply()
      sql"SELECT id FROM polls_variants WHERE vote = $voteId AND label = $label".map(rs => rs.int(1)).single.apply().get
    }

    PollTopic(topicId, variantIds.toList)
  end insertPollTopic

  private def createPollTopic(): PollTopic =
    springDB.localTx {
      insertPollTopic()
    }

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

  private def deletePollTopic(topicId: Int): Unit =
    springDB.localTx {
      deletePollTopicRows(topicId)
    }

  private def variantLabels(topicId: Int): List[String] =
    springDB.run:
      sql"SELECT pv.label FROM polls_variants pv JOIN polls p ON p.id = pv.vote WHERE p.topic = $topicId ORDER BY pv.id"
        .map(rs => rs.string(1))
        .list
        .apply()
  end variantLabels

  private def isMultiselect(topicId: Int): Boolean =
    springDB.run:
      sql"SELECT multiselect FROM polls WHERE topic = $topicId".map(rs => rs.boolean(1)).single.apply().get
  end isMultiselect

  private def postEdit(auth: String, params: (String, String)*): Response[Either[String, String]] =
    basicRequest
      .body(params.toMap)
      .cookie(AuthCookie, auth)
      .cookie(CSRFProtectionService.CSRF_COOKIE, "csrf")
      .followRedirects(false)
      .post(MainUrl.addPath("edit.jsp"))
      .send(backend)
  end postEdit

  test("tags-only editor cannot relabel poll variants"):
    val topic = createPollTopic()

    try
      val auth = doLogin()

      val response = postEdit(
        auth,
        "msgid" -> topic.id.toString,
        "title" -> TestTitle,
        "msg" -> TestMsg,
        s"poll[${topic.variantIds.head}]" -> "переименованный злоумышленником вариант",
        s"poll[${topic.variantIds(1)}]" -> VariantB,
        "csrf" -> "csrf"
      )

      assertEquals(response.code, StatusCode.Forbidden, s"body: ${response.body.merge}")
      assert(
        response.body.merge.contains("нельзя править это сообщение, только теги"),
        s"expected tags-only rejection, got: ${response.body.merge}")

      assertEquals(variantLabels(topic.id), List(VariantA, VariantB), "poll labels must be unchanged")
    finally
      deletePollTopic(topic.id)

  test("tags-only editor cannot flip poll multiselect"):
    val topic = createPollTopic()

    try
      val auth = doLogin()

      val response = postEdit(
        auth,
        "msgid" -> topic.id.toString,
        "title" -> TestTitle,
        "msg" -> TestMsg,
        s"poll[${topic.variantIds.head}]" -> VariantA,
        s"poll[${topic.variantIds(1)}]" -> VariantB,
        "multiselect" -> "true",
        "csrf" -> "csrf"
      )

      assertEquals(response.code, StatusCode.Forbidden, s"body: ${response.body.merge}")
      assert(
        response.body.merge.contains("нельзя править это сообщение, только теги"),
        s"expected tags-only rejection, got: ${response.body.merge}")

      assert(!isMultiselect(topic.id), "multiselect must be unchanged")
    finally
      deletePollTopic(topic.id)

  test("tags-only editor can still edit tags of a poll topic"):
    val topic = createPollTopic()

    try
      val auth = doLogin()

      val existingTag = springDB.run:
        sql"SELECT value FROM tags_values ORDER BY id LIMIT 1".map(rs => rs.string(1)).single.apply().get

      val response = postEdit(
        auth,
        "msgid" -> topic.id.toString,
        "title" -> TestTitle,
        "msg" -> TestMsg,
        "tags" -> existingTag,
        "csrf" -> "csrf")

      assertEquals(response.code, StatusCode.Found, s"body: ${response.body.merge}")

      assertEquals(variantLabels(topic.id), List(VariantA, VariantB), "poll labels must be unchanged")
      assert(!isMultiselect(topic.id), "multiselect must be unchanged")
    finally
      deletePollTopic(topic.id)
