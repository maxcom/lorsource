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

package ru.org.linux.msgbase

import munit.FunSuite
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.user.User
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[DeleteInfoDaoIntegrationTestConfiguration]))
class DeleteInfoDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var deleteInfoDao: DeleteInfoDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testCommentId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testCommentId = springDB.run:
      sql"select min(id) from comments where not deleted".map(rs => rs.int(1)).single.apply().get

  private def mockUser(id: Int): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    user

  test("insertAndGetDeleteInfo"):
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "test delete reason", -5, deleter)

    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val result = deleteInfoDao.getDeleteInfo(testTopicId)
    assert(result.isDefined, "Should find delete info")
    assertEquals(result.get.userid, 1)
    assertEquals(result.get.reason, "test delete reason")
    assertEquals(result.get.bonus, Some(-5))

  test("insertWithZeroBonus"):
    val deleter = mockUser(2)
    val info = InsertDeleteInfo(testCommentId, "zero bonus delete", 0, deleter)

    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val result = deleteInfoDao.getDeleteInfo(testCommentId)
    assert(result.isDefined, "Should find delete info")
    assertEquals(result.get.bonus.getOrElse(0), 0)

  test("getDeleteInfoNotFound"):
    val result = deleteInfoDao.getDeleteInfo(999999)
    assert(result.isEmpty, "Should not find delete info for non-existent id")

  test("deleteDeleteInfo"):
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "to be deleted", -2, deleter)
    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val before = deleteInfoDao.getDeleteInfo(testTopicId)
    assert(before.isDefined, "Should exist before delete")

    springDB.localTx {
      deleteInfoDao.delete(testTopicId)
    }

    val after = deleteInfoDao.getDeleteInfo(testTopicId)
    assert(after.isEmpty, "Should not exist after delete")

  test("batchInsertDeleteInfo"):
    val deleter = mockUser(1)
    val commentId2 = springDB.run:
      sql"select min(id) + 1 from comments where not deleted".map(rs => rs.int(1)).single.apply().get

    val infos = Seq(
      InsertDeleteInfo(testCommentId, "batch delete 1", -3, deleter),
      InsertDeleteInfo(commentId2, "batch delete 2", -1, deleter))

    springDB.localTx {
      deleteInfoDao.insert(infos)
    }

    val result1 = deleteInfoDao.getDeleteInfo(testCommentId)
    assert(result1.isDefined, "Should find first batch delete info")
    assertEquals(result1.get.reason, "batch delete 1")

    val result2 = deleteInfoDao.getDeleteInfo(commentId2)
    assert(result2.isDefined, "Should find second batch delete info")
    assertEquals(result2.get.reason, "batch delete 2")

  test("scoreLoss"):
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "score loss test", -10, deleter)
    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val loss = deleteInfoDao.scoreLoss(testTopicId)
    assert(loss >= 0, "Score loss should be non-negative")

  test("getRecentScoreLoss"):
    val deleter = mockUser(1)
    val user = mockUser(1)

    val before = deleteInfoDao.getRecentScoreLoss(user)
    assert(before >= 0, "Score loss should be non-negative")

    val info = InsertDeleteInfo(testTopicId, "recent score loss", -7, deleter)
    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val after = deleteInfoDao.getRecentScoreLoss(user)
    assertEquals(after, before + 7, "Score loss should increase after insertion")

  test("getDeleteInfoForUpdate"):
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "for update test", -1, deleter)
    springDB.localTx {
      deleteInfoDao.insert(info)
    }

    val result = deleteInfoDao.getDeleteInfo(testTopicId, forUpdate = true)
    assert(result.isDefined, "Should find delete info with FOR UPDATE")
    assertEquals(result.get.reason, "for update test")

end DeleteInfoDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeleteInfoDaoIntegrationTestConfiguration:

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = new DeleteInfoDao(springDB)

end DeleteInfoDaoIntegrationTestConfiguration
