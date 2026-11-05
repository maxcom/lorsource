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
import ru.org.linux.auth.{AuthorizedSession, FloodProtector, IpBlockInfo}
import ru.org.linux.edithistory.EditHistoryService
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.msgbase.{DeleteInfoDao, MessageText, MsgbaseDao}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.user.{IgnoreListDao, Profile, User, UserEventService, UserErrorException, UserService}
import scalikejdbc.*

/** Интеграционные тесты защиты самоудаления от гонки с параллельным добавлением ответов (sast/final-report.md, находка
  * 42).
  *
  * Самоудаление автором допустимо только пока нет ответов, и проверка выполняется атомарно:
  *   - для темы — предикат stat1=0 входит в UPDATE (триггер comins обновляет stat1 в транзакции вставки комментария и
  *     блокирует строку topics);
  *   - для комментария — строка блокируется FOR UPDATE (конфликтует с FOR KEY SHARE от FK вставки ответа), затем ответы
  *     подсчитываются в той же транзакции.
  *
  * Обратное направление гонки закрыто перепроверками в CommentCreateService.create: комментарий откатывается, если тема
  * или родительский комментарий удалены параллельно с его добавлением.
  */
@ContextConfiguration(classes = Array(classOf[DeleteServiceGuardIntegrationTestConfiguration]))
class DeleteServiceGuardIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  @Autowired
  var deleteInfoDao: DeleteInfoDao = scala.compiletime.uninitialized

  @Autowired
  var msgbaseDao: MsgbaseDao = scala.compiletime.uninitialized

  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private val userService = mock(classOf[UserService])
  private val userEventService = mock(classOf[UserEventService])
  private val commentService = mock(classOf[CommentReadService])
  private val commentPrepareService = mock(classOf[CommentPrepareService])
  private val floodProtector = mock(classOf[FloodProtector])
  private val editHistoryService = mock(classOf[EditHistoryService])
  private val textService = mock(classOf[MessageTextService])
  private val ignoreListDao = mock(classOf[IgnoreListDao])
  private val topicPermissionService = mock(classOf[TopicPermissionService])

  when(userService.getUserCached(anyInt())).thenReturn(makeUser(1))

  private lazy val deleteService =
    DeleteService(commentDao, userService, userEventService, deleteInfoDao, commentService, topicDao, springDB)

  private lazy val createService =
    CommentCreateService(
      commentDao,
      topicDao,
      userService,
      commentPrepareService,
      floodProtector,
      editHistoryService,
      textService,
      userEventService,
      msgbaseDao,
      ignoreListDao,
      topicPermissionService,
      springDB
    )

  private var topicId: Int = scala.compiletime.uninitialized
  private var emptyTopicId: Int = scala.compiletime.uninitialized
  private var authorId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    topicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    emptyTopicId = springDB.run:
      sql"select min(id) from topics where not deleted and stat1=0".map(rs => rs.int(1)).single.apply().get
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
      nick = "guard-test",
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

  private def userSession: AuthorizedSession =
    AuthorizedSession(
      makeUser(authorId),
      corrector = false,
      moderator = false,
      administrator = false,
      profile = Profile.DEFAULT,
      ipBlockInfo = IpBlockInfo("127.0.0.1"))

  private def insertComment(commentId: Int, replyToId: Option[Int], topic: Int): Unit =
    springDB.run:
      val replyTo = replyToId.getOrElse(null: Integer)
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $authorId, 'guard test', CURRENT_TIMESTAMP,
                    $replyTo, 'f', $topic, '127.0.0.1'::inet, create_user_agent('Integration test User Agent'))"""
        .update
        .apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'guard test body')".update.apply()

  test("selfDelete of topic refuses comment added after the topic was loaded (check/delete race)"):
    // имитация TOCTOU-окна: объект Topic загружен до появления комментария (commentCount=0),
    // в БД комментарий (и stat1=1) уже есть
    val staleTopic = topicDao.getById(emptyTopicId)
    assert(!staleTopic.deleted)
    insertComment(nextMsgId, None, emptyTopicId)

    val e = intercept[UserErrorException]:
      deleteService.deleteTopic(staleTopic, "guard test", 0, selfDelete = true)(using userSession)

    assertEquals(e.getMessage, "Нельзя удалить сообщение с ответами")

    assert(!topicDao.findById(emptyTopicId).get.deleted, "тема не должна удалиться при появившемся ответе")

  test("selfDelete of topic reports already-deleted when topic deleted concurrently"):
    // имитация параллельного удаления: объект Topic загружен до удаления,
    // в БД тема уже помечена deleted (NOT deleted делает UPDATE пустым)
    val staleTopic = topicDao.getById(emptyTopicId)
    assert(!staleTopic.deleted)

    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $emptyTopicId".update.apply()

    val e = intercept[UserErrorException]:
      deleteService.deleteTopic(staleTopic, "guard test", 0, selfDelete = true)(using userSession)

    assertEquals(e.getMessage, "Сообщение уже удалено")

  test("deleteTopic without selfDelete guard deletes topic with comments (moderator path)"):
    insertComment(nextMsgId, None, emptyTopicId)

    deleteService.deleteTopic(topicDao.getById(emptyTopicId), "guard test", 0, selfDelete = false)(using
      moderatorSession)

    assert(topicDao.findById(emptyTopicId).get.deleted)

  test("deleteComment with checkForReply refuses comment with reply"):
    val parentId = nextMsgId
    insertComment(parentId, None, topicId)
    insertComment(nextMsgId, Some(parentId), topicId)

    intercept[ScriptErrorException]:
      deleteService.deleteComment(commentDao.getById(parentId), "guard test", 0, checkForReply = true)(using
        userSession)

    assert(!commentDao.getById(parentId).deleted, "комментарий с ответами не должен удалиться")

  test("deleteComment with checkForReply deletes comment without replies"):
    val commentId = nextMsgId
    insertComment(commentId, None, topicId)

    val deleted =
      deleteService.deleteComment(commentDao.getById(commentId), "guard test", 0, checkForReply = true)(using
        userSession)

    assert(deleted)
    assert(commentDao.getById(commentId).deleted)

  // Проверяется именно отказ: вставка в CommentCreateService.create происходит в общей
  // тестовой транзакции, поэтому отсутствие строки после исключения изнутри той же транзакции
  // проконтролировать нельзя — откат гарантируется пробросом исключения через границу localTx.
  test("create refuses reply to concurrently deleted comment"):
    val parentId = nextMsgId
    insertComment(parentId, None, topicId)
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $parentId".update.apply()

    val comment = Comment.buildNew(
      replyto = Some(parentId),
      topic = topicId,
      msgid = nextMsgId,
      userid = authorId,
      postIP = "127.0.0.1")

    intercept[UserErrorException]:
      createService.create(
        makeUser(authorId),
        comment,
        MessageText("guard test", MarkupType.Lorcode),
        "127.0.0.1",
        None,
        Some("guard test UA"))

  test("create refuses comment in concurrently deleted topic"):
    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $topicId".update.apply()

    val comment = Comment.buildNew(
      replyto = None,
      topic = topicId,
      msgid = nextMsgId,
      userid = authorId,
      postIP = "127.0.0.1")

    intercept[UserErrorException]:
      createService.create(
        makeUser(authorId),
        comment,
        MessageText("guard test", MarkupType.Lorcode),
        "127.0.0.1",
        None,
        Some("guard test UA"))

end DeleteServiceGuardIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeleteServiceGuardIntegrationTestConfiguration:

  @Bean
  def commentDao(springDB: SpringDB): CommentDao = CommentDao(springDB)

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = DeleteInfoDao(springDB)

  @Bean
  def msgbaseDao(springDB: SpringDB): MsgbaseDao = MsgbaseDao(springDB)

  @Bean
  def topicDao(springDB: SpringDB): TopicDao = TopicDao(springDB)

end DeleteServiceGuardIntegrationTestConfiguration
