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
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

object UserServiceIntegrationTest:
  private val TestId = 7806

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class UserServiceIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    fixUser()
    clearCache()

  override def afterEach(context: AfterEach): Unit =
    fixUser()
    super.afterEach(context)

  // бывший @Before @After fixUser(): выполняется и до, и после каждого теста
  private def fixUser(): Unit =
    springDB.run:
      sql"UPDATE users SET blocked='f' WHERE id=${UserServiceIntegrationTest.TestId}".update.apply()
      sql"DELETE FROM ban_info WHERE userid=${UserServiceIntegrationTest.TestId}".update.apply()

  // бывший @Before clearCache()
  private def clearCache(): Unit = userService.idToUserCache.invalidateAll()

  test("userCached"):
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)

    springDB.run:
      sql"UPDATE users SET blocked='t' WHERE id=${UserServiceIntegrationTest.TestId}".update.apply()

    val userCached = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assert(!userCached.blocked)

    val userNotCached = userService.getUser(user.nick)
    assert(userNotCached.blocked)

  test("cachePutOnGet"):
    userService.idToUserCache.invalidate(UserServiceIntegrationTest.TestId)

    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assert(user != null)
    assert(!user.blocked)
    assert(userService.idToUserCache.get(user.id) != null)

  test("block"):
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    userService.block(user, user, "")
    val userAfter = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assert(userAfter.blocked)

  test("cacheResetOnBlock"):
    val user = userService.getUserCached(UserServiceIntegrationTest.TestId)
    userService.block(user, user, "")
    val userAfter = userService.getUserCached(UserServiceIntegrationTest.TestId)
    assert(userAfter.blocked)

end UserServiceIntegrationTest
