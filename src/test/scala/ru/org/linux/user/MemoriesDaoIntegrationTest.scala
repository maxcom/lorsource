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
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.Topic

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[MemoriesDaoIntegrationTestConfiguration])) @Transactional
class MemoriesDaoIntegrationTest:

  @Autowired
  var memoriesDao: MemoriesDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  private def mockTopic(id: Int): Topic =
    val topic = mock(classOf[Topic])
    when(topic.id).thenReturn(id)
    topic

  @Test
  def testGetWatchCountForUser(): Unit =
    val maxcom = userDao.getUser(1)
    assertTrue("Should have watch count > 0", memoriesDao.getWatchCountForUser(maxcom) > 0)

  @Test
  def testIsWatchPresetForUser(): Unit =
    val maxcom = userDao.getUser(1)
    assertTrue("Should have watch preset", memoriesDao.isWatchPresetForUser(maxcom))

  @Test
  def testGetWatchCountForUserWithNoMemories(): Unit =
    val anonymous = userDao.getUser(2)
    assertEquals(0, memoriesDao.getWatchCountForUser(anonymous))

  @Test
  def testIsFavPresetForUserWithNoMemories(): Unit =
    val anonymous = userDao.getUser(2)
    assertFalse("Should not have fav preset", memoriesDao.isFavPresetForUser(anonymous))

  @Test
  def testAddToMemoriesWatch(): Unit =
    val user = userDao.getUser(1)
    val topic = mockTopic(1948660)
    val id = memoriesDao.addToMemories(user, topic, watch = true)
    assertTrue("Should return valid id", id > 0)

  @Test
  def testAddToMemoriesIdempotent(): Unit =
    val user = userDao.getUser(1)
    val topic = mockTopic(1948660)
    val id1 = memoriesDao.addToMemories(user, topic, watch = true)
    val id2 = memoriesDao.addToMemories(user, topic, watch = true)
    assertEquals("Should return same id on duplicate", id1, id2)

  @Test
  def testGetTopicInfoWithUser(): Unit =
    val user = userDao.getUser(1)
    val info = memoriesDao.getTopicInfo(1948660, Some(user))
    assertNotNull(info)

  @Test
  def testGetTopicInfoWithoutUser(): Unit =
    val info = memoriesDao.getTopicInfo(1948660, None)
    assertNotNull(info)

  @Test
  def testDeleteMemories(): Unit =
    val user = userDao.getUser(1)
    val topic = mockTopic(1948660)
    val id = memoriesDao.addToMemories(user, topic, watch = true)
    memoriesDao.delete(id)
    val item = memoriesDao.getMemoriesListItem(id)
    assertFalse("Should be empty after delete", item.isPresent)

end MemoriesDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class MemoriesDaoIntegrationTestConfiguration:

  @Bean
  def memoriesDao(springDB: SpringDB): MemoriesDao = new MemoriesDao(springDB)

  @Bean
  def userDao(dataSource: javax.sql.DataSource): UserDao = new UserDao(dataSource)

end MemoriesDaoIntegrationTestConfiguration
