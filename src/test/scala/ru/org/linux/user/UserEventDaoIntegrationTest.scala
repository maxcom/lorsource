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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

import java.sql.BatchUpdateException

object UserEventDaoIntegrationTest:
  private val TestTopicId = 98075
  private val TestUserId = 32670

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[UserEventDaoIntegrationTestConfiguration]))
  ))
class UserEventDaoIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var userEventDao: UserEventDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  test("add"):
    createSimpleEvent()

    val events = userEventDao.getRepliesForUser(
      UserEventDaoIntegrationTest.TestUserId,
      showPrivate = true,
      50,
      0,
      UserEventFilterEnum.ALL)

    assertEquals(events.size, 1)

  test("insertTopicUserNotification"):
    userEventDao.insertTopicNotification(
      UserEventDaoIntegrationTest.TestTopicId,
      Seq(UserEventDaoIntegrationTest.TestUserId))

  test("insertTopicUserNotificationDup"):
    intercept[BatchUpdateException] {
      userEventDao.insertTopicNotification(
        UserEventDaoIntegrationTest.TestTopicId,
        Seq(UserEventDaoIntegrationTest.TestUserId))
      userEventDao.insertTopicNotification(
        UserEventDaoIntegrationTest.TestTopicId,
        Seq(UserEventDaoIntegrationTest.TestUserId))
    }

  private def createSimpleEvent(): Unit =
    userEventDao.addEvent(
      eventType = UserEventFilterEnum.TAG.getType,
      userId = UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(UserEventDaoIntegrationTest.TestTopicId),
      commentId = None,
      message = None
    )

  test("addRemove"):
    createSimpleEvent()
    val events = userEventDao.getRepliesForUser(
      UserEventDaoIntegrationTest.TestUserId,
      showPrivate = true,
      50,
      0,
      UserEventFilterEnum.ALL)
    assertEquals(events.size, 1)
    springDB.localTx {
      userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    }
    val eventsAfterDelete = userEventDao.getRepliesForUser(
      UserEventDaoIntegrationTest.TestUserId,
      showPrivate = true,
      50,
      0,
      UserEventFilterEnum.ALL)
    assertEquals(eventsAfterDelete.size, 0)

  test("removeSyntax"):
    springDB.localTx {
      userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    }

  test("recalc"):
    createSimpleEvent()
    assertEquals(userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents, 1)
    val affected = springDB.localTx {
      userEventDao.deleteTopicEvents(Seq(UserEventDaoIntegrationTest.TestTopicId))
    }
    assertEquals(affected.size, 1)
    assertEquals(userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents, 1)
    springDB.localTx {
      userEventDao.recalcEventCount(Seq(UserEventDaoIntegrationTest.TestUserId))
    }
    assertEquals(userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents, 0)

  test("resetUnreadReactionGroup"):
    val topicId = springDB.run {
      sql"SELECT topic FROM comments WHERE NOT deleted GROUP BY topic HAVING count(*) >= 2 ORDER BY topic LIMIT 1"
        .map(rs => rs.int("topic"))
        .single
        .apply()
        .get
    }
    val commentIds = springDB.run {
      sql"SELECT id FROM comments WHERE topic=$topicId AND NOT deleted ORDER BY id LIMIT 2"
        .map(rs => rs.int("id"))
        .list
        .apply()
    }
    assertEquals(commentIds.size, 2)

    val firstCommentId = commentIds.head
    val secondCommentId = commentIds(1)
    val unreadBefore = userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents
    val maxEventIdBefore = springDB.run {
      sql"SELECT coalesce(max(id), 0) as maxid FROM user_events".map(rs => rs.int("maxid")).single.apply().get
    }

    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.getType,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(firstCommentId),
      message = None,
      originUser = Some(userDao.findUserId("maxcom"))
    )
    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.getType,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(firstCommentId),
      message = None,
      originUser = Some(userDao.findUserId("svu"))
    )
    userEventDao.addEvent(
      UserEventFilterEnum.REACTION.getType,
      UserEventDaoIntegrationTest.TestUserId,
      isPrivate = false,
      topicId = Some(topicId),
      commentId = Some(secondCommentId),
      message = None,
      originUser = Some(userDao.findUserId("edo"))
    )

    val insertedEvents = springDB.run {
      sql"""SELECT id, comment_id, unread FROM user_events WHERE userid=${UserEventDaoIntegrationTest
          .TestUserId} AND id>${maxEventIdBefore} AND type='REACTION' ORDER BY id"""
        .map(rs => (rs.int("id"), rs.int("comment_id"), rs.boolean("unread")))
        .list
        .apply()
    }
    assertEquals(insertedEvents.size, 3)

    val firstEventId = insertedEvents.head._1
    val lastEventId = insertedEvents(1)._1

    springDB.localTx:
      userEventDao.resetUnreadReactionGroup(
        UserEventDaoIntegrationTest.TestUserId,
        firstEventId,
        lastEventId,
        topicId,
        firstCommentId)

    val unreadFlagsAfterReset =
      springDB
        .run {
          sql"""SELECT id, unread FROM user_events WHERE userid=${UserEventDaoIntegrationTest
              .TestUserId} AND id>${maxEventIdBefore} AND type='REACTION' ORDER BY id"""
            .map(rs => rs.int("id") -> rs.boolean("unread"))
            .list
            .apply()
        }
        .toMap

    assertEquals(unreadFlagsAfterReset(firstEventId), false)
    assertEquals(unreadFlagsAfterReset(lastEventId), false)
    assertEquals(unreadFlagsAfterReset(insertedEvents(2)._1), true)
    assertEquals(userDao.getUser(UserEventDaoIntegrationTest.TestUserId).unreadEvents, unreadBefore + 1)

  private def eventCount(userId: Int): Int =
    springDB.run:
      sql"SELECT count(*) FROM user_events WHERE userid=$userId".map(rs => rs.int(1)).single.apply().get

  private def banUser(userId: Int, bannedYearsAgo: Int): Unit =
    springDB.run:
      sql"UPDATE users SET blocked='t', lastlogin=CURRENT_TIMESTAMP - interval '5 years' WHERE id=$userId".update
        .apply()
      sql"""INSERT INTO ban_info (userid, reason, ban_by)
            VALUES ($userId, 'test', ${userDao.findUserId("maxcom")})""".update.apply()
      sql"""UPDATE ban_info SET bandate=CURRENT_TIMESTAMP - make_interval(years => $bannedYearsAgo)
            WHERE userid=$userId""".update.apply()

  test("dropBannedUserEvents deletes all events of long-banned user"):
    val userId = UserEventDaoIntegrationTest.TestUserId

    banUser(userId, bannedYearsAgo = 4)
    createSimpleEvent()

    val deleted = springDB.localTx:
      userEventDao.dropBannedUserEvents()

    assert(deleted >= 1)
    assertEquals(eventCount(userId), 0)
    assertEquals(userDao.getUser(userId).unreadEvents, 0)

  test("dropBannedUserEvents keeps events of recently banned user"):
    val userId = UserEventDaoIntegrationTest.TestUserId

    // lastlogin старше bandate: событие сохраняется только если берётся именно дата блокировки
    banUser(userId, bannedYearsAgo = 2)
    createSimpleEvent()

    springDB.localTx:
      userEventDao.dropBannedUserEvents()

    assertEquals(eventCount(userId), 1)

  test("dropBannedUserEvents uses lastlogin when bandate is missing"):
    val userId = UserEventDaoIntegrationTest.TestUserId

    springDB.run:
      sql"UPDATE users SET blocked='t', lastlogin=CURRENT_TIMESTAMP - interval '4 years' WHERE id=$userId".update
        .apply()

    createSimpleEvent()

    springDB.localTx:
      userEventDao.dropBannedUserEvents()

    assertEquals(eventCount(userId), 0)
    assertEquals(userDao.getUser(userId).unreadEvents, 0)

  test("dropBannedUserEvents treats user without dates as old"):
    val userId = UserEventDaoIntegrationTest.TestUserId

    springDB.run:
      sql"UPDATE users SET blocked='t', lastlogin=NULL WHERE id=$userId".update.apply()

    createSimpleEvent()

    springDB.localTx:
      userEventDao.dropBannedUserEvents()

    assertEquals(eventCount(userId), 0)

  test("dropOldEvents deletes events older than 10 years"):
    val userId = UserEventDaoIntegrationTest.TestUserId
    createSimpleEvent()
    createSimpleEvent()

    springDB.run:
      sql"""UPDATE user_events SET event_date=CURRENT_TIMESTAMP - interval '11 years'
            WHERE id=(SELECT min(id) FROM user_events WHERE userid=$userId)""".update.apply()

    assertEquals(userDao.getUser(userId).unreadEvents, 2)

    springDB.localTx:
      userEventDao.dropOldEvents()

    assertEquals(eventCount(userId), 1)
    assertEquals(userDao.getUser(userId).unreadEvents, 1)
