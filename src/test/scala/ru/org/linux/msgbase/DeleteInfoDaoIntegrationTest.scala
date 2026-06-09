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

import org.junit.Assert.*
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.user.User
import scalikejdbc.*

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[DeleteInfoDaoIntegrationTestConfiguration])) @Transactional
class DeleteInfoDaoIntegrationTest:

  @Autowired
  var deleteInfoDao: DeleteInfoDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testCommentId: Int = scala.compiletime.uninitialized

  @Before
  def setUp(): Unit =
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testCommentId = springDB.run:
      sql"select min(id) from comments where not deleted".map(rs => rs.int(1)).single.apply().get

  private def mockUser(id: Int): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    user

  @Test
  def testInsertAndGetDeleteInfo(): Unit =
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "test delete reason", -5, deleter)

    springDB.localTx { deleteInfoDao.insert(info) }

    val result = deleteInfoDao.getDeleteInfo(testTopicId)
    assertTrue("Should find delete info", result.isDefined)
    assertEquals(1, result.get.userid)
    assertEquals("test delete reason", result.get.reason)
    assertEquals(Some(-5), result.get.bonus)

  @Test
  def testInsertWithZeroBonus(): Unit =
    val deleter = mockUser(2)
    val info = InsertDeleteInfo(testCommentId, "zero bonus delete", 0, deleter)

    springDB.localTx { deleteInfoDao.insert(info) }

    val result = deleteInfoDao.getDeleteInfo(testCommentId)
    assertTrue("Should find delete info", result.isDefined)
    assertEquals(0, result.get.bonus.getOrElse(0))

  @Test
  def testGetDeleteInfoNotFound(): Unit =
    val result = deleteInfoDao.getDeleteInfo(999999)
    assertTrue("Should not find delete info for non-existent id", result.isEmpty)

  @Test
  def testDeleteDeleteInfo(): Unit =
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "to be deleted", -2, deleter)
    springDB.localTx { deleteInfoDao.insert(info) }

    val before = deleteInfoDao.getDeleteInfo(testTopicId)
    assertTrue("Should exist before delete", before.isDefined)

    springDB.localTx { deleteInfoDao.delete(testTopicId) }

    val after = deleteInfoDao.getDeleteInfo(testTopicId)
    assertTrue("Should not exist after delete", after.isEmpty)

  @Test
  def testBatchInsertDeleteInfo(): Unit =
    val deleter = mockUser(1)
    val commentId2 = springDB.run:
      sql"select min(id) + 1 from comments where not deleted".map(rs => rs.int(1)).single.apply().get

    val infos = Seq(
      InsertDeleteInfo(testCommentId, "batch delete 1", -3, deleter),
      InsertDeleteInfo(commentId2, "batch delete 2", -1, deleter))

    springDB.localTx { deleteInfoDao.insert(infos) }

    val result1 = deleteInfoDao.getDeleteInfo(testCommentId)
    assertTrue("Should find first batch delete info", result1.isDefined)
    assertEquals("batch delete 1", result1.get.reason)

    val result2 = deleteInfoDao.getDeleteInfo(commentId2)
    assertTrue("Should find second batch delete info", result2.isDefined)
    assertEquals("batch delete 2", result2.get.reason)

  @Test
  def testScoreLoss(): Unit =
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "score loss test", -10, deleter)
    springDB.localTx { deleteInfoDao.insert(info) }

    val loss = deleteInfoDao.scoreLoss(testTopicId)
    assertTrue("Score loss should be non-negative", loss >= 0)

  @Test
  def testGetRecentScoreLoss(): Unit =
    val deleter = mockUser(1)
    val user = mockUser(1)

    val before = deleteInfoDao.getRecentScoreLoss(user)
    assertTrue("Score loss should be non-negative", before >= 0)

    val info = InsertDeleteInfo(testTopicId, "recent score loss", -7, deleter)
    springDB.localTx { deleteInfoDao.insert(info) }

    val after = deleteInfoDao.getRecentScoreLoss(user)
    assertEquals("Score loss should increase after insertion", before + 7, after)

  @Test
  def testGetDeleteInfoForUpdate(): Unit =
    val deleter = mockUser(1)
    val info = InsertDeleteInfo(testTopicId, "for update test", -1, deleter)
    springDB.localTx { deleteInfoDao.insert(info) }

    val result = deleteInfoDao.getDeleteInfo(testTopicId, forUpdate = true)
    assertTrue("Should find delete info with FOR UPDATE", result.isDefined)
    assertEquals("for update test", result.get.reason)

end DeleteInfoDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class DeleteInfoDaoIntegrationTestConfiguration:

  @Bean
  def deleteInfoDao(springDB: SpringDB): DeleteInfoDao = new DeleteInfoDao(springDB)

end DeleteInfoDaoIntegrationTestConfiguration
