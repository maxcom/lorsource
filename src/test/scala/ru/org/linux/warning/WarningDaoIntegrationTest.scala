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

package ru.org.linux.warning

import org.junit.Assert.*
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import scalikejdbc.*

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[WarningDaoIntegrationTestConfiguration])) @Transactional
class WarningDaoIntegrationTest:

  @Autowired
  var warningDao: WarningDao = scala.compiletime.uninitialized

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

  @Test
  def testPostAndLoadTopicWarning(): Unit =
    val id = springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = None,
      authorId = TestUserId,
      message = "test topic warning",
      warningType = RuleWarning) }
    assertTrue("postWarning should return positive id", id > 0)

    val warnings = warningDao.loadForTopic(testTopicId, forModerator = true)
    val found = warnings.find(_.id == id)
    assertTrue("Warning should be found in loadForTopic", found.isDefined)
    assertEquals(testTopicId, found.get.topicId)
    assertEquals(None, found.get.commentId)
    assertEquals(TestUserId, found.get.authorId)
    assertEquals("test topic warning", found.get.message)
    assertEquals(RuleWarning, found.get.warningType)

  @Test
  def testPostAndLoadCommentWarning(): Unit =
    val commentId = testCommentId
    val id = springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = Some(commentId),
      authorId = TestUserId,
      message = "test comment warning",
      warningType = TagsWarning) }
    assertTrue("postWarning should return positive id", id > 0)

    val loaded = warningDao.loadForComments(Set(commentId))
    assertTrue("Should find warnings for comment", loaded.contains(commentId))
    val found = loaded(commentId).find(_.id == id).get
    assertEquals(Some(commentId), found.commentId)
    assertEquals(TagsWarning, found.warningType)

  @Test
  def testLoadForTopicNonModerator(): Unit =
    val id = springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = None,
      authorId = TestUserId,
      message = "non-moderator test",
      warningType = RuleWarning) }

    val allWarnings = warningDao.loadForTopic(testTopicId, forModerator = true)
    val filteredWarnings = warningDao.loadForTopic(testTopicId, forModerator = false)

    assertTrue("All warnings should include rule type", allWarnings.exists(_.id == id))
    assertTrue("Non-moderator filter should exclude rule type", filteredWarnings.forall(_.warningType != RuleWarning))

  @Test
  def testLoadForCommentsEmptySet(): Unit =
    val result = warningDao.loadForComments(Set.empty)
    assertTrue("Empty set should return empty map", result.isEmpty)

  @Test
  def testLoadForCommentsNonexistentIds(): Unit =
    val result = warningDao.loadForComments(Set(999999))
    assertTrue("Nonexistent comment ids should return empty map", result.isEmpty)

  @Test
  def testGetWarning(): Unit =
    val id = springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = None,
      authorId = TestUserId,
      message = "get test warning",
      warningType = SpellingWarning) }

    val warning = warningDao.get(id)
    assertEquals(id, warning.id)
    assertEquals(testTopicId, warning.topicId)
    assertEquals("get test warning", warning.message)
    assertEquals(SpellingWarning, warning.warningType)

  @Test
  def testClearWarning(): Unit =
    val id = springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = None,
      authorId = TestUserId,
      message = "clear test warning",
      warningType = GroupWarning) }

    val before = warningDao.get(id)
    assertEquals(None, before.closedBy)
    assertEquals(None, before.closedWhen)

    springDB.localTx { warningDao.clear(id, byUserId = 2) }

    val after = warningDao.get(id)
    assertEquals(Some(2), after.closedBy)
    assertTrue("closedWhen should be set", after.closedWhen.isDefined)

  @Test
  def testLastWarningsCount(): Unit =
    val beforeCount = warningDao.lastWarningsCount(TestUserId)

    springDB.localTx { warningDao.postWarning(
      topicId = testTopicId,
      commentId = None,
      authorId = TestUserId,
      message = "count test warning",
      warningType = RuleWarning) }

    val afterCount = warningDao.lastWarningsCount(TestUserId)
    assertEquals(beforeCount + 1, afterCount)

  @Test
  def testAllWarningTypes(): Unit =
    for warningType <- Seq(RuleWarning, TagsWarning, SpellingWarning, GroupWarning) do
      val id = springDB.localTx { warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = s"type test: ${warningType.id}",
        warningType = warningType) }
      val loaded = warningDao.get(id)
      assertEquals(warningType, loaded.warningType)

end WarningDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class WarningDaoIntegrationTestConfiguration:

  @Bean
  def warningDao(springDB: SpringDB): WarningDao = new WarningDao(springDB)

end WarningDaoIntegrationTestConfiguration
