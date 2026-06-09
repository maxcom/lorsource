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
    springDB.localTx { userDao.block(user, user, "") }
    val userAfter = userDao.getUser(UserDaoIntegrationTest.TestId)
    assertTrue(userAfter.blocked)

end UserDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class UserDaoIntegrationTestConfiguration:
  @Bean
  def userDao(springDB: SpringDB): UserDao = UserDao(springDB)

end UserDaoIntegrationTestConfiguration
