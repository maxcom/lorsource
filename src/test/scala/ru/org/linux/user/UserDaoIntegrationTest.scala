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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

import java.sql.Timestamp

object UserDaoIntegrationTest:
  private val TestId = 7806

@ContextConfiguration(classes = Array(classOf[UserDaoIntegrationTestConfiguration]))
class UserDaoIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    fixUser()

  override def afterEach(context: AfterEach): Unit =
    fixUser()
    super.afterEach(context)

  private def fixUser(): Unit =
    springDB.run:
      sql"UPDATE users SET blocked='f' WHERE id=${UserDaoIntegrationTest.TestId}".update.apply()
      sql"DELETE FROM ban_info WHERE userid=${UserDaoIntegrationTest.TestId}".update.apply()

  test("user"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    assert(user != null)
    assert(!user.blocked)

  test("block"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    springDB.localTx {
      userDao.block(user, user, "")
    }
    val userAfter = userDao.getUser(UserDaoIntegrationTest.TestId)
    assert(userAfter.blocked)

  test("reset"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    val tm = userDao.getResetDate(user)

    springDB.localTx {
      userDao.updateResetDate(user, tm.plusSeconds(60))
    }

    val after = userDao.getResetDate(user)

    assertEquals(after, tm.plusSeconds(60))

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

  test("getDeletableBlockedUsers"):
    val oldBan = createBlockedUser("test-old-ban", Some(ts("2015-01-01")), None, None)
    val recentBan = createBlockedUser("test-recent-ban", Some(ts("2026-01-01")), None, None)
    val oldLogin = createBlockedUser("test-old-login", None, Some(ts("2015-01-01")), None)
    val oldReg = createBlockedUser("test-old-reg", None, None, Some(ts("2015-01-01")))
    val noDates = createBlockedUser("test-no-dates", None, None, None)
    val recentLogin = createBlockedUser("test-recent-login", None, Some(ts("2026-01-01")), Some(ts("2015-01-01")))

    val ids = userDao.getDeletableBlockedUserIds

    assert(ids.contains(oldBan), "old ban date should be a candidate")
    assert(!ids.contains(recentBan), "recent ban date should not be a candidate")
    assert(ids.contains(oldLogin), "old lastlogin should be a candidate")
    assert(ids.contains(oldReg), "old regdate should be a candidate")
    assert(ids.contains(noDates), "no dates should be a candidate")
    assert(!ids.contains(recentLogin), "recent lastlogin should take priority over old regdate")

  test("deleteBlockedUsers"):
    val id = createBlockedUser("test-delete-blocked", Some(ts("2015-01-01")), None, None)

    val deleted = userDao.deleteBlockedUsers(Seq(id))

    assertEquals(deleted, 1)
    intercept[UserNotFoundException] {
      userDao.getUser(id)
    }

  private def setIp(id: Int, ip: Option[String]): Unit =
    springDB.run:
      ip match
        case Some(v) =>
          sql"UPDATE users SET lastip=${v}::inet WHERE id=${id}".update.apply()
        case None =>
          sql"UPDATE users SET lastip=NULL WHERE id=${id}".update.apply()

  private def getIp(id: Int): String =
    springDB.run(
      sql"SELECT host(lastip) AS ip FROM users WHERE id=${id}".map(rs => rs.string("ip")).single.apply().orNull)

  test("updateLastloginStoresIp"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)

    val updated = userDao.updateLastlogin(user, force = true, "192.168.1.10")

    assert(updated)
    assertEquals(getIp(UserDaoIntegrationTest.TestId), "192.168.1.10")

  test("updateLastloginThrottled"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    val id = UserDaoIntegrationTest.TestId

    springDB.run:
      sql"UPDATE users SET lastlogin=CURRENT_TIMESTAMP-'2 hours'::interval, lastip=NULL WHERE id=${id}".update.apply()

    assert(userDao.updateLastlogin(user, force = false, "10.1.2.3"))
    assertEquals(getIp(id), "10.1.2.3")

    assert(!userDao.updateLastlogin(user, force = false, "10.1.9.9"))
    assertEquals(getIp(id), "10.1.2.3", "lastip must not change within 1 hour")

  test("sameNetworkAsLastLogin"):
    val user = userDao.getUser(UserDaoIntegrationTest.TestId)
    val id = UserDaoIntegrationTest.TestId

    setIp(id, None)
    assert(!userDao.sameNetworkAsLastLogin(user, "10.1.2.3"), "unknown previous ip must not match")

    setIp(id, Some("10.1.2.3"))
    assert(userDao.sameNetworkAsLastLogin(user, "10.1.2.250"), "same /24 must match")
    assert(!userDao.sameNetworkAsLastLogin(user, "10.1.3.3"), "different /24 must not match")

    setIp(id, Some("2001:db8:1::1"))
    assert(userDao.sameNetworkAsLastLogin(user, "2001:db8:1::9"), "same /64 must match")
    assert(!userDao.sameNetworkAsLastLogin(user, "2001:db8:2::5"), "different /64 must not match")

    setIp(id, Some("10.1.2.3"))
    assert(!userDao.sameNetworkAsLastLogin(user, "2001:db8:1::1"), "different address families must not match")

end UserDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class UserDaoIntegrationTestConfiguration:
  @Bean
  def userDao(springDB: SpringDB): UserDao = UserDao(springDB)

end UserDaoIntegrationTestConfiguration
