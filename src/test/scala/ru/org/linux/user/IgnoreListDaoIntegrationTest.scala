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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.scalikejdbc.SpringDB

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[IgnoreListDaoIntegrationTestConfiguration])) @Transactional
class IgnoreListDaoIntegrationTest:

  @Autowired
  var ignoreListDao: IgnoreListDao = scala.compiletime.uninitialized

  private def mockUser(id: Int, moderator: Boolean = false): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    when(user.isModerator).thenReturn(moderator)
    user

  @Test
  def testAddAndGetIgnored(): Unit =
    val owner = mockUser(1)
    val ignored = mockUser(2)

    ignoreListDao.addUser(owner, ignored)

    val result = ignoreListDao.get(1)
    assertTrue("Should contain ignored user", result.contains(2))

  @Test
  def testAddDuplicateIsNoop(): Unit =
    val owner = mockUser(1)
    val ignored = mockUser(2)

    ignoreListDao.addUser(owner, ignored)
    ignoreListDao.addUser(owner, ignored)

    val result = ignoreListDao.get(1)
    assertEquals("Should have exactly one entry", 1, result.size)

  @Test
  def testRemoveIgnored(): Unit =
    val owner = mockUser(1)
    val ignored = mockUser(2)

    ignoreListDao.addUser(owner, ignored)
    ignoreListDao.remove(owner, ignored)

    val result = ignoreListDao.get(1)
    assertFalse("Should not contain removed user", result.contains(2))

  @Test
  def testCannotIgnoreModerator(): Unit =
    val owner = mockUser(1)
    val moderator = mockUser(2, moderator = true)

    assertThrows(classOf[AccessViolationException], () => ignoreListDao.addUser(owner, moderator))

  @Test
  def testGetIgnoreCount(): Unit =
    val ignored = mockUser(2)
    val owner = mockUser(1)

    ignoreListDao.addUser(owner, ignored)

    val count = ignoreListDao.getIgnoreCount(ignored)
    assertTrue("Ignore count should be at least 1", count >= 1)

  @Test
  def testIsIgnoredReturnsFalseWhenNotIgnored(): Unit =
    val result = ignoreListDao.isIgnored(99999, 99999)
    assertFalse("Should not be ignored", result)

  @Test
  def testGetReturnsEmptyForNoIgnores(): Unit =
    val result = ignoreListDao.get(99999)
    assertTrue("Should be empty for unknown user", result.isEmpty)

end IgnoreListDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class IgnoreListDaoIntegrationTestConfiguration:

  @Bean
  def ignoreListDao(springDB: SpringDB) = new IgnoreListDao(springDB)

end IgnoreListDaoIntegrationTestConfiguration
