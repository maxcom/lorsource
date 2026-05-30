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
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

import javax.sql.DataSource

object UserEventDaoIntegrationTest:
  private val TestTopicId = 98075
  private val TestUserId = 32670

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[UserEventDaoIntegrationTestConfiguration]))
  )) @Transactional
class UserEventDaoIntegrationTest:
  @Autowired
  var userEventDao: UserEventDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  private var jdbcTemplate: JdbcTemplate = scala.compiletime.uninitialized

  @Autowired
  def setDataSource(ds: DataSource): Unit = jdbcTemplate = new JdbcTemplate(ds)

  @Test
  def testAdd(): Unit =
    createSimpleEvent()

    val events = userEventDao.getRepliesForUser(UserEventDaoIntegrationTest.TestUserId, showPrivate = true, 50, 0, None)

    assertEquals(1, events.size)

  @Test
  def testInsertTopicUserNotification(): Unit =
    userEventDao.insertTopicNotification(
      UserEventDaoIntegrationTest.TestTopicId,
      Seq(UserEventDaoIntegrationTest.TestUserId))

  @Test(expected = classOf[DuplicateKeyException])
  def testInsertTopicUserNotificationDup(): Unit =
    userEventDao.insertTopicNotification(
      UserEventDaoIntegrationTest.TestTopicId,
      Seq(UserEventDaoIntegrationTest.TestUserId))
    userEventDao.insertTopicNotification(
      UserEventDaoIntegrationTest.TestTopicId,
      Seq(UserEventDaoIntegrationTest.TestUserId))

  private def createSimpleEvent(): Unit =
    userEventDao.addEvent(
      eventType = UserEventFilterEnum.TAG.toString,
      userId = UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(UserEventDaoIntegrationTest.TestTopicId),
      commentId = None,
      message = None
    )

  @Test
  def testAddRemove(): Unit =
    createSimpleEvent()
    val events = userEventDao.getRepliesForUser(UserEventDaoIntegrationTest.TestUserId, showPrivate = true, 50, 0, None)
    assertEquals(1, events.size)
    userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    val eventsAfterDelete = userEventDao.getRepliesForUser(
      UserEventDaoIntegrationTest.TestUserId,
      showPrivate = true,
      50,
      0,
      None)
    assertEquals(0, eventsAfterDelete.size)

  @Test
  def testRemoveSyntax(): Unit = userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))

  @Test
  def testRecalc(): Unit =
    createSimpleEvent()
    assertEquals(1, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents)
    val affected = userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    assertEquals(1, affected.size)
    assertEquals(1, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents)
    userEventDao.recalcEventCount(Seq(UserEventDaoIntegrationTest.TestUserId))
    assertEquals(0, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents)

  @Test
  def testResetUnreadReactionGroup(): Unit =
    val topicId =
      jdbcTemplate
        .queryForObject[Int](
          "SELECT topic FROM comments WHERE NOT deleted GROUP BY topic HAVING count(*) >= 2 ORDER BY topic LIMIT 1")
        .get
    val commentIds =
      jdbcTemplate.queryAndMap("SELECT id FROM comments WHERE topic=? AND NOT deleted ORDER BY id LIMIT 2", topicId) {
        (rs, _) =>
          rs.getInt("id")
      }
    assertEquals(2, commentIds.size)

    val firstCommentId = commentIds.head
    val secondCommentId = commentIds(1)
    val unreadBefore = userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents
    val maxEventIdBefore = jdbcTemplate.queryForObject[Int]("SELECT coalesce(max(id), 0) FROM user_events").get

    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.toString,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(firstCommentId),
      message = None,
      originUser = Some(userDao.findUserId("maxcom"))
    )
    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.toString,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(firstCommentId),
      message = None,
      originUser = Some(userDao.findUserId("svu"))
    )
    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.toString,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(secondCommentId),
      message = None,
      originUser = Some(userDao.findUserId("edo"))
    )

    val insertedEvents =
      jdbcTemplate.queryAndMap(
        "SELECT id, comment_id, unread FROM user_events WHERE userid=? AND id>? AND type='REACTION' ORDER BY id",
        UserEventDaoIntegrationTest.TestUserId,
        maxEventIdBefore
      ) { (rs, _) =>
        (rs.getInt("id"), rs.getInt("comment_id"), rs.getBoolean("unread"))
      }
    assertEquals(3, insertedEvents.size)

    val firstEventId = insertedEvents.head._1
    val lastEventId = insertedEvents(1)._1

    userEventDao.resetUnreadReactionGroup(
      UserEventDaoIntegrationTest.TestUserId,
      firstEventId,
      lastEventId,
      topicId,
      firstCommentId)

    val unreadFlagsAfterReset =
      jdbcTemplate
        .queryAndMap(
          "SELECT id, unread FROM user_events WHERE userid=? AND id>? AND type='REACTION' ORDER BY id",
          UserEventDaoIntegrationTest.TestUserId,
          maxEventIdBefore
        ) { (rs, _) =>
          (rs.getInt("id"), rs.getBoolean("unread"))
        }
        .toMap

    assertEquals(false, unreadFlagsAfterReset(firstEventId))
    assertEquals(false, unreadFlagsAfterReset(lastEventId))
    assertEquals(true, unreadFlagsAfterReset(insertedEvents(2)._1))
    assertEquals(unreadBefore + 1, userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents)
