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

package ru.org.linux.comment

import munit.FunSuite
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo}
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.{Profile, User, UserEventService, UserService}
import scalikejdbc.*

/** Интеграционные тесты поддержки денормализованного topics.score_loss сервисом удаления/восстановления.
  *
  * Сравнение идёт с эталонным SQL (бывший запрос DeleteInfoDao.scoreLoss).
  */
@ContextConfiguration(classes = Array(classOf[DeleteServiceScoreLossIntegrationTestConfiguration]))
class DeleteServiceScoreLossIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  @Autowired
  var deleteInfoDao: DeleteInfoDao = scala.compiletime.uninitialized

  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private val userService = mock(classOf[UserService])
  private val userEventService = mock(classOf[UserEventService])
  private val commentService = mock(classOf[CommentReadService])

  when(userService.getUserCached(anyInt())).thenReturn(makeUser(1))

  private lazy val deleteService =
    DeleteService(commentDao, userService, userEventService, deleteInfoDao, commentService, topicDao, springDB)

  private var topicId: Int = scala.compiletime.uninitialized
  private var authorId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    topicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    authorId = springDB.run:
      sql"select min(id) from users where id not in (${UserService.AnonymousUserId}, ${UserService.DeletedUserId})"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get

  private def nextMsgId: Int =
    springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get

  private def makeUser(id: Int): User =
    User(
      nick = "score-loss-test",
      id = id,
      canmod = true,
      candel = true,
      anonymous = false,
      corrector = false,
      blocked = false,
      password = "password",
      score = 100,
      maxScore = 100,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

  private def moderatorSession: AuthorizedSession =
    AuthorizedSession(
      makeUser(authorId),
      corrector = false,
      moderator = true,
      administrator = false,
      profile = Profile.DEFAULT,
      ipBlockInfo = IpBlockInfo("127.0.0.1"))

  private def insertComment(commentId: Int, userId: Int): Unit =
    springDB.run:
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $userId, 'score loss test', CURRENT_TIMESTAMP,
                    ${null: Integer}, 'f', $topicId, '127.0.0.1'::inet, create_user_agent('Integration test User Agent'))"""
        .update
        .apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'score loss test body')".update.apply()

  private def scoreLossColumn: Int =
    springDB.run:
      sql"SELECT score_loss FROM topics WHERE id = $topicId".map(rs => rs.int("score_loss")).single.apply().get

  /** Эталонный SQL — бывший запрос DeleteInfoDao.scoreLoss. */
  private def scoreLossReference: Int =
    springDB.run:
      sql"""SELECT COALESCE(
              (SELECT sum(-bonus) AS total_bonus FROM del_info
               JOIN comments ON comments.id = del_info.msgid
               WHERE bonus IS NOT NULL AND bonus != 0
               AND comments.userid != 2 AND comments.deleted AND topic = $topicId),
              0)""".map(rs => rs.int(1)).single.apply().getOrElse(0)

  test("deleteComment with score bonus increments score_loss"):
    val commentId = nextMsgId
    insertComment(commentId, authorId)
    val comment = commentDao.getById(commentId)
    val before = scoreLossColumn

    val deleted =
      deleteService.deleteComment(comment, "score loss test", 5, checkForReply = false)(using moderatorSession)

    assert(deleted)
    assertEquals(scoreLossColumn, before + 5, "score_loss should increase by removed score")
    assertEquals(scoreLossColumn, scoreLossReference, "column should match the reference query")

  test("deleteComment without score bonus does not change score_loss"):
    val commentId = nextMsgId
    insertComment(commentId, authorId)
    val comment = commentDao.getById(commentId)
    val before = scoreLossColumn

    val deleted =
      deleteService.deleteComment(comment, "no bonus test", 0, checkForReply = false)(using moderatorSession)

    assert(deleted)
    assertEquals(scoreLossColumn, before, "score_loss should not change on zero bonus")
    assertEquals(scoreLossColumn, scoreLossReference, "column should match the reference query")

  test("anonymous comment score penalties are not counted"):
    val commentId = nextMsgId
    insertComment(commentId, UserService.AnonymousUserId)
    val comment = commentDao.getById(commentId)
    val before = scoreLossColumn

    val deleted =
      deleteService.deleteComment(comment, "anonymous test", 5, checkForReply = false)(using moderatorSession)

    assert(deleted)
    assertEquals(scoreLossColumn, before, "anonymous author penalties should not count")
    assertEquals(scoreLossColumn, scoreLossReference, "column should match the reference query")

  test("undeleteComment restores score_loss"):
    val commentId = nextMsgId
    insertComment(commentId, authorId)
    val comment = commentDao.getById(commentId)
    val before = scoreLossColumn

    deleteService.deleteComment(comment, "score loss test", 5, checkForReply = false)(using moderatorSession)
    assertEquals(scoreLossColumn, before + 5)

    deleteService.undeleteComment(commentDao.getById(commentId))(using moderatorSession)

    assertEquals(scoreLossColumn, before, "score_loss should be restored on undelete")
    assertEquals(scoreLossColumn, scoreLossReference, "column should match the reference query")

end DeleteServiceScoreLossIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeleteServiceScoreLossIntegrationTestConfiguration:

  @Bean
  def commentDao(springDB: SpringDB): CommentDao = CommentDao(springDB)

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = DeleteInfoDao(springDB)

  @Bean
  def topicDao(springDB: SpringDB): TopicDao = TopicDao(springDB)

end DeleteServiceScoreLossIntegrationTestConfiguration
