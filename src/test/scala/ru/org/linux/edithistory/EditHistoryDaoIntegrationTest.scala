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
package ru.org.linux.edithistory

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.poll.{Poll, PollVariant}
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[EditHistoryDaoIntegrationTestConfiguration])) @Transactional
class EditHistoryDaoIntegrationTest:

  @Autowired
  var editHistoryDao: EditHistoryDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  @Test
  def testGetEditInfoEmpty(): Unit =
    val result = editHistoryDao.getEditInfo(99999, EditHistoryObjectTypeEnum.TOPIC)
    assertNotNull(result)
    assertTrue("Should be empty for nonexistent topic", result.isEmpty)

  @Test
  def testGetBriefEditInfoEmpty(): Unit =
    val result = editHistoryDao.getBriefEditInfo(99999, EditHistoryObjectTypeEnum.TOPIC)
    assertNotNull(result)
    assertTrue("Should be empty for nonexistent topic", result.isEmpty)

  @Test
  def testInsertAndGetEditInfo(): Unit =
    val record = EditHistoryRecord(
      msgid = 98075,
      editor = 1,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldmessage = Some("test old message"),
      oldtitle = Some("test old title"))
    springDB.localTx { editHistoryDao.insert(record) }

    val edits = editHistoryDao.getEditInfo(98075, EditHistoryObjectTypeEnum.TOPIC)
    assertTrue("Should have at least one edit", edits.nonEmpty)
    val edit = edits.head
    assertEquals(98075, edit.msgid)
    assertEquals(1, edit.editor)
    assertEquals(EditHistoryObjectTypeEnum.TOPIC, edit.objectType)
    assertEquals(Some("test old message"), edit.oldmessage)
    assertEquals(Some("test old title"), edit.oldtitle)

  @Test
  def testInsertWithNulls(): Unit =
    val record = EditHistoryRecord(msgid = 98076, editor = 1, objectType = EditHistoryObjectTypeEnum.COMMENT)
    springDB.localTx { editHistoryDao.insert(record) }

    val edits = editHistoryDao.getEditInfo(98076, EditHistoryObjectTypeEnum.COMMENT)
    assertTrue("Should have at least one edit", edits.nonEmpty)
    val edit = edits.head
    assertEquals(None, edit.oldmessage)
    assertEquals(None, edit.oldtitle)
    assertEquals(None, edit.oldtags)
    assertEquals(None, edit.oldlinktext)
    assertEquals(None, edit.oldurl)
    assertEquals(None, edit.oldminor)
    assertEquals(None, edit.oldimage)

  @Test
  def testInsertWithPollAndAddimages(): Unit =
    val poll = Poll(
      id = 1,
      topic = 98077,
      multiSelect = false,
      variants = Seq(PollVariant(id = 1, label = "Yes"), PollVariant(id = 2, label = "No")))
    val record = EditHistoryRecord(
      msgid = 98077,
      editor = 1,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldmessage = Some("test with poll"),
      oldPoll = Some(poll),
      oldaddimages = Some(Seq(100, 200, 300))
    )
    springDB.localTx { editHistoryDao.insert(record) }

    val edits = editHistoryDao.getEditInfo(98077, EditHistoryObjectTypeEnum.TOPIC)
    assertTrue("Should have at least one edit", edits.nonEmpty)
    val edit = edits.head
    assertEquals(Some("test with poll"), edit.oldmessage)
    assertTrue("Should have oldPoll", edit.oldPoll.isDefined)
    assertEquals(false, edit.oldPoll.get.multiSelect)
    assertEquals(Some(Seq(100, 200, 300)), edit.oldaddimages)

  @Test
  def testInsertWithTags(): Unit =
    val record = EditHistoryRecord(
      msgid = 98078,
      editor = 1,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldtags = Some(Seq("linux", "kernel"))    )
    springDB.localTx { editHistoryDao.insert(record) }

    val edits = editHistoryDao.getEditInfo(98078, EditHistoryObjectTypeEnum.TOPIC)
    assertTrue("Should have at least one edit", edits.nonEmpty)
    val edit = edits.head
    assertEquals(Some(Seq("linux", "kernel")), edit.oldtags)

end EditHistoryDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class EditHistoryDaoIntegrationTestConfiguration:
  @Bean
  def editHistoryDao(springDB: SpringDB): EditHistoryDao = new EditHistoryDao(springDB)
end EditHistoryDaoIntegrationTestConfiguration
