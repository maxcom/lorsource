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
package ru.org.linux.topic

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.gallery.ImageDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{SectionDao, SectionDaoImpl, SectionService}
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

import java.sql.Timestamp
import java.time.LocalDate

/** Интеграционные тесты окончательного удаления старых удалённых топиков и черновиков неактивных пользователей
  * ([[TopicDao.getDeletableDeletedTopicIds]], [[TopicDao.getDeletableDraftTopicIds]] и
  * [[TopicDao.purgeDeletedTopics]]).
  *
  * Даты вычисляются относительно текущей даты: пороги запросов — 3 года (дата удаления, заблокированные авторы) и 10
  * лет (неактивные авторы), контрольные значения берутся с запасом от границ.
  */
@ContextConfiguration(classes = Array(classOf[DeletedTopicPurgeIntegrationTestConfiguration]))
class DeletedTopicPurgeIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var imageDao: ImageDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var baseGroupId: Int = scala.compiletime.uninitialized

  private def yearsAgo(years: Int): Timestamp = Timestamp.valueOf(LocalDate.now.minusYears(years).atStartOfDay)

  private def monthsAgo(months: Int): Timestamp = Timestamp.valueOf(LocalDate.now.minusMonths(months).atStartOfDay)

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    baseGroupId = springDB.run:
      sql"select groupid from topics order by id limit 1".map(rs => rs.int(1)).single.apply().get

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

  private def insertTopic(topicId: Int, userId: Int, title: String, draft: Boolean = false): Unit =
    springDB.run:
      sql"""INSERT INTO topics (id, groupid, userid, title, postdate, deleted, draft, lastmod)
            VALUES ($topicId, $baseGroupId, $userId, $title, CURRENT_TIMESTAMP, 'f', $draft, CURRENT_TIMESTAMP)"""
        .update
        .apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($topicId, 'test message')".update.apply()

  private def insertDraft(userId: Int, title: String): Int =
    val id = nextMsgId
    insertTopic(id, userId, title, draft = true)
    id

  private def markTopicDeleted(topic: Int, deldate: Option[Timestamp], delby: Int): Unit =
    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $topic".update.apply()
      sql"""INSERT INTO del_info (msgid, delby, reason, deldate, bonus)
            VALUES ($topic, $delby, 'test topic deletion', ${deldate.orNull}, 0)""".update.apply()

  private def insertComment(commentId: Int, userId: Int, topic: Int): Unit =
    springDB.run:
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $userId, 'test comment', CURRENT_TIMESTAMP, NULL, 'f', $topic,
                    '127.0.0.1'::inet, create_user_agent('Integration test User Agent'))""".update.apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'comment body')".update.apply()

  private def insertImage(topic: Int, purged: Boolean): Int =
    springDB.run:
      sql"""INSERT INTO images (topic, extension, purged) VALUES ($topic, 'jpg', $purged)
            RETURNING id""".map(rs => rs.int("id")).single.apply().get

  private def insertUserEvent(
      userId: Int,
      eventType: String,
      topic: Int,
      commentId: Option[Int] = None,
      warningId: Option[Int] = None): Unit =
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

  test("getDeletableDeletedTopicIds"):
    val inactive = createUser("test-topic-purge-inactive", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))
    val blockedOld = createUser("test-topic-purge-blocked-old", blocked = true, Some(yearsAgo(4)), None)
    val noDates = createUser("test-topic-purge-no-dates", blocked = false, None, None)
    val regdateFallback = createUser("test-topic-purge-reg-fallback", blocked = false, None, Some(yearsAgo(11)))
    val active = createUser("test-topic-purge-active", blocked = false, Some(monthsAgo(1)), Some(yearsAgo(11)))
    val blockedRecent = createUser("test-topic-purge-blocked-recent", blocked = true, Some(monthsAgo(6)), None)
    val recentRegdate = createUser("test-topic-purge-recent-reg", blocked = false, None, Some(monthsAgo(6)))

    def deletedTopic(userId: Int, title: String, deldate: Option[Timestamp]): Int =
      val id = nextMsgId
      insertTopic(id, userId, title)
      markTopicDeleted(id, deldate, userId)
      id

    val candInactive = deletedTopic(inactive, "cand inactive", Some(yearsAgo(4)))
    val candBlocked = deletedTopic(blockedOld, "cand blocked", Some(yearsAgo(4)))
    val candNoDates = deletedTopic(noDates, "cand no dates", Some(yearsAgo(4)))
    val candRegdateFallback = deletedTopic(regdateFallback, "cand regdate fallback", Some(yearsAgo(4)))
    val candNoDeldate = deletedTopic(inactive, "cand no deldate", None)

    val ctrlActiveAuthor = deletedTopic(active, "ctrl active author", Some(yearsAgo(4)))
    val ctrlBlockedRecent = deletedTopic(blockedRecent, "ctrl blocked recent", Some(yearsAgo(4)))
    val ctrlRecentRegdate = deletedTopic(recentRegdate, "ctrl recent regdate", Some(yearsAgo(4)))
    val ctrlRecentDelete = deletedTopic(inactive, "ctrl recent delete", Some(monthsAgo(1)))
    val ctrlNoDeldateActive = deletedTopic(active, "ctrl no deldate active author", None)

    val ctrlWithComment = deletedTopic(inactive, "ctrl with comment", Some(yearsAgo(4)))
    insertComment(nextMsgId, active, ctrlWithComment)

    val ctrlNotDeleted = nextMsgId
    insertTopic(ctrlNotDeleted, inactive, "ctrl not deleted")

    val ctrlUnpurgedImage = deletedTopic(inactive, "ctrl unpurged image", Some(yearsAgo(4)))
    insertImage(ctrlUnpurgedImage, purged = false)

    val ids = topicDao.getDeletableDeletedTopicIds

    assert(ids.contains(candInactive), "inactive author should be a candidate")
    assert(ids.contains(candBlocked), "blocked author with old lastlogin should be a candidate")
    assert(ids.contains(candNoDates), "author without dates should be a candidate")
    assert(ids.contains(candRegdateFallback), "unknown lastlogin should fall back to old regdate")
    assert(ids.contains(candNoDeldate), "deleted topic without deldate should be a candidate")
    assert(!ids.contains(ctrlActiveAuthor), "active author should not be a candidate")
    assert(!ids.contains(ctrlBlockedRecent), "blocked author with recent lastlogin should not be a candidate")
    assert(!ids.contains(ctrlRecentRegdate), "recent regdate fallback should not be a candidate")
    assert(!ids.contains(ctrlRecentDelete), "recently deleted topic should not be a candidate")
    assert(!ids.contains(ctrlNoDeldateActive), "topic without deldate of active author should not be a candidate")
    assert(!ids.contains(ctrlWithComment), "topic with comment should not be a candidate")
    assert(!ids.contains(ctrlNotDeleted), "not deleted topic should not be a candidate")
    assert(!ids.contains(ctrlUnpurgedImage), "topic with unpurged image should not be a candidate")

  test("purgeDeletedTopics"):
    val author = createUser("test-topic-purge-author", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))
    val eventOwner = createUser("test-topic-purge-event-owner", blocked = false, Some(monthsAgo(1)), None)

    val targetId = nextMsgId
    insertTopic(targetId, author, "to be purged")
    markTopicDeleted(targetId, None, author)

    val controlId = nextMsgId
    insertTopic(controlId, author, "control topic")
    markTopicDeleted(controlId, Some(yearsAgo(4)), author)

    val tagId = springDB.run:
      sql"SELECT id FROM tags_values ORDER BY id LIMIT 1".map(rs => rs.int("id")).single.apply().get

    val pollId = springDB.run:
      val id = sql"select nextval('vote_id') as voteid".map(rs => rs.int("voteid")).single.apply().get
      sql"INSERT INTO polls (id, multiselect, topic) VALUES ($id, false, $targetId)".update.apply()
      sql"""INSERT INTO polls_variants (id, vote, label) VALUES (nextval('votes_id'), $id, 'variant')
            RETURNING id""".map(rs => rs.int("id")).single.apply().get
      sql"""INSERT INTO vote_users (vote, userid, variant_id)
            SELECT $id, $eventOwner, id FROM polls_variants WHERE vote = $id LIMIT 1""".update.apply()
      id

    springDB.run:
      sql"""INSERT INTO edit_info (msgid, editor, object_type)
            VALUES ($targetId, $author, 'TOPIC')""".update.apply()
      sql"INSERT INTO memories (userid, topic, watch) VALUES ($eventOwner, $targetId, false)".update.apply()
      sql"""INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction)
            VALUES ($eventOwner, $targetId, NULL, 'like')""".update.apply()
      sql"INSERT INTO images (topic, extension, purged) VALUES ($targetId, 'jpg', true)".update.apply()
      sql"INSERT INTO topic_users_notified (topic, userid) VALUES ($targetId, $eventOwner)".update.apply()
      sql"""INSERT INTO telegram_posts (topic_id, telegram_id, postdate)
            VALUES ($targetId, 987654, CURRENT_TIMESTAMP)""".update.apply()
      sql"INSERT INTO tags VALUES ($targetId, $tagId)".update.apply()
      sql"""INSERT INTO memories (userid, topic, watch) VALUES ($eventOwner, $controlId, false)""".update.apply()
      sql"INSERT INTO tags VALUES ($controlId, $tagId)".update.apply()

    val warningId = springDB.run:
      sql"""INSERT INTO message_warnings (topic, comment, author, message, warning_type)
            VALUES ($targetId, NULL, $author, 'test warning', 'rule')
            RETURNING id""".map(rs => rs.int("id")).single.apply().get

    insertUserEvent(eventOwner, "REF", targetId)
    insertUserEvent(eventOwner, "DEL", targetId)
    insertUserEvent(eventOwner, "WATCH", controlId)
    insertUserEvent(eventOwner, "WARNING", targetId, warningId = Some(warningId))

    val unreadBefore = getUnreadEvents(eventOwner)

    assert(topicDao.getDeletableDeletedTopicIds.contains(targetId), "target should be a candidate")

    val purged = topicDao.purgeDeletedTopics(Seq(targetId))

    assertEquals(purged, 1)
    assertEquals(countRows("topics", "id", targetId), 0)
    assertEquals(countRows("msgbase", "id", targetId), 0)
    assertEquals(countRows("del_info", "msgid", targetId), 0)
    assertEquals(countRows("edit_info", "msgid", targetId), 0)
    assertEquals(countRows("memories", "topic", targetId), 0)
    assertEquals(countRows("reactions_log", "topic_id", targetId), 0)
    assertEquals(countRows("images", "topic", targetId), 0)
    assertEquals(countRows("topic_users_notified", "topic", targetId), 0)
    assertEquals(countRows("telegram_posts", "topic_id", targetId), 0)
    assertEquals(countRows("tags", "msgid", targetId), 0)
    assertEquals(countRows("message_warnings", "topic", targetId), 0)
    assertEquals(countRows("polls", "topic", targetId), 0)
    assertEquals(countRows("polls_variants", "vote", pollId), 0)
    assertEquals(countRows("vote_users", "vote", pollId), 0)
    assertEquals(countRows("user_events", "message_id", targetId), 0)
    assertEquals(countRows("user_events", "warning_id", warningId), 0)

    assertEquals(countRows("topics", "id", controlId), 1)
    assertEquals(countRows("msgbase", "id", controlId), 1)
    assertEquals(countRows("del_info", "msgid", controlId), 1)
    // 2 строки: явная подписка eventOwner + автоподписка автора топика (триггер topins_t)
    assertEquals(countRows("memories", "topic", controlId), 2)
    assertEquals(countRows("tags", "msgid", controlId), 1)
    assertEquals(countRows("user_events", "message_id", controlId), 1)

    val unreadAfter = getUnreadEvents(eventOwner)
    assertEquals(unreadAfter, unreadBefore - 3, "unread events counter should be recalculated")

    intercept[MessageNotFoundException] {
      topicDao.getById(targetId)
    }

  test("purgeSkipsRestoredTopic"):
    val author = createUser("test-topic-purge-restored", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = nextMsgId
    insertTopic(topicId, author, "restored topic")
    markTopicDeleted(topicId, Some(yearsAgo(4)), author)

    assert(topicDao.getDeletableDeletedTopicIds.contains(topicId))

    springDB.run:
      sql"UPDATE topics SET deleted='f' WHERE id = $topicId".update.apply()

    val purged = topicDao.purgeDeletedTopics(Seq(topicId))

    assertEquals(purged, 0)
    assertEquals(countRows("topics", "id", topicId), 1)
    assertEquals(countRows("msgbase", "id", topicId), 1)

  test("purgeSkipsTopicsWithComments"):
    val author = createUser("test-topic-purge-comment", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))
    val commenter = createUser("test-topic-purge-commenter", blocked = false, Some(monthsAgo(1)), None)

    val topicId = nextMsgId
    insertTopic(topicId, author, "topic with comment")
    markTopicDeleted(topicId, Some(yearsAgo(4)), author)
    insertComment(nextMsgId, commenter, topicId)

    assert(!topicDao.getDeletableDeletedTopicIds.contains(topicId))

    val purged = topicDao.purgeDeletedTopics(Seq(topicId))

    assertEquals(purged, 0)
    assertEquals(countRows("topics", "id", topicId), 1)

  test("purgeSkipsTopicsWithUnpurgedImages"):
    val author = createUser("test-topic-purge-image", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = nextMsgId
    insertTopic(topicId, author, "topic with image")
    markTopicDeleted(topicId, Some(yearsAgo(4)), author)
    val imageId = insertImage(topicId, purged = false)

    assert(!topicDao.getDeletableDeletedTopicIds.contains(topicId), "topic with unpurged image is not a candidate")

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 0)
    assertEquals(countRows("topics", "id", topicId), 1)
    assertEquals(countRows("images", "id", imageId), 1)

    springDB.run:
      sql"UPDATE images SET purged=true WHERE id = $imageId".update.apply()

    assert(topicDao.getDeletableDeletedTopicIds.contains(topicId), "topic becomes candidate after image purge")

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 1)
    assertEquals(countRows("topics", "id", topicId), 0)
    assertEquals(countRows("images", "id", imageId), 0)

  test("getDeletableDraftTopicIds"):
    val inactive = createUser("test-draft-purge-inactive", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))
    val blockedOld = createUser("test-draft-purge-blocked-old", blocked = true, Some(yearsAgo(4)), None)
    val noDates = createUser("test-draft-purge-no-dates", blocked = false, None, None)
    val regdateFallback = createUser("test-draft-purge-reg-fallback", blocked = false, None, Some(yearsAgo(11)))
    val active = createUser("test-draft-purge-active", blocked = false, Some(monthsAgo(1)), Some(yearsAgo(11)))
    val blockedRecent = createUser("test-draft-purge-blocked-recent", blocked = true, Some(monthsAgo(6)), None)
    val recentRegdate = createUser("test-draft-purge-recent-reg", blocked = false, None, Some(monthsAgo(6)))

    val candInactive = insertDraft(inactive, "cand inactive draft")
    val candBlocked = insertDraft(blockedOld, "cand blocked draft")
    val candNoDates = insertDraft(noDates, "cand no dates draft")
    val candRegdateFallback = insertDraft(regdateFallback, "cand regdate fallback draft")

    val ctrlActiveAuthor = insertDraft(active, "ctrl active author draft")
    val ctrlBlockedRecent = insertDraft(blockedRecent, "ctrl blocked recent draft")
    val ctrlRecentRegdate = insertDraft(recentRegdate, "ctrl recent regdate draft")

    val ctrlPublished = nextMsgId
    insertTopic(ctrlPublished, inactive, "ctrl published topic")

    val ctrlDeletedDraft = insertDraft(inactive, "ctrl deleted draft")
    markTopicDeleted(ctrlDeletedDraft, Some(yearsAgo(4)), inactive)

    val ctrlWithComment = insertDraft(inactive, "ctrl with comment")
    insertComment(nextMsgId, active, ctrlWithComment)

    val ids = topicDao.getDeletableDraftTopicIds

    assert(ids.contains(candInactive), "draft of inactive author should be a candidate")
    assert(ids.contains(candBlocked), "draft of blocked author with old lastlogin should be a candidate")
    assert(ids.contains(candNoDates), "draft of author without dates should be a candidate")
    assert(ids.contains(candRegdateFallback), "unknown lastlogin should fall back to old regdate for drafts")
    assert(!ids.contains(ctrlActiveAuthor), "draft of active author should not be a candidate")
    assert(!ids.contains(ctrlBlockedRecent), "draft of blocked author with recent lastlogin should not be a candidate")
    assert(!ids.contains(ctrlRecentRegdate), "draft with recent regdate fallback should not be a candidate")
    assert(!ids.contains(ctrlPublished), "published topic should not be a candidate")
    assert(!ids.contains(ctrlDeletedDraft), "soft-deleted draft should not be a candidate")
    assert(!ids.contains(ctrlWithComment), "draft with comment should not be a candidate")

  test("purgeDeletedTopicsPurgesDraft"):
    val author = createUser("test-draft-purge-author", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))
    val eventOwner = createUser("test-draft-purge-event-owner", blocked = false, Some(monthsAgo(1)), None)

    val targetId = nextMsgId
    insertTopic(targetId, author, "draft to be purged", draft = true)

    val controlId = nextMsgId
    insertTopic(controlId, author, "draft control", draft = true)

    val tagId = springDB.run:
      sql"SELECT id FROM tags_values ORDER BY id LIMIT 1".map(rs => rs.int("id")).single.apply().get

    springDB.run:
      sql"INSERT INTO tags VALUES ($targetId, $tagId)".update.apply()
      sql"INSERT INTO memories (userid, topic, watch) VALUES ($eventOwner, $targetId, false)".update.apply()
      sql"""INSERT INTO reactions_log (origin_user, topic_id, comment_id, reaction)
            VALUES ($eventOwner, $targetId, NULL, 'like')""".update.apply()
      sql"INSERT INTO images (topic, extension, purged) VALUES ($targetId, 'jpg', true)".update.apply()
      sql"INSERT INTO topic_users_notified (topic, userid) VALUES ($targetId, $eventOwner)".update.apply()
      sql"""INSERT INTO telegram_posts (topic_id, telegram_id, postdate)
            VALUES ($targetId, 987654, CURRENT_TIMESTAMP)""".update.apply()
      sql"INSERT INTO tags VALUES ($controlId, $tagId)".update.apply()

    insertUserEvent(eventOwner, "WATCH", targetId)

    val unreadBefore = getUnreadEvents(eventOwner)

    assert(topicDao.getDeletableDraftTopicIds.contains(targetId), "target draft should be a candidate")
    assert(topicDao.getDeletableDraftTopicIds.contains(controlId), "control draft should be a candidate")

    val purged = topicDao.purgeDeletedTopics(Seq(targetId))

    assertEquals(purged, 1)
    assertEquals(countRows("topics", "id", targetId), 0)
    assertEquals(countRows("msgbase", "id", targetId), 0)
    assertEquals(countRows("edit_info", "msgid", targetId), 0)
    assertEquals(countRows("memories", "topic", targetId), 0)
    assertEquals(countRows("reactions_log", "topic_id", targetId), 0)
    assertEquals(countRows("images", "topic", targetId), 0)
    assertEquals(countRows("topic_users_notified", "topic", targetId), 0)
    assertEquals(countRows("telegram_posts", "topic_id", targetId), 0)
    assertEquals(countRows("tags", "msgid", targetId), 0)
    assertEquals(countRows("user_events", "message_id", targetId), 0)

    assertEquals(countRows("topics", "id", controlId), 1)
    assertEquals(countRows("tags", "msgid", controlId), 1)

    val unreadAfter = getUnreadEvents(eventOwner)
    assertEquals(unreadAfter, unreadBefore - 1, "unread events counter should be recalculated")

    intercept[MessageNotFoundException] {
      topicDao.getById(targetId)
    }

  test("purgeSkipsPublishedDraft"):
    val author = createUser("test-draft-purge-published", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = nextMsgId
    insertTopic(topicId, author, "draft to be published", draft = true)

    assert(topicDao.getDeletableDraftTopicIds.contains(topicId))

    springDB.run:
      sql"UPDATE topics SET draft='f' WHERE id = $topicId".update.apply()

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 0)
    assertEquals(countRows("topics", "id", topicId), 1)

  test("purgeSkipsDraftsWithUnpurgedImages"):
    val author = createUser("test-draft-purge-image", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = nextMsgId
    insertTopic(topicId, author, "draft with image", draft = true)
    val imageId = insertImage(topicId, purged = false)

    assert(topicDao.getDeletableDraftTopicIds.contains(topicId), "draft with unpurged image is a candidate")

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 0)
    assertEquals(countRows("topics", "id", topicId), 1)
    assertEquals(countRows("images", "id", imageId), 1)

    springDB.run:
      sql"UPDATE images SET purged=true WHERE id = $imageId".update.apply()

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 1)
    assertEquals(countRows("topics", "id", topicId), 0)
    assertEquals(countRows("images", "id", imageId), 0)

  test("purgeSkipsDraftOfReturnedUser"):
    val author = createUser("test-draft-purge-returned", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = insertDraft(author, "draft of returned user")

    assert(topicDao.getDeletableDraftTopicIds.contains(topicId), "draft of inactive author should be a candidate")

    springDB.run:
      sql"UPDATE users SET lastlogin = CURRENT_TIMESTAMP WHERE id = $author".update.apply()

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 0)
    assertEquals(countRows("topics", "id", topicId), 1)
    assertEquals(countRows("msgbase", "id", topicId), 1)

  test("purgeSkipsDeletedTopicOfReturnedUser"):
    val author = createUser("test-topic-purge-returned", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val topicId = nextMsgId
    insertTopic(topicId, author, "deleted topic of returned user")
    markTopicDeleted(topicId, Some(yearsAgo(4)), author)

    assert(topicDao.getDeletableDeletedTopicIds.contains(topicId), "topic of inactive author should be a candidate")

    springDB.run:
      sql"UPDATE users SET lastlogin = CURRENT_TIMESTAMP WHERE id = $author".update.apply()

    assertEquals(topicDao.purgeDeletedTopics(Seq(topicId)), 0)
    assertEquals(countRows("topics", "id", topicId), 1)
    assertEquals(countRows("msgbase", "id", topicId), 1)

  test("unpurgedImagesOfDrafts"):
    val author = createUser("test-image-dao-author", blocked = false, Some(yearsAgo(11)), Some(yearsAgo(11)))

    val draft1 = nextMsgId
    insertTopic(draft1, author, "draft 1", draft = true)
    val draft2 = nextMsgId
    insertTopic(draft2, author, "draft 2", draft = true)
    val published = nextMsgId
    insertTopic(published, author, "published topic")
    val deletedDraft = nextMsgId
    insertTopic(deletedDraft, author, "deleted draft", draft = true)
    markTopicDeleted(deletedDraft, Some(yearsAgo(4)), author)

    val unpurged1 = insertImage(draft1, purged = false)
    val unpurged2 = insertImage(draft1, purged = false)
    insertImage(draft1, purged = true)
    insertImage(draft2, purged = true)
    insertImage(published, purged = false)
    insertImage(deletedDraft, purged = false)

    val images = imageDao.unpurgedImagesOfDrafts(Seq(draft1, draft2, published, deletedDraft))

    assertEquals(images.map(_.id).toSet, Set(unpurged1, unpurged2), "only images of non-deleted drafts are returned")
    assertEquals(images.map(_.topicId).toSet, Set(draft1))
    assertEquals(imageDao.unpurgedImagesOfDrafts(Seq.empty), Seq.empty)

  test("purgeEmptyList"):
    assertEquals(topicDao.purgeDeletedTopics(Seq.empty), 0)

end DeletedTopicPurgeIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeletedTopicPurgeIntegrationTestConfiguration:

  @Bean
  def topicDao(springDB: SpringDB): TopicDao = TopicDao(springDB)

  @Bean
  def sectionDao(springDB: SpringDB): SectionDao = SectionDaoImpl(springDB)

  @Bean
  def sectionService(sectionDao: SectionDao): SectionService = SectionService(sectionDao)

  @Bean
  def imageDao(sectionService: SectionService, springDB: SpringDB): ImageDao = ImageDao(sectionService, springDB)

end DeletedTopicPurgeIntegrationTestConfiguration
