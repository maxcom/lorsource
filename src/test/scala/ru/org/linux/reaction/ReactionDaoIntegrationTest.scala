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

import munit.FunSuite
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.comment.Comment
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[ReactionDaoIntegrationTestConfiguration]))
class ReactionDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var reactionDao: ReactionDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testCommentId: Int = scala.compiletime.uninitialized
  private val TestUserId = 1

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
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

  test("setCommentReaction"):
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    val count = springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true)
    }
    assertEquals(count, 1)

    val log = reactionDao.getLogByComment(comment)
    assert(log.nonEmpty, "Should have log entries")
    assertEquals(log.head.originUserId, TestUserId)
    assertEquals(log.head.topicId, testTopicId)
    assertEquals(log.head.commentId, Some(testCommentId))
    assertEquals(log.head.reaction, "\uD83D\uDC4D")

  test("unsetCommentReaction"):
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true)
    }

    val count = springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = false)
    }
    assertEquals(count, 0)

    val log = reactionDao.getLogByComment(comment)
    assert(log.isEmpty, "Should have no log entries after unset")

  test("setTopicReaction"):
    clearReactions()
    val topic = mockTopic(testTopicId)
    val user = mockUser(TestUserId)

    val count = springDB.localTx {
      reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = true)
    }
    assertEquals(count, 1)

    val log = reactionDao.getLogByTopic(topic)
    assert(log.nonEmpty, "Should have log entries")
    assertEquals(log.head.originUserId, TestUserId)
    assertEquals(log.head.commentId, None)

  test("unsetTopicReaction"):
    clearReactions()
    val topic = mockTopic(testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx {
      reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = true)
    }

    val count = springDB.localTx {
      reactionDao.setTopicReaction(topic, user, "\uD83D\uDC4D", set = false)
    }
    assertEquals(count, 0)

    val log = reactionDao.getLogByTopic(topic)
    assert(log.isEmpty, "Should have no log entries after unset")

  test("recentReactionCount"):
    clearReactions()
    val user = mockUser(TestUserId)

    val before = reactionDao.recentReactionCount(user)

    val comment = mockComment(testCommentId, testTopicId)
    springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true)
    }

    val after = reactionDao.recentReactionCount(user)
    assertEquals(after, before + 1)

  test("updateReactionOnConflict"):
    clearReactions()
    val comment = mockComment(testCommentId, testTopicId)
    val user = mockUser(TestUserId)

    springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4D", set = true)
    }
    val countAfterFirst = springDB.localTx {
      reactionDao.setCommentReaction(comment, user, "\uD83D\uDC4E", set = true)
    }
    assertEquals(countAfterFirst, 1)

    val log = reactionDao.getLogByComment(comment)
    assertEquals(log.size, 1)
    assertEquals(log.head.reaction, "\uD83D\uDC4E")

  test("getReactionsViewByUser"):
    clearReactions()
    val originUser = mockUser(TestUserId)
    val topic = mockTopic(testTopicId)

    springDB.localTx {
      reactionDao.setTopicReaction(topic, originUser, "\uD83D\uDC4D", set = true)
    }

    val view = reactionDao.getReactionsView(
      originUser,
      offset = 0,
      size = 10,
      isReactionsOn = false,
      includeDeleted = false)
    assert(view.nonEmpty, "Should have reactions view entries")
    assertEquals(view.head.item.topicId, testTopicId)

  test("getLogByTopicEmpty"):
    val topic = mockTopic(999999)
    val log = reactionDao.getLogByTopic(topic)
    assert(log.isEmpty, "Should be empty for non-existent topic")

  test("getLogByCommentEmpty"):
    val comment = mockComment(999999, 999999)
    val log = reactionDao.getLogByComment(comment)
    assert(log.isEmpty, "Should be empty for non-existent comment")

end ReactionDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class ReactionDaoIntegrationTestConfiguration:

  @Bean
  def reactionDao(springDB: SpringDB): ReactionDao = new ReactionDao(springDB)

end ReactionDaoIntegrationTestConfiguration
