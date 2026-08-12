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
package ru.org.linux.user

import org.junit.Assert.{assertEquals, assertNotNull}
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

import java.time.{OffsetDateTime, ZoneOffset}

object UserLogDaoIntegrationTest:
  private val TestId = 1

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[UserLogDaoIntegrationTestConfiguration]))
  )) @Transactional
class UserLogDaoIntegrationTest:
  @Autowired
  var userLogDao: UserLogDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  @Test
  def testLogAcceptEmail(): Unit =
    val user = mock(classOf[User])
    when(user.id).thenReturn(UserLogDaoIntegrationTest.TestId)
    when(user.email).thenReturn("old@email")

    val oldLogItems = userLogDao.getLogItems(user, includeSelf = true)

    springDB.localTx {
      userLogDao.logAcceptNewEmail(user, "test@email")
    }

    val logItems = userLogDao.getLogItems(user, includeSelf = true)

    assertEquals(1, logItems.size - oldLogItems.size)

    val item = logItems.head

    assertNotNull(item)
    assertEquals(UserLogAction.AcceptNewEmail, item.action)

  @Test
  def testLogScore50(): Unit =
    val user = mock(classOf[User])
    when(user.id).thenReturn(UserLogDaoIntegrationTest.TestId)

    val oldLogItems = userLogDao.getLogItems(user, includeSelf = true)

    springDB.localTx {
      userLogDao.logScore50(user, user)
    }

    val logItems = userLogDao.getLogItems(user, includeSelf = true)

    assertEquals(1, logItems.size - oldLogItems.size)

    val item = logItems.head

    assertNotNull(item)
    assertEquals(UserLogAction.Score50, item.action)

  @Test
  def getLatestUserpicMentionsEmpty(): Unit = assertEquals(Map.empty, userLogDao.getLatestUserpicMentions(Seq.empty))

  @Test
  def getLatestUserpicMentionsNoRows(): Unit =
    val res = userLogDao.getLatestUserpicMentions(Seq("does-not-exist-12345.jpg"))
    assertEquals(None, res.get("does-not-exist-12345.jpg"))

  @Test
  def getLatestUserpicMentionsRecent(): Unit =
    val user = mock(classOf[User])
    when(user.id).thenReturn(UserLogDaoIntegrationTest.TestId)
    when(user.photo).thenReturn("old.jpg")

    springDB.localTx {
      userLogDao.logSetUserpic(user, "new.jpg")
    }

    val res = userLogDao.getLatestUserpicMentions(Seq("old.jpg", "new.jpg"))

    assert(res.contains("old.jpg"))
    assert(res.contains("new.jpg"))
    assert(res("old.jpg").isDefined)
    assert(res("new.jpg").isDefined)

    // дата должна быть близка к текущему времени (нами только что записана)
    val newDate = res("new.jpg").getOrElse(throw new AssertionError("expected date"))
    assert(newDate.isAfter(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)))

  @Test
  def getLatestUserpicMentionsPicksLatest(): Unit =
    // вставляем две записи напрямую с разными датами, проверяем что берётся MAX(action_date)
    springDB.run {
      sql"""INSERT INTO user_log (userid, action_userid, action_date, action, info)
         VALUES (${UserLogDaoIntegrationTest.TestId}, ${UserLogDaoIntegrationTest.TestId},
                 ${OffsetDateTime.now(ZoneOffset.UTC).minusYears(5)},
                 'set_userpic'::user_log_action,
'new_userpic=>"old-5y.jpg"'::hstore)""".update.apply()
      sql"""INSERT INTO user_log (userid, action_userid, action_date, action, info)
          VALUES (${UserLogDaoIntegrationTest.TestId}, ${UserLogDaoIntegrationTest.TestId},
                  ${OffsetDateTime.now(ZoneOffset.UTC).minusYears(2)},
                  'reset_userpic'::user_log_action,
                  'old_userpic=>"old-5y.jpg",bonus=>"0"'::hstore)""".update.apply()
    }

    val res = userLogDao.getLatestUserpicMentions(Seq("old-5y.jpg"))
    val latest = res("old-5y.jpg").getOrElse(throw new AssertionError("expected date"))
    // должна вернуться дата ~2 года назад, а не ~5
    assert(latest.isAfter(OffsetDateTime.now(ZoneOffset.UTC).minusYears(3)))
    assert(latest.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusYears(1)))
