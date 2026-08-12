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

import org.junit.Assert.*
import org.junit.{After, Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import scalikejdbc.*

import java.sql.Timestamp

object UserDaoIntegrationTest:
  private val TestId = 7806

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[UserDaoIntegrationTestConfiguration])) @Transactional
class UserDaoIntegrationTest:
  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  @Before @After
  def fixUser(): Unit =
    springDB.run:
      sql"UPDATE users SET blocked='f' WHERE id=${UserDaoIntegrationTest.TestId}".update.apply()
      sql"DELETE FROM ban_info WHERE userid=${UserDaoIntegrationTest.TestId}".update.apply()

  @Test
  def testUser(): Unit =
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    assertNotNull(user)
    assertFalse(user.blocked)

  @Test
  def testBlock(): Unit =
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    springDB.localTx {
      userDao.block(user, user, "")
    }
    val userAfter = userDao.getUser(UserDaoIntegrationTest.TestId)
    assertTrue(userAfter.blocked)

  @Test
  def testReset(): Unit =
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    val tm = userDao.getResetDate(user)

    springDB.localTx {
      userDao.updateResetDate(user, tm.plusSeconds(60))
    }

    val after = userDao.getResetDate(user)

    assertEquals(tm.plusSeconds(60), after)

  private def ts(value: String): Timestamp = Timestamp.valueOf(value + " 00:00:00")

  private def createBlockedUser(
      nick: String,
      bandate: Option[Timestamp],
      lastlogin: Option[Timestamp],
      regdate: Option[Timestamp]): Int =
    springDB.run {
      val id =
        sql"""INSERT INTO users (id, name, nick, passwd, score, max_score, regdate, blocked, lastlogin)
                     VALUES (nextval('s_uid'), '', $nick, 'x', 45, 45, ${regdate.orNull}, 't', ${lastlogin.orNull})
                     RETURNING id""".map(_.int("id")).single.apply().get

      bandate.foreach { d =>
        sql"""INSERT INTO ban_info (userid, bandate, reason, ban_by)
              VALUES ($id, $d, 'test', ${UserDaoIntegrationTest.TestId})""".update.apply()
      }

      id
    }

  @Test
  def testGetDeletableBlockedUsers(): Unit =
    val oldBan = createBlockedUser("test-old-ban", Some(ts("2015-01-01")), None, None)
    val recentBan = createBlockedUser("test-recent-ban", Some(ts("2026-01-01")), None, None)
    val oldLogin = createBlockedUser("test-old-login", None, Some(ts("2015-01-01")), None)
    val oldReg = createBlockedUser("test-old-reg", None, None, Some(ts("2015-01-01")))
    val noDates = createBlockedUser("test-no-dates", None, None, None)
    val recentLogin = createBlockedUser("test-recent-login", None, Some(ts("2026-01-01")), Some(ts("2015-01-01")))

    val ids = userDao.getDeletableBlockedUserIds

    assertTrue("old ban date should be a candidate", ids.contains(oldBan))
    assertFalse("recent ban date should not be a candidate", ids.contains(recentBan))
    assertTrue("old lastlogin should be a candidate", ids.contains(oldLogin))
    assertTrue("old regdate should be a candidate", ids.contains(oldReg))
    assertTrue("no dates should be a candidate", ids.contains(noDates))
    assertFalse("recent lastlogin should take priority over old regdate", ids.contains(recentLogin))

  @Test
  def testDeleteBlockedUsers(): Unit =
    val id = createBlockedUser("test-delete-blocked", Some(ts("2015-01-01")), None, None)

    val deleted = userDao.deleteBlockedUsers(Seq(id))

    assertEquals(1, deleted)
    assertThrows(classOf[UserNotFoundException], () => userDao.getUser(id))

end UserDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class UserDaoIntegrationTestConfiguration:
  @Bean
  def userDao(springDB: SpringDB): UserDao = UserDao(springDB)

end UserDaoIntegrationTestConfiguration
