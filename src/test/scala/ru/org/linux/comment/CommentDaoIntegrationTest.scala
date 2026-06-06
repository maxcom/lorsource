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

import org.junit.Assert.{assertEquals, assertFalse, assertNotNull, assertTrue}
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.scalikejdbc.SpringDB

import javax.sql.DataSource
import java.sql.Timestamp
import java.util.Date

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(
  new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[CommentDaoIntegrationTestConfiguration]))
))
@Transactional
class CommentDaoIntegrationTest {
  @Autowired
  var commentDao: CommentDao = scala.compiletime.uninitialized

  private var jdbcTemplate: JdbcTemplate = scala.compiletime.uninitialized

  @Autowired
  def setDataSource(ds: DataSource): Unit = {
    jdbcTemplate = new JdbcTemplate(ds)
  }

  private var topicId: Int = scala.compiletime.uninitialized

  @Before
  def setUp(): Unit = {
    topicId = jdbcTemplate.queryForObject[Int]("select min (id) from topics").get
  }

  private def addComment(commentId: Int, replyToId: AnyRef, title: String, body: String): Unit = {
    jdbcTemplate.update(
      "INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id) " +
        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, ?::inet, create_user_agent(?))",
      commentId,
      1,
      title,
      replyToId,
      topicId,
      "127.0.0.1",
      "Integration test User Agent"
    )
    jdbcTemplate.update(
      "INSERT INTO msgbase (id, message) VALUES (?, ?)",
      commentId,
      body
    )
  }

  @Test
  def editCommentTest(): Unit = {
    val commentId = jdbcTemplate.queryForObject[Int]("select nextval('s_msgid')").get

    addComment(
      commentId,
      null,
      "CommentDaoIntegrationTest.editCommentTest()",
      "CommentDaoIntegrationTest.editCommentTest(): comment body"
    )

    val oldComment = mock(classOf[Comment])
    when(oldComment.id).thenReturn(commentId)

    commentDao.changeTitle(oldComment, "CommentDaoIntegrationTest.editCommentTest(): new title")

    val rows = jdbcTemplate.queryAndMap(
      "SELECT *, editors.nick as edit_nick FROM comments " +
        " LEFT JOIN users as editors ON comments.editor_id=editors.id " +
        " WHERE comments.id=? ",
      commentId
    ) { (rs, _) =>
      (rs.getString("title"), rs.getString("edit_nick"), rs.getTimestamp("edit_date"), rs.getInt("edit_count"))
    }

    assertFalse("No any records", rows.isEmpty)
    val (title, _, _, _) = rows.head
    assertEquals("CommentDaoIntegrationTest.editCommentTest(): new title", title)
  }

  @Test
  def updateLatestEditorInfoTest(): Unit = {
    val commentId = jdbcTemplate.queryForObject[Int]("select nextval('s_msgid')").get

    addComment(
      commentId,
      null,
      "CommentDaoIntegrationTest.updateLatestEditorInfoTest()",
      "comment body"
    )

    val rowsBefore = jdbcTemplate.queryAndMap(
      "SELECT *, editors.nick as edit_nick FROM comments " +
        " LEFT JOIN users as editors ON comments.editor_id=editors.id " +
        " WHERE comments.id=? ",
      commentId
    ) { (rs, _) =>
      (rs.getString("edit_nick"), rs.getTimestamp("edit_date"), rs.getInt("edit_count"))
    }

    assertFalse("No any records", rowsBefore.isEmpty)
    val (editNickBefore, editDateBefore, editCountBefore) = rowsBefore.head

    assertEquals(null, editNickBefore)
    assertEquals(null, editDateBefore)
    assertEquals(0, editCountBefore)

    val commentEditDate = new Date()
    commentDao.updateLatestEditorInfo(commentId, 1, new Timestamp(commentEditDate.getTime), 1234)

    val rowsAfter = jdbcTemplate.queryAndMap(
      "SELECT *, editors.nick as edit_nick FROM comments " +
        " LEFT JOIN users as editors ON comments.editor_id=editors.id " +
        " WHERE comments.id=? ",
      commentId
    ) { (rs, _) =>
      (rs.getString("edit_nick"), rs.getTimestamp("edit_date"), rs.getInt("edit_count"))
    }

    assertFalse("No any records", rowsAfter.isEmpty)
    val (editNickAfter, editDateAfter, editCountAfter) = rowsAfter.head
    assertEquals("maxcom", editNickAfter)
    assertEquals(editDateAfter.getTime, commentEditDate.getTime)
    assertEquals(1234, editCountAfter)
  }

  @Test
  def isHaveAnswersTest(): Unit = {
    val commentId1 = jdbcTemplate.queryForObject[Int]("select nextval('s_msgid')").get
    val commentId2 = jdbcTemplate.queryForObject[Int]("select nextval('s_msgid')").get

    addComment(
      commentId1,
      null,
      "CommentDaoIntegrationTest.isHaveAnswersTest() - 1",
      "comment body"
    )

    assertEquals(0, commentDao.getRepliesCount(commentId1))

    addComment(
      commentId2,
      commentId1.asInstanceOf[AnyRef],
      "CommentDaoIntegrationTest.isHaveAnswersTest() - 2",
      "comment body"
    )

    assertTrue(commentDao.getRepliesCount(commentId1) > 0)
  }
}

@Configuration
class CommentDaoIntegrationTestConfiguration {
  @Bean
  def commentDao(dataSource: DataSource, transactionManager: PlatformTransactionManager): CommentDao =
    new CommentDao(dataSource, transactionManager)

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao =
    new DeleteInfoDao(springDB)
}