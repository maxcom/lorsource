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
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.test.TransactionalTestSupport

object ProfileDaoIntegrationTest:
  private val TestId = 1

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[ProfileDaoIntegrationTestConfiguration]))
  ))
class ProfileDaoIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var profileDao: ProfileDao = scala.compiletime.uninitialized

  test("writeAndRead"):
    val profile = Profile.DEFAULT

    assert(profile.messages != 125)

    val builder = new ProfileBuilder(profile)
    builder.setMessages(125)

    val testUser = mock(classOf[User])
    when(testUser.id).thenReturn(ProfileDaoIntegrationTest.TestId)

    profileDao.writeProfile(testUser, builder)

    val profile1 = profileDao.readProfile(testUser.id)

    profileDao.deleteProfile(testUser)

    assertEquals(profile1.messages, 125)

  test("updateProfile"):
    val testUser = mock(classOf[User])
    when(testUser.id).thenReturn(ProfileDaoIntegrationTest.TestId)

    val builder1 = new ProfileBuilder(Profile.DEFAULT)
    builder1.setMessages(50)
    profileDao.writeProfile(testUser, builder1)

    val profile1 = profileDao.readProfile(testUser.id)
    assertEquals(profile1.messages, 50)

    val builder2 = new ProfileBuilder(profile1)
    builder2.setMessages(200)
    profileDao.writeProfile(testUser, builder2)

    val profile2 = profileDao.readProfile(testUser.id)
    assertEquals(profile2.messages, 200)

    profileDao.deleteProfile(testUser)

  test("deleteProfile"):
    val testUser = mock(classOf[User])
    when(testUser.id).thenReturn(ProfileDaoIntegrationTest.TestId)

    val builder = new ProfileBuilder(Profile.DEFAULT)
    builder.setMessages(99)
    profileDao.writeProfile(testUser, builder)

    val profile1 = profileDao.readProfile(testUser.id)
    assertEquals(profile1.messages, 99)

    profileDao.deleteProfile(testUser)

    val profile2 = profileDao.readProfile(testUser.id)
    assert(profile2 != null)
    assertEquals(profile2.messages, Profile.DEFAULT.messages)

end ProfileDaoIntegrationTest
