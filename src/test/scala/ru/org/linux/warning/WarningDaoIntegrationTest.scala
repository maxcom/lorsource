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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[WarningDaoIntegrationTestConfiguration]))
class WarningDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var warningDao: WarningDao = scala.compiletime.uninitialized

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

  test("postAndLoadTopicWarning"):
    val id = springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = "test topic warning",
        warningType = RuleWarning)
    }
    assert(id > 0, "postWarning should return positive id")

    val warnings = warningDao.loadForTopic(testTopicId, forModerator = true)
    val found = warnings.find(_.id == id)
    assert(found.isDefined, "Warning should be found in loadForTopic")
    assertEquals(found.get.topicId, testTopicId)
    assertEquals(found.get.commentId, None)
    assertEquals(found.get.authorId, TestUserId)
    assertEquals(found.get.message, "test topic warning")
    assertEquals(found.get.warningType, RuleWarning)

  test("postAndLoadCommentWarning"):
    val commentId = testCommentId
    val id = springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = Some(commentId),
        authorId = TestUserId,
        message = "test comment warning",
        warningType = TagsWarning)
    }
    assert(id > 0, "postWarning should return positive id")

    val loaded = warningDao.loadForComments(Set(commentId))
    assert(loaded.contains(commentId), "Should find warnings for comment")
    val found = loaded(commentId).find(_.id == id).get
    assertEquals(found.commentId, Some(commentId))
    assertEquals(found.warningType, TagsWarning)

  test("loadForTopicNonModerator"):
    val id = springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = "non-moderator test",
        warningType = RuleWarning)
    }

    val allWarnings = warningDao.loadForTopic(testTopicId, forModerator = true)
    val filteredWarnings = warningDao.loadForTopic(testTopicId, forModerator = false)

    assert(allWarnings.exists(_.id == id), "All warnings should include rule type")
    assert(filteredWarnings.forall(_.warningType != RuleWarning), "Non-moderator filter should exclude rule type")

  test("loadForCommentsEmptySet"):
    val result = warningDao.loadForComments(Set.empty)
    assert(result.isEmpty, "Empty set should return empty map")

  test("loadForCommentsNonexistentIds"):
    val result = warningDao.loadForComments(Set(999999))
    assert(result.isEmpty, "Nonexistent comment ids should return empty map")

  test("getWarning"):
    val id = springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = "get test warning",
        warningType = SpellingWarning)
    }

    val warning = warningDao.get(id)
    assertEquals(warning.id, id)
    assertEquals(warning.topicId, testTopicId)
    assertEquals(warning.message, "get test warning")
    assertEquals(warning.warningType, SpellingWarning)

  test("clearWarning"):
    val id = springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = "clear test warning",
        warningType = GroupWarning)
    }

    val before = warningDao.get(id)
    assertEquals(before.closedBy, None)
    assertEquals(before.closedWhen, None)

    springDB.localTx {
      warningDao.clear(id, byUserId = 2)
    }

    val after = warningDao.get(id)
    assertEquals(after.closedBy, Some(2))
    assert(after.closedWhen.isDefined, "closedWhen should be set")

  test("lastWarningsCount"):
    val beforeCount = warningDao.lastWarningsCount(TestUserId)

    springDB.localTx {
      warningDao.postWarning(
        topicId = testTopicId,
        commentId = None,
        authorId = TestUserId,
        message = "count test warning",
        warningType = RuleWarning)
    }

    val afterCount = warningDao.lastWarningsCount(TestUserId)
    assertEquals(afterCount, beforeCount + 1)

  test("allWarningTypes"):
    for warningType <- Seq(RuleWarning, TagsWarning, SpellingWarning, GroupWarning) do
      val id = springDB.localTx {
        warningDao.postWarning(
          topicId = testTopicId,
          commentId = None,
          authorId = TestUserId,
          message = s"type test: ${warningType.id}",
          warningType = warningType)
      }
      val loaded = warningDao.get(id)
      assertEquals(loaded.warningType, warningType)

end WarningDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class WarningDaoIntegrationTestConfiguration:

  @Bean
  def warningDao(springDB: SpringDB): WarningDao = new WarningDao(springDB)

end WarningDaoIntegrationTestConfiguration
