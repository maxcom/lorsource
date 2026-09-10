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
package ru.org.linux.comment

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

import java.sql.Timestamp

/** Интеграционные тесты окончательного удаления старых удалённых комментариев
  * ([[CommentDao.getDeletableDeletedCommentIds]] и [[CommentDao.purgeDeletedComments]]).
  */
@ContextConfiguration(classes = Array(classOf[DeletedCommentPurgeIntegrationTestConfiguration]))
class DeletedCommentPurgeIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var topicId: Int = scala.compiletime.uninitialized

  private def ts(value: String): Timestamp = Timestamp.valueOf(value + " 00:00:00")

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    topicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get

  private def nextMsgId: Int =
    springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get

  private def createUser(
      nick: String,
      blocked: Boolean,
      lastlogin: Option[Timestamp],
      regdate: Option[Timestamp]): Int =
    springDB.run:
      sql"""INSERT INTO users (id, name, nick, passwd, score, max_score, regdate, blocked, lastlogin)
            VALUES (nextval('s_uid'), '', $nick, 'x', 45, 45, ${regdate.orNull}, $blocked, ${lastlogin.orNull})
            RETURNING id""".map(rs => rs.int("id")).single.apply().get

  private def insertComment(
      commentId: Int,
      userId: Int,
      replyTo: Option[Int],
      deleted: Boolean,
      body: String,
      topic: Int = topicId): Unit =
    springDB.run:
      val replyToValue = replyTo.getOrElse(null: Integer)
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $userId, 'test comment', CURRENT_TIMESTAMP,
                    $replyToValue, $deleted, $topic, '127.0.0.1'::inet,
                    create_user_agent('Integration test User Agent'))""".update.apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, $body)".update.apply()

  private def insertDelInfo(msgid: Int, delby: Int, deldate: Timestamp): Unit =
    springDB.run:
      sql"""INSERT INTO del_info (msgid, delby, reason, deldate, bonus)
            VALUES ($msgid, $delby, 'test reason', $deldate, 0)""".update.apply()

  private def nextTopicId(after: Int): Int =
    springDB.run:
      sql"select min(id) from topics where not deleted and id > $after".map(rs => rs.int(1)).single.apply().get

  private def markTopicDeleted(topic: Int, deldate: Timestamp, delby: Int): Unit =
    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $topic".update.apply()
      sql"""INSERT INTO del_info (msgid, delby, reason, deldate, bonus)
            VALUES ($topic, $delby, 'test topic deletion', $deldate, 0)""".update.apply()

  private def insertUserEvent(
      userId: Int,
      eventType: String,
      commentId: Option[Int],
      warningId: Option[Int],
      topic: Int = topicId): Unit =
    springDB.run:
      sql"""INSERT INTO user_events (userid, type, private, message_id, comment_id, warning_id)
            VALUES ($userId, ${eventType}::event_type, false, $topic, ${commentId.orNull}, ${warningId.orNull})"""
        .update
        .apply()

  private def countRows(table: String, column: String, value: Int): Int =
    val tableSyntax = SQLSyntax.createUnsafely(table)
    val columnSyntax = SQLSyntax.createUnsafely(column)
    springDB.run:
      sql"SELECT count(*) FROM $tableSyntax WHERE $columnSyntax = $value".map(rs => rs.int(1)).single.apply().get

  private def getUnreadEvents(userId: Int): Int =
    springDB.run:
      sql"SELECT unread_events FROM users WHERE id = $userId".map(rs => rs.int("unread_events")).single.apply().get

  test("getDeletableDeletedCommentIds"):
    val oldLogin = createUser("test-purge-old-login", blocked = false, Some(ts("2015-01-01")), Some(ts("2015-01-01")))
    val blockedOldLogin = createUser("test-purge-blocked-old", blocked = true, Some(ts("2023-01-01")), None)
    val blockedRecentLogin = createUser("test-purge-blocked-recent", blocked = true, Some(ts("2026-01-01")), None)
    val active = createUser("test-purge-active", blocked = false, Some(ts("2026-08-01")), Some(ts("2015-01-01")))
    val nullDates = createUser("test-purge-null-dates", blocked = false, None, None)
    val regdateFallback = createUser("test-purge-reg-fallback", blocked = false, None, Some(ts("2015-01-01")))
    val recentRegdate = createUser("test-purge-recent-reg", blocked = false, None, Some(ts("2026-01-01")))

    val candOldLogin = nextMsgId
    insertComment(candOldLogin, oldLogin, None, deleted = true, "cand old login")
    insertDelInfo(candOldLogin, oldLogin, ts("2015-01-01"))

    val candBlocked = nextMsgId
    insertComment(candBlocked, blockedOldLogin, None, deleted = true, "cand blocked")
    insertDelInfo(candBlocked, blockedOldLogin, ts("2015-01-01"))

    val candNullDates = nextMsgId
    insertComment(candNullDates, nullDates, None, deleted = true, "cand null dates")
    insertDelInfo(candNullDates, nullDates, ts("2015-01-01"))

    val candRegdateFallback = nextMsgId
    insertComment(candRegdateFallback, regdateFallback, None, deleted = true, "cand regdate fallback")
    insertDelInfo(candRegdateFallback, regdateFallback, ts("2015-01-01"))

    val ctrlActiveAuthor = nextMsgId
    insertComment(ctrlActiveAuthor, active, None, deleted = true, "ctrl active author")
    insertDelInfo(ctrlActiveAuthor, active, ts("2015-01-01"))

    val ctrlBlockedRecent = nextMsgId
    insertComment(ctrlBlockedRecent, blockedRecentLogin, None, deleted = true, "ctrl blocked recent login")
    insertDelInfo(ctrlBlockedRecent, blockedRecentLogin, ts("2015-01-01"))

    val ctrlRecentRegdate = nextMsgId
    insertComment(ctrlRecentRegdate, recentRegdate, None, deleted = true, "ctrl recent regdate")
    insertDelInfo(ctrlRecentRegdate, recentRegdate, ts("2015-01-01"))

    val ctrlRecentDelete = nextMsgId
    insertComment(ctrlRecentDelete, oldLogin, None, deleted = true, "ctrl recent delete")
    insertDelInfo(ctrlRecentDelete, oldLogin, ts("2026-08-01"))

    val ctrlHasReply = nextMsgId
    insertComment(ctrlHasReply, oldLogin, None, deleted = true, "ctrl has reply")
    insertDelInfo(ctrlHasReply, oldLogin, ts("2015-01-01"))
    insertComment(nextMsgId, active, Some(ctrlHasReply), deleted = false, "reply")

    val ctrlHasDeletedReply = nextMsgId
    insertComment(ctrlHasDeletedReply, oldLogin, None, deleted = true, "ctrl has deleted reply")
    insertDelInfo(ctrlHasDeletedReply, oldLogin, ts("2015-01-01"))
    val deletedReplyId = nextMsgId
    insertComment(deletedReplyId, active, Some(ctrlHasDeletedReply), deleted = true, "deleted reply")
    insertDelInfo(deletedReplyId, active, ts("2015-01-01"))

    val ctrlNotDeleted = nextMsgId
    insertComment(ctrlNotDeleted, oldLogin, None, deleted = false, "ctrl not deleted")

    val ids = commentDao.getDeletableDeletedCommentIds

    assert(ids.contains(candOldLogin), "old lastlogin author should be a candidate")
    assert(ids.contains(candBlocked), "blocked author with old lastlogin should be a candidate")
    assert(ids.contains(candNullDates), "author without dates should be a candidate")
    assert(ids.contains(candRegdateFallback), "unknown lastlogin should fall back to old regdate")
    assert(!ids.contains(ctrlActiveAuthor), "active author should not be a candidate")
    assert(!ids.contains(ctrlBlockedRecent), "blocked author with recent lastlogin should not be a candidate")
    assert(!ids.contains(ctrlRecentRegdate), "recent regdate fallback should not be a candidate")
    assert(!ids.contains(ctrlRecentDelete), "recently deleted comment should not be a candidate")
    assert(!ids.contains(ctrlHasReply), "comment with reply should not be a candidate")
    assert(!ids.contains(ctrlHasDeletedReply), "comment with deleted reply should not be a candidate")
    assert(!ids.contains(ctrlNotDeleted), "not deleted comment should not be a candidate")

  test("getDeletableCommentIdsInDeletedTopics"):
    val moderator = createUser("test-purge-topic-mod", blocked = false, Some(ts("2026-08-01")), None)

    val deletedTopic = nextTopicId(topicId)
    val recentTopic = nextTopicId(deletedTopic)
    markTopicDeleted(deletedTopic, ts("2015-01-01"), moderator)
    markTopicDeleted(recentTopic, ts("2026-08-01"), moderator)

    val blockedOld = createUser("test-purge-topic-blocked-old", blocked = true, Some(ts("2023-01-01")), None)
    val inactive = createUser(
      "test-purge-topic-inactive",
      blocked = false,
      Some(ts("2015-01-01")),
      Some(ts("2015-01-01")))
    val noDates = createUser("test-purge-topic-no-dates", blocked = false, None, None)
    val active = createUser("test-purge-topic-active", blocked = false, Some(ts("2026-08-01")), Some(ts("2015-01-01")))
    val blockedRecent = createUser("test-purge-topic-blocked-recent", blocked = true, Some(ts("2026-01-01")), None)

    val candBlocked = nextMsgId
    insertComment(candBlocked, blockedOld, None, deleted = false, "topic cand blocked", deletedTopic)

    val candInactive = nextMsgId
    insertComment(candInactive, inactive, None, deleted = false, "topic cand inactive", deletedTopic)

    val candNoDates = nextMsgId
    insertComment(candNoDates, noDates, None, deleted = false, "topic cand no dates", deletedTopic)

    val candBoth = nextMsgId
    insertComment(candBoth, inactive, None, deleted = true, "topic cand both branches", deletedTopic)
    insertDelInfo(candBoth, inactive, ts("2015-01-01"))

    val ctrlActive = nextMsgId
    insertComment(ctrlActive, active, None, deleted = false, "topic ctrl active", deletedTopic)

    val ctrlBlockedRecent = nextMsgId
    insertComment(ctrlBlockedRecent, blockedRecent, None, deleted = false, "topic ctrl blocked recent", deletedTopic)

    val ctrlRecentDelete = nextMsgId
    insertComment(ctrlRecentDelete, blockedOld, None, deleted = false, "topic ctrl recent delete", recentTopic)

    val ctrlHasReply = nextMsgId
    insertComment(ctrlHasReply, blockedOld, None, deleted = false, "topic ctrl has reply", deletedTopic)
    insertComment(nextMsgId, active, Some(ctrlHasReply), deleted = false, "topic ctrl reply", deletedTopic)

    val ctrlNotDeletedTopic = nextMsgId
    insertComment(ctrlNotDeletedTopic, blockedOld, None, deleted = false, "topic ctrl not deleted topic")

    val ids = commentDao.getDeletableDeletedCommentIds

    assert(ids.contains(candBlocked), "blocked author in deleted topic should be a candidate")
    assert(ids.contains(candInactive), "inactive author in deleted topic should be a candidate")
    assert(ids.contains(candNoDates), "author without dates in deleted topic should be a candidate")
    assert(ids.contains(candBoth), "individually deleted comment in deleted topic should be a candidate")
    assertEquals(ids.count(_ == candBoth), 1, "comment matching both branches should be listed once")
    assert(!ids.contains(ctrlActive), "active author in deleted topic should not be a candidate")
    assert(!ids.contains(ctrlBlockedRecent), "blocked author with recent lastlogin should not be a candidate")
    assert(!ids.contains(ctrlRecentDelete), "comment in recently deleted topic should not be a candidate")
    assert(!ids.contains(ctrlHasReply), "comment with reply in deleted topic should not be a candidate")
    assert(!ids.contains(ctrlNotDeletedTopic), "comment in not deleted topic should not be a candidate")

  test("purgeDeletedComments"):
    val author = createUser("test-purge-author", blocked = false, Some(ts("2026-08-01")), Some(ts("2015-01-01")))
    val eventOwner = createUser("test-purge-event-owner", blocked = false, Some(ts("2026-08-01")), None)

    val targetId = nextMsgId
    insertComment(targetId, author, None, deleted = true, "to be purged")
    insertDelInfo(targetId, author, ts("2015-01-01"))

    val controlId = nextMsgId
    insertComment(controlId, author, None, deleted = true, "control comment")
    insertDelInfo(controlId, author, ts("2015-01-01"))

    springDB.run:
      sql"""INSERT INTO edit_info (msgid, editor, object_type)
            VALUES ($targetId, $author, 'COMMENT')""".update.apply()
      sql"""INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction)
            VALUES ($eventOwner, $topicId, $targetId, 'like')""".update.apply()

    insertUserEvent(eventOwner, "REPLY", Some(targetId), None)
    insertUserEvent(eventOwner, "DEL", Some(targetId), None)
    insertUserEvent(eventOwner, "REF", None, None)
    insertUserEvent(eventOwner, "REPLY", Some(controlId), None)

    val warningId = springDB.run:
      sql"""INSERT INTO message_warnings (topic, comment, author, message, warning_type)
            VALUES ($topicId, $targetId, $author, 'test warning', 'rule')
            RETURNING id""".map(rs => rs.int("id")).single.apply().get
    insertUserEvent(eventOwner, "WARNING", None, Some(warningId))

    val unreadBefore = getUnreadEvents(eventOwner)

    val purged = commentDao.purgeDeletedComments(Seq(targetId))

    assertEquals(purged, 1)
    assertEquals(countRows("comments", "id", targetId), 0)
    assertEquals(countRows("msgbase", "id", targetId), 0)
    assertEquals(countRows("del_info", "msgid", targetId), 0)
    assertEquals(countRows("edit_info", "msgid", targetId), 0)
    assertEquals(countRows("reactions_log", "comment_id", targetId), 0)
    assertEquals(countRows("message_warnings", "comment", targetId), 0)
    assertEquals(countRows("user_events", "comment_id", targetId), 0)
    assertEquals(countRows("user_events", "warning_id", warningId), 0)

    assertEquals(countRows("comments", "id", controlId), 1)
    assertEquals(countRows("msgbase", "id", controlId), 1)
    assertEquals(countRows("del_info", "msgid", controlId), 1)
    assertEquals(countRows("user_events", "comment_id", controlId), 1)

    val unreadAfter = getUnreadEvents(eventOwner)
    assertEquals(unreadAfter, unreadBefore - 3, "unread events counter should be recalculated")

    intercept[MessageNotFoundException] {
      commentDao.getById(targetId)
    }

  test("purgeCommentInDeletedTopic"):
    val author = createUser("test-purge-in-topic-author", blocked = true, Some(ts("2023-01-01")), None)
    val eventOwner = createUser("test-purge-in-topic-event-owner", blocked = false, Some(ts("2026-08-01")), None)

    val deletedTopic = nextTopicId(topicId)
    markTopicDeleted(deletedTopic, ts("2015-01-01"), author)

    val targetId = nextMsgId
    insertComment(targetId, author, None, deleted = false, "to be purged from deleted topic", deletedTopic)

    val controlId = nextMsgId
    insertComment(controlId, author, None, deleted = false, "control comment in not deleted topic")

    springDB.run:
      sql"""INSERT INTO edit_info (msgid, editor, object_type)
            VALUES ($targetId, $author, 'COMMENT')""".update.apply()
      sql"""INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction)
            VALUES ($eventOwner, $deletedTopic, $targetId, 'like')""".update.apply()

    insertUserEvent(eventOwner, "REPLY", Some(targetId), None, deletedTopic)
    insertUserEvent(eventOwner, "REF", None, None, deletedTopic)

    val warningId = springDB.run:
      sql"""INSERT INTO message_warnings (topic, comment, author, message, warning_type)
            VALUES ($deletedTopic, $targetId, $author, 'test warning', 'rule')
            RETURNING id""".map(rs => rs.int("id")).single.apply().get
    insertUserEvent(eventOwner, "WARNING", None, Some(warningId), deletedTopic)

    val unreadBefore = getUnreadEvents(eventOwner)

    val purged = commentDao.purgeDeletedComments(Seq(targetId, controlId))

    assertEquals(purged, 1)
    assertEquals(countRows("comments", "id", targetId), 0)
    assertEquals(countRows("msgbase", "id", targetId), 0)
    assertEquals(countRows("del_info", "msgid", targetId), 0)
    assertEquals(countRows("edit_info", "msgid", targetId), 0)
    assertEquals(countRows("reactions_log", "comment_id", targetId), 0)
    assertEquals(countRows("message_warnings", "comment", targetId), 0)
    assertEquals(countRows("user_events", "comment_id", targetId), 0)
    assertEquals(countRows("user_events", "warning_id", warningId), 0)
    assertEquals(countRows("comments", "id", controlId), 1)
    assertEquals(countRows("msgbase", "id", controlId), 1)

    val unreadAfter = getUnreadEvents(eventOwner)
    assertEquals(unreadAfter, unreadBefore - 2, "unread events counter should be recalculated")

  test("purgeSkipsNotDeletedComments"):
    val author = createUser("test-purge-skip", blocked = false, Some(ts("2026-08-01")), None)
    val commentId = nextMsgId
    insertComment(commentId, author, None, deleted = false, "not deleted")

    val purged = commentDao.purgeDeletedComments(Seq(commentId))

    assertEquals(purged, 0)
    assertEquals(countRows("comments", "id", commentId), 1)
    assertEquals(countRows("msgbase", "id", commentId), 1)

  test("purgeEmptyList"):
    assertEquals(commentDao.purgeDeletedComments(Seq.empty), 0)

end DeletedCommentPurgeIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeletedCommentPurgeIntegrationTestConfiguration:

  @Bean
  def commentDao(springDB: SpringDB): CommentDao = CommentDao(springDB)

end DeletedCommentPurgeIntegrationTestConfiguration
