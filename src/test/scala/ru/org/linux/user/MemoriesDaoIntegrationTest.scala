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
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.Topic
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[MemoriesDaoIntegrationTestConfiguration]))
class MemoriesDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var memoriesDao: MemoriesDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get

  private def mockTopic(id: Int): Topic =
    val topic = mock(classOf[Topic])
    when(topic.id).thenReturn(id)
    topic

  test("getWatchCountForUser"):
    val maxcom = userDao.getUser(1)
    assert(memoriesDao.getWatchCountForUser(maxcom) > 0, "Should have watch count > 0")

  test("isWatchPresetForUser"):
    val maxcom = userDao.getUser(1)
    assert(memoriesDao.isWatchPresetForUser(maxcom), "Should have watch preset")

  test("getWatchCountForUserWithNoMemories"):
    val anonymous = userDao.getUser(2)
    assertEquals(memoriesDao.getWatchCountForUser(anonymous), 0)

  test("isFavPresetForUserWithNoMemories"):
    val anonymous = userDao.getUser(2)
    assert(!memoriesDao.isFavPresetForUser(anonymous), "Should not have fav preset")

  test("addToMemoriesWatch"):
    val user = userDao.getUser(1)
    val topic = mockTopic(testTopicId)
    val id = memoriesDao.addToMemories(user, topic, watch = true)
    assert(id > 0, "Should return valid id")

  test("addToMemoriesIdempotent"):
    val user = userDao.getUser(1)
    val topic = mockTopic(testTopicId)
    val id1 = memoriesDao.addToMemories(user, topic, watch = true)
    val id2 = memoriesDao.addToMemories(user, topic, watch = true)
    assertEquals(id2, id1, "Should return same id on duplicate")

  test("getTopicInfoWithUser"):
    val user = userDao.getUser(1)
    val info = memoriesDao.getTopicInfo(testTopicId, Some(user))
    assert(info != null)

  test("getTopicInfoWithoutUser"):
    val info = memoriesDao.getTopicInfo(testTopicId, None)
    assert(info != null)

  test("deleteMemories"):
    val user = userDao.getUser(1)
    val topic = mockTopic(testTopicId)
    val id = memoriesDao.addToMemories(user, topic, watch = true)
    memoriesDao.delete(id)
    val item = memoriesDao.getMemoriesListItem(id)
    assert(!item.isPresent, "Should be empty after delete")

end MemoriesDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class MemoriesDaoIntegrationTestConfiguration:

  @Bean
  def memoriesDao(springDB: SpringDB): MemoriesDao = new MemoriesDao(springDB)

  @Bean
  def userDao(springDB: SpringDB): UserDao = new UserDao(springDB)

end MemoriesDaoIntegrationTestConfiguration
