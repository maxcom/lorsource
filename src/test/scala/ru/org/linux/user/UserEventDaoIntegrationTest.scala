/*
 * Copyright 1998-2022 Linux.org.ru
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
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

object UserEventDaoIntegrationTest {
  private val TestTopicId = 98075
  private val TestUserId = 32670
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[UserEventDaoIntegrationTestConfiguration]))))
@Transactional
class UserEventDaoIntegrationTest {
  @Autowired
  var userEventDao: UserEventDao = _

  @Autowired
  var userDao: UserDao = _

  @Test
  def testAdd(): Unit = {
    createSimpleEvent()

    val events = userEventDao.getRepliesForUser(UserEventDaoIntegrationTest.TestUserId, showPrivate = true, 50, 0, None)

    assertEquals(1, events.size)
  }

  @Test
  def testInsertTopicUserNotification(): Unit =
    userEventDao.insertTopicNotification(UserEventDaoIntegrationTest.TestTopicId, Seq(UserEventDaoIntegrationTest.TestUserId))

  @Test(expected = classOf[DuplicateKeyException])
  def testInsertTopicUserNotificationDup(): Unit = {
    userEventDao.insertTopicNotification(UserEventDaoIntegrationTest.TestTopicId, Seq(UserEventDaoIntegrationTest.TestUserId))
    userEventDao.insertTopicNotification(UserEventDaoIntegrationTest.TestTopicId, Seq(UserEventDaoIntegrationTest.TestUserId))
  }

  private def createSimpleEvent(): Unit =
    userEventDao.addEvent(
      eventType = UserEventFilterEnum.TAG.toString,
      userId = UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(UserEventDaoIntegrationTest.TestTopicId),
      commentId = None,
      message = None)

  @Test
  def testAddRemove(): Unit = {
    createSimpleEvent()
    val events = userEventDao.getRepliesForUser(UserEventDaoIntegrationTest.TestUserId, showPrivate = true, 50, 0, None)
    assertEquals(1, events.size)
    userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    val eventsAfterDelete = userEventDao.getRepliesForUser(UserEventDaoIntegrationTest.TestUserId, showPrivate = true, 50, 0, None)
    assertEquals(0, eventsAfterDelete.size)
  }

  @Test
  def testRemoveSyntax():Unit = userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))

  @Test
  def testRecalc(): Unit = {
    createSimpleEvent()
    assertEquals(1, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).getUnreadEvents)
    val affected = userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    assertEquals(1, affected.size)
    assertEquals(1, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).getUnreadEvents)
    userEventDao.recalcEventCount(Seq(UserEventDaoIntegrationTest.TestUserId))
    assertEquals(0, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).getUnreadEvents)
  }
}