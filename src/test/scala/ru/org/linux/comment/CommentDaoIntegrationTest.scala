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

import org.junit.Assert.*
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.user.User
import scalikejdbc.*

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[CommentDaoIntegrationTestConfiguration])) @Transactional
class CommentDaoIntegrationTest:

  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var topicId: Int = scala.compiletime.uninitialized
  private var testUserId: Int = scala.compiletime.uninitialized

  @Before
  def setUp(): Unit =
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

  @Test
  def testGetById(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "testGetById comment", "test body")

    val comment = commentDao.getById(commentId)
    assertEquals(commentId, comment.id)
    assertEquals("testGetById comment", comment.title)
    assertEquals(topicId, comment.topicId)
    assertEquals(testUserId, comment.userid)
    assertFalse(comment.deleted)

  @Test(expected = classOf[MessageNotFoundException])
  def testGetByIdNotFound(): Unit = commentDao.getById(999999999)

  @Test
  def testGetCommentListWithDeleted(): Unit =
    val commentId1 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val commentId2 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId1, None, "visible comment", "body 1")
    insertComment(commentId2, None, "deleted comment", "body 2")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId2".update.apply()

    val comments = commentDao.getCommentList(topicId, showDeleted = true)
    assertTrue("Should contain at least 2 comments", comments.size >= 2)
    assertTrue("Should contain deleted comment", comments.exists(_.id == commentId2))

  @Test
  def testGetCommentListWithoutDeleted(): Unit =
    val commentId1 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val commentId2 = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId1, None, "visible comment", "body 1")
    insertComment(commentId2, None, "should be hidden", "body 2")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId2".update.apply()

    val comments = commentDao.getCommentList(topicId, showDeleted = false)
    assertTrue("Should not contain deleted comment", comments.forall(_.id != commentId2))

  @Test
  def testDeleteComment(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "to be deleted", "body")

    val deleted = commentDao.deleteComment(commentId)
    assertTrue("Should delete existing comment", deleted)

    val comment = commentDao.getById(commentId)
    assertTrue("Comment should be marked as deleted", comment.deleted)

    val deletedAgain = commentDao.deleteComment(commentId)
    assertFalse("Should not delete already-deleted comment", deletedAgain)

  @Test
  def testUndeleteComment(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "to be undeleted", "body")
    springDB.run:
      sql"UPDATE comments SET deleted='t' WHERE id = $commentId".update.apply()

    val comment = commentDao.getById(commentId)
    assertTrue("Comment should be deleted", comment.deleted)

    commentDao.undeleteComment(comment)
    val restored = commentDao.getById(commentId)
    assertFalse("Comment should be restored", restored.deleted)

  @Test
  def testGetRepliesCount(): Unit =
    val parentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val childId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(parentId, None, "parent comment", "body")
    insertComment(childId, Some(parentId), "child comment", "body")

    assertEquals(1, commentDao.getRepliesCount(parentId))
    assertEquals(0, commentDao.getRepliesCount(childId))

  @Test
  def testUpdateStatsAfterDelete(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "stats test", "body")

    val statBefore = springDB.run:
      sql"SELECT stat1, stat3 FROM topics WHERE id = $topicId"
        .map(rs => (rs.int("stat1"), rs.int("stat3")))
        .single
        .apply()
        .get

    commentDao.updateStatsAfterDelete(commentId, 1)

    val statAfter = springDB.run:
      sql"SELECT stat1, stat3 FROM topics WHERE id = $topicId"
        .map(rs => (rs.int("stat1"), rs.int("stat3")))
        .single
        .apply()
        .get

    assertEquals(statBefore._1 - 1, statAfter._1)

  @Test
  def testChangeTitle(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "original title", "body")

    val oldComment = commentDao.getById(commentId)
    assertEquals("original title", oldComment.title)

    commentDao.changeTitle(oldComment, "new title")

    val updated = commentDao.getById(commentId)
    assertEquals("new title", updated.title)

  @Test
  def testUpdateLatestEditorInfo(): Unit =
    val commentId = springDB.run:
      sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    insertComment(commentId, None, "editor test", "body")

    val editDate = new java.sql.Timestamp(System.currentTimeMillis())
    commentDao.updateLatestEditorInfo(commentId, testUserId, editDate, 5)

    val comment = commentDao.getById(commentId)
    assertEquals(testUserId, comment.editorId)
    assertEquals(5, comment.editCount)

  @Test
  def testGetCommentsByIPAddressForUpdate(): Unit =
    val ip = "127.0.0.1"
    val timedelta = new java.sql.Timestamp(System.currentTimeMillis() - 86400000)
    val result = commentDao.getCommentsByIPAddressForUpdate(ip, timedelta)
    assertNotNull(result)

  @Test
  def testGetAllByUserForUpdate(): Unit =
    val user = mock(classOf[User])
    when(user.id).thenReturn(testUserId)
    val result = commentDao.getAllByUserForUpdate(user)
    assertNotNull(result)

  @Test
  def testGetDeletedCommentsWhenTopicDeletedNotComment(): Unit =
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
                    create_user_agent('Integration test User Agent'))"""
        .update
        .apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'test body')".update.apply()

    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $newTopicId".update.apply()
      sql"""INSERT INTO del_info (msgid, delby, reason, deldate, bonus)
            VALUES ($newTopicId, $testUserId, 'topic reason', CURRENT_TIMESTAMP, -5)"""
        .update
        .apply()

    val result = commentDao.getDeletedComments(testUserId)
    val item = result.find(_.commentId == commentId)
    assertTrue("Should find comment deleted with its topic", item.isDefined)
    assertNull("No reason from comdel when comment not individually deleted", item.get.reason)
    assertEquals(0, item.get.bonus)
    assertTrue(item.get.topicDeleted)
    assertFalse(item.get.deleted)

end CommentDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class CommentDaoIntegrationTestConfiguration:

  @Bean
  def commentDao(springDB: SpringDB): CommentDao = CommentDao(springDB)

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = DeleteInfoDao(springDB)

end CommentDaoIntegrationTestConfiguration
