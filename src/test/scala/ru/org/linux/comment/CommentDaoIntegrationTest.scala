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
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.user.User
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[CommentDaoIntegrationTestConfiguration]))
class CommentDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var topicId: Int = scala.compiletime.uninitialized
  private var testUserId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    topicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testUserId = springDB.run:
      sql"select min(id) from users".map(rs => rs.int(1)).single.apply().get

  private def insertComment(commentId: Int, replyToId: Option[Int], title: String, body: String): Unit =
    springDB.run:
      val replyTo = replyToId.getOrElse(null: Integer)
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $testUserId, $title, CURRENT_TIMESTAMP,
                    $replyTo, 'f', $topicId, '127.0.0.1'::inet, create_user_agent('Integration test User Agent'))"""
        .update
        .apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, $body)".update.apply()

  test("getById"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "testGetById comment", "test body")

    val comment = commentDao.getById(commentId)
    assertEquals(comment.id, commentId)
    assertEquals(comment.title, "testGetById comment")
    assertEquals(comment.topicId, topicId)
    assertEquals(comment.userid, testUserId)
    assert(!comment.deleted)

  test("getByIdNotFound"):
    intercept[MessageNotFoundException] {
      commentDao.getById(999999999)
    }

  test("getCommentListWithDeleted"):
    val commentId1 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val commentId2 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId1, None, "visible comment", "body 1")
    insertComment(commentId2, None, "deleted comment", "body 2")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId2".update.apply()

    val comments = commentDao.getCommentList(topicId, showDeleted = true)
    assert(comments.size >= 2, "Should contain at least 2 comments")
    assert(comments.exists(_.id == commentId2), "Should contain deleted comment")

  test("getCommentListWithoutDeleted"):
    val commentId1 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val commentId2 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId1, None, "visible comment", "body 1")
    insertComment(commentId2, None, "should be hidden", "body 2")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId2".update.apply()

    val comments = commentDao.getCommentList(topicId, showDeleted = false)
    assert(comments.forall(_.id != commentId2), "Should not contain deleted comment")

  test("deleteComment"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "to be deleted", "body")

    val deleted = springDB.localTx {
      commentDao.deleteComment(commentId)
    }
    assert(deleted, "Should delete existing comment")

    val comment = commentDao.getById(commentId)
    assert(comment.deleted, "Comment should be marked as deleted")

    val deletedAgain = springDB.localTx {
      commentDao.deleteComment(commentId)
    }
    assert(!deletedAgain, "Should not delete already-deleted comment")

  test("undeleteComment"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "to be undeleted", "body")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId".update.apply()

    val comment = commentDao.getById(commentId)
    assert(comment.deleted, "Comment should be deleted")

    springDB.localTx:
      commentDao.undeleteComment(comment)
    val restored = commentDao.getById(commentId)
    assert(!restored.deleted, "Comment should be restored")

  test("getRepliesCount"):
    val parentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val childId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(parentId, None, "parent comment", "body")
    insertComment(childId, Some(parentId), "child comment", "body")

    assertEquals(commentDao.getRepliesCount(parentId), 1)
    assertEquals(commentDao.getRepliesCount(childId), 0)

  test("updateStatsAfterDelete"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "stats test", "body")

    val statBefore = springDB.run:
      sql"SELECT stat1, stat3 FROM topics WHERE id = $topicId"
        .map(rs => (rs.int("stat1"), rs.int("stat3")))
        .single
        .apply()
        .get

    springDB.localTx {
      commentDao.updateStatsAfterDelete(commentId, 1)
    }

    val statAfter = springDB.run:
      sql"SELECT stat1, stat3 FROM topics WHERE id = $topicId"
        .map(rs => (rs.int("stat1"), rs.int("stat3")))
        .single
        .apply()
        .get

    assertEquals(statAfter._1, statBefore._1 - 1)

  test("changeTitle"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "original title", "body")

    val oldComment = commentDao.getById(commentId)
    assertEquals(oldComment.title, "original title")

    springDB.localTx {
      commentDao.changeTitle(oldComment, "new title")
    }

    val updated = commentDao.getById(commentId)
    assertEquals(updated.title, "new title")

  test("updateLatestEditorInfo"):
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "editor test", "body")

    val editDate = new java.sql.Timestamp(System.currentTimeMillis())
    springDB.localTx {
      commentDao.updateLatestEditorInfo(commentId, testUserId, editDate, 5)
    }

    val comment = commentDao.getById(commentId)
    assertEquals(comment.editorId, testUserId)
    assertEquals(comment.editCount, 5)

  test("getCommentsByIPAddressForUpdate"):
    val ip = "127.0.0.1"
    val timedelta = new java.sql.Timestamp(System.currentTimeMillis() - 86400000)
    val result = springDB.localTx:
      commentDao.getCommentsByIPAddressForUpdate(ip, timedelta)
    assert(result != null)

  test("getAllByUserForUpdate"):
    val user = mock(classOf[User])
    when(user.id).thenReturn(testUserId)
    val result = springDB.localTx:
      commentDao.getAllByUserForUpdate(user)
    assert(result != null)

  test("getDeletedCommentsWhenTopicDeletedNotComment"):
    val groupId = springDB.run:
      sql"SELECT id FROM groups LIMIT 1".map(rs => rs.int("id")).single.apply().get

    val newTopicId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get

    springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES ($groupId, $testUserId, 'deleted topic test', '', 't', CURRENT_TIMESTAMP, $newTopicId, '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()

    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get

    springDB.run:
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $testUserId, 'test comment', CURRENT_TIMESTAMP,
                    ${null: Integer}, 'f', $newTopicId, '127.0.0.1'::inet,
                    create_user_agent('Integration test User Agent'))""".update.apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'test body')".update.apply()

    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $newTopicId".update.apply()
      sql"""INSERT INTO del_info (msgid, delby, reason, deldate, bonus)
            VALUES ($newTopicId, $testUserId, 'topic reason', CURRENT_TIMESTAMP, -5)""".update.apply()

    val result = commentDao.getDeletedComments(testUserId, DeletedCommentsFilterEnum.ALL, 0)
    val item = result.find(_.commentId == commentId)
    assert(item.isDefined, "Should find comment deleted with its topic")
    assertEquals(item.get.reason, null, "No reason from comdel when comment not individually deleted")
    assertEquals(item.get.bonus, 0)
    assert(item.get.topicDeleted)
    assert(!item.get.deleted)

end CommentDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class CommentDaoIntegrationTestConfiguration:

  @Bean
  def commentDao(springDB: SpringDB): CommentDao = CommentDao(springDB)

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = DeleteInfoDao(springDB)

end CommentDaoIntegrationTestConfiguration
