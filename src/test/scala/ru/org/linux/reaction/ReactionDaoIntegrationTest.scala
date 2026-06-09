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

package ru.org.linux.reaction

import org.junit.Assert.*
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.comment.Comment
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import scalikejdbc.*

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[ReactionDaoIntegrationTestConfiguration])) @Transactional
class ReactionDaoIntegrationTest:

  @Autowired
  var reactionDao: ReactionDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testCommentId: Int = scala.compiletime.uninitialized
  private val TestUserId = 1

  @Before
  def setUp(): Unit =
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testCommentId = springDB.run:
      sql"select min(id) from comments where not deleted".map(rs => rs.int(1)).single.apply().get

  private def mockComment(id: Int, topicId: Int): Comment =
    val comment = mock(classOf[Comment])
    when(comment.id).thenReturn(id)
    when(comment.topicId).thenReturn(topicId)
    comment

  private def mockTopic(id: Int): Topic =
    val topic = mock(classOf[Topic])
    when(topic.id).thenReturn(id)
    topic

  private def mockUser(id: Int): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    user

  private def clearReactions(): Unit =
    springDB.run:
      sql"UPDATE comments SET reactions = '{}'::jsonb WHERE id = $testCommentId".update.apply()
      sql"UPDATE topics SET reactions = '{}'::jsonb WHERE id = $testTopicId".update.apply()
      sql"DELETE FROM reactions_log WHERE origin_user = $TestUserId".update.apply()

  @Test
  def testSetCommentReaction(): Unit =
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    val count = springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true) }
    assertEquals(1, count)

    val log = reactionDao.getLogByComment(comment)
    assertTrue("Should have log entries", log.nonEmpty)
    assertEquals(TestUserId, log.head.originUserId)
    assertEquals(testTopicId, log.head.topicId)
    assertEquals(Some(testCommentId), log.head.commentId)
    assertEquals("\uD83D\uDC4D", log.head.reaction)

  @Test
  def testUnsetCommentReaction(): Unit =
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true) }

    val count = springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = false) }
    assertEquals(0, count)

    val log = reactionDao.getLogByComment(comment)
    assertTrue("Should have no log entries after unset", log.isEmpty)

  @Test
  def testSetTopicReaction(): Unit =
    clearReactions()
    val topic = mockTopic(testTopicId)
    val user = mockUser(TestUserId)

    val count = springDB.localTx { reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = true) }
    assertEquals(1, count)

    val log = reactionDao.getLogByTopic(topic)
    assertTrue("Should have log entries", log.nonEmpty)
    assertEquals(TestUserId, log.head.originUserId)
    assertEquals(None, log.head.commentId)

  @Test
  def testUnsetTopicReaction(): Unit =
    clearReactions()
    val topic = mockTopic(testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx { reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = true) }

    val count = springDB.localTx { reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = false) }
    assertEquals(0, count)

    val log = reactionDao.getLogByTopic(topic)
    assertTrue("Should have no log entries after unset", log.isEmpty)

  @Test
  def testRecentReactionCount(): Unit =
    clearReactions()
    val user = mockUser(TestUserId)

    val before = reactionDao.recentReactionCount(user)

    val comment = mockComment(testCommentId, testTopicId)
    springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true) }

    val after = reactionDao.recentReactionCount(user)
    assertEquals(before + 1, after)

  @Test
  def testUpdateReactionOnConflict(): Unit =
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true) }
    val countAfterFirst = springDB.localTx { reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4E", set = true) }
    assertEquals(1, countAfterFirst)

    val log = reactionDao.getLogByComment(comment)
    assertEquals(1, log.size)
    assertEquals("\uD83D\uDC4E", log.head.reaction)

  @Test
  def testGetReactionsViewByUser(): Unit =
    clearReactions()
    val originUser = mockUser(TestUserId)
    val topic = mockTopic(testTopicId)

    springDB.localTx { reactionDao.setTopicReaction(topic, originUser, "\uD83D\uDC4D", set = true) }

    val view = reactionDao.getReactionsView(
      originUser,
      offset = 0,
      size = 10,
      isReactionsOn = false,
      includeDeleted = false)
    assertTrue("Should have reactions view entries", view.nonEmpty)
    assertEquals(testTopicId, view.head.item.topicId)

  @Test
  def testGetLogByTopicEmpty(): Unit =
    val topic = mockTopic(999999)
    val log = reactionDao.getLogByTopic(topic)
    assertTrue("Should be empty for non-existent topic", log.isEmpty)

  @Test
  def testGetLogByCommentEmpty(): Unit =
    val comment = mockComment(999999, 999999)
    val log = reactionDao.getLogByComment(comment)
    assertTrue("Should be empty for non-existent comment", log.isEmpty)

end ReactionDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class ReactionDaoIntegrationTestConfiguration:

  @Bean
  def reactionDao(springDB: SpringDB): ReactionDao = new ReactionDao(springDB)

end ReactionDaoIntegrationTestConfiguration
