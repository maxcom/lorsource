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
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport

@ContextConfiguration(classes = Array(classOf[IgnoreListDaoIntegrationTestConfiguration]))
class IgnoreListDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var ignoreListDao: IgnoreListDao = scala.compiletime.uninitialized

  private def mockUser(id: Int, moderator: Boolean = false): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    when(user.isModerator).thenReturn(moderator)
    user

  test("addAndGetIgnored"):
    val owner = mockUser(1)
    val ignored = mockUser(2)

    ignoreListDao.addUser(owner, ignored)

    val result = ignoreListDao.get(1)
    assert(result.contains(2), "Should contain ignored user")

  test("addDuplicateIsNoop"):
    val owner = mockUser(1)
    val ignored = mockUser(2)

    val initialSize = ignoreListDao.get(1).size
    ignoreListDao.addUser(owner, ignored)
    assertEquals(ignoreListDao.get(1).size, initialSize + 1, "Should have one more entry")
    ignoreListDao.addUser(owner, ignored)
    assertEquals(ignoreListDao.get(1).size, initialSize + 1, "Should not add duplicate")

  test("removeIgnored"):
    val owner = mockUser(1)
    val ignored = mockUser(2)

    ignoreListDao.addUser(owner, ignored)
    ignoreListDao.remove(owner, ignored)

    val result = ignoreListDao.get(1)
    assert(!result.contains(2), "Should not contain removed user")

  test("cannotIgnoreModerator"):
    val owner = mockUser(1)
    val moderator = mockUser(2, moderator = true)

    intercept[AccessViolationException] {
      ignoreListDao.addUser(owner, moderator)
    }

  test("getIgnoreCount"):
    val ignored = mockUser(2)
    val owner = mockUser(1)

    ignoreListDao.addUser(owner, ignored)

    val count = ignoreListDao.getIgnoreCount(ignored)
    assert(count >= 1, "Ignore count should be at least 1")

  test("isIgnoredReturnsFalseWhenNotIgnored"):
    val result = ignoreListDao.isIgnored(99999, 99999)
    assert(!result, "Should not be ignored")

  test("getReturnsEmptyForNoIgnores"):
    val result = ignoreListDao.get(99999)
    assert(result.isEmpty, "Should be empty for unknown user")

end IgnoreListDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class IgnoreListDaoIntegrationTestConfiguration:

  @Bean
  def ignoreListDao(springDB: SpringDB) = new IgnoreListDao(springDB)

end IgnoreListDaoIntegrationTestConfiguration
