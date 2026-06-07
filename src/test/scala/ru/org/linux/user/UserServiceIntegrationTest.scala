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
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

object UserServiceIntegrationTest:
  private val TestId = 7806

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration])))) @Transactional
class UserServiceIntegrationTest:
  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  @Before @After
  def fixUser(): Unit =
    springDB.run:
      sql"UPDATE users SET blocked='f' WHERE id=${UserServiceIntegrationTest.TestId}".update.apply()
      sql"DELETE FROM ban_info WHERE userid=${UserServiceIntegrationTest.TestId}".update.apply()

  @Before
  def clearCache(): Unit = userService.idToUserCache.invalidateAll()

  @Test
  def testUserCached(): Unit =
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)

    springDB.run:
      sql"UPDATE users SET blocked='t' WHERE id=${UserServiceIntegrationTest.TestId}".update.apply()

    val userCached = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assertFalse(userCached.blocked)

    val userNotCached = userService.getUser(user.nick)
    assertTrue(userNotCached.blocked)

  @Test
  def testCachePutOnGet(): Unit =
    userService.idToUserCache.invalidate(UserServiceIntegrationTest.TestId)

    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assertNotNull(user)
    assertFalse(user.blocked)
    assertNotNull(userService.idToUserCache.get(user.id))

  @Test
  def testBlock(): Unit =
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    userService.block(user, user, "")
    val userAfter = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assertTrue(userAfter.blocked)

  @Test
  def testCacheResetOnBlock(): Unit =
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    userService.block(user, user, "")
    val userAfter = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assertTrue(userAfter.blocked)

end UserServiceIntegrationTest
