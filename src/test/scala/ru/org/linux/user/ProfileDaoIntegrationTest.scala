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

import org.junit.Assert.{assertEquals, assertNotSame}
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

object ProfileDaoIntegrationTest {
  private val TestId = 1
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(
  new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[ProfileDaoIntegrationTestConfiguration]))
))
@Transactional
class ProfileDaoIntegrationTest {
  @Autowired
  var profileDao: ProfileDao = scala.compiletime.uninitialized

  @Test
  def testModification(): Unit = {
    val profile = Profile.DEFAULT

    assertNotSame(125, profile.messages)

    val builder = new ProfileBuilder(profile)
    builder.setMessages(125)

    val testUser = mock(classOf[User])
    when(testUser.id).thenReturn(ProfileDaoIntegrationTest.TestId)

    profileDao.writeProfile(testUser, builder)

    val profile1 = profileDao.readProfile(testUser.id)

    profileDao.deleteProfile(testUser)

    assertEquals(125, profile1.messages)
  }
}
