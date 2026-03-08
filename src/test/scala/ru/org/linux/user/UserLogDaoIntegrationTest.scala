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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

object UserLogDaoIntegrationTest {
  private val TestId = 1
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(
  new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[UserLogDaoIntegrationTestConfiguration]))
))
@Transactional
class UserLogDaoIntegrationTest {
  @Autowired
  var userLogDao: UserLogDao = _

  @Test
  def testLogAcceptEmail(): Unit = {
    val user = mock(classOf[User])
    when(user.id).thenReturn(UserLogDaoIntegrationTest.TestId)
    when(user.email).thenReturn("old@email")

    val oldLogItems = userLogDao.getLogItems(user, includeSelf = true)

    userLogDao.logAcceptNewEmail(user, "test@email")

    val logItems = userLogDao.getLogItems(user, includeSelf = true)

    assertEquals(1, logItems.size - oldLogItems.size)

    val item = logItems.head

    assertNotNull(item)
    assertEquals(UserLogAction.ACCEPT_NEW_EMAIL, item.action)
  }

  @Test
  def testLogScore50(): Unit = {
    val user = mock(classOf[User])
    when(user.id).thenReturn(UserLogDaoIntegrationTest.TestId)

    val oldLogItems = userLogDao.getLogItems(user, includeSelf = true)

    userLogDao.logScore50(user, user)

    val logItems = userLogDao.getLogItems(user, includeSelf = true)

    assertEquals(1, logItems.size - oldLogItems.size)

    val item = logItems.head

    assertNotNull(item)
    assertEquals(UserLogAction.SCORE50, item.action)
  }
}
