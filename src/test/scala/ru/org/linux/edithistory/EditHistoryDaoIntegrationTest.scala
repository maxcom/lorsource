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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.poll.{Poll, PollVariant}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport

@ContextConfiguration(classes = Array(classOf[EditHistoryDaoIntegrationTestConfiguration]))
class EditHistoryDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var editHistoryDao: EditHistoryDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  test("getEditInfoEmpty"):
    val result = editHistoryDao.getEditInfo(99999, EditHistoryObjectTypeEnum.TOPIC)
    assert(result != null)
    assert(result.isEmpty, "Should be empty for nonexistent topic")

  test("getBriefEditInfoEmpty"):
    val result = editHistoryDao.getBriefEditInfo(99999, EditHistoryObjectTypeEnum.TOPIC)
    assert(result != null)
    assert(result.isEmpty, "Should be empty for nonexistent topic")

  test("insertAndGetEditInfo"):
    val record = EditHistoryRecord(
      msgid = 98075,
      editor = 1,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldmessage = Some("test old message"),
      oldtitle = Some("test old title"))
    springDB.localTx {
      editHistoryDao.insert(record)
    }

    val edits = editHistoryDao.getEditInfo(98075, EditHistoryObjectTypeEnum.TOPIC)
    assert(edits.nonEmpty, "Should have at least one edit")
    val edit = edits.head
    assertEquals(edit.msgid, 98075)
    assertEquals(edit.editor, 1)
    assertEquals(edit.objectType, EditHistoryObjectTypeEnum.TOPIC)
    assertEquals(edit.oldmessage, Some("test old message"))
    assertEquals(edit.oldtitle, Some("test old title"))

  test("insertWithNulls"):
    val record = EditHistoryRecord(msgid = 98076, editor = 1, objectType = EditHistoryObjectTypeEnum.COMMENT)
    springDB.localTx {
      editHistoryDao.insert(record)
    }

    val edits = editHistoryDao.getEditInfo(98076, EditHistoryObjectTypeEnum.COMMENT)
    assert(edits.nonEmpty, "Should have at least one edit")
    val edit = edits.head
    assertEquals(edit.oldmessage, None)
    assertEquals(edit.oldtitle, None)
    assertEquals(edit.oldtags, None)
    assertEquals(edit.oldlinktext, None)
    assertEquals(edit.oldurl, None)
    assertEquals(edit.oldminor, None)

  test("insertWithPollAndAddimages"):
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
    springDB.localTx {
      editHistoryDao.insert(record)
    }

    val edits = editHistoryDao.getEditInfo(98077, EditHistoryObjectTypeEnum.TOPIC)
    assert(edits.nonEmpty, "Should have at least one edit")
    val edit = edits.head
    assertEquals(edit.oldmessage, Some("test with poll"))
    assert(edit.oldPoll.isDefined, "Should have oldPoll")
    assertEquals(edit.oldPoll.get.multiSelect, false)
    assertEquals(edit.oldaddimages, Some(Seq(100, 200, 300)))
    assertEquals(edit.legacyMainImage, None)

  test("insertWithTags"):
    val record = EditHistoryRecord(
      msgid = 98078,
      editor = 1,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldtags = Some(Seq("linux", "kernel")))
    springDB.localTx {
      editHistoryDao.insert(record)
    }

    val edits = editHistoryDao.getEditInfo(98078, EditHistoryObjectTypeEnum.TOPIC)
    assert(edits.nonEmpty, "Should have at least one edit")
    val edit = edits.head
    assertEquals(edit.oldtags, Some(Seq("linux", "kernel")))

end EditHistoryDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class EditHistoryDaoIntegrationTestConfiguration:
  @Bean
  def editHistoryDao(springDB: SpringDB): EditHistoryDao = new EditHistoryDao(springDB)
end EditHistoryDaoIntegrationTestConfiguration
