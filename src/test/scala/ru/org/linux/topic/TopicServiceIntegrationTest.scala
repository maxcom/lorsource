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
package ru.org.linux.topic

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.PekkoConfiguration
import ru.org.linux.group.GroupService
import ru.org.linux.markup.MarkupType
import ru.org.linux.msgbase.{MessageText, MsgbaseDao}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.user.{User, UserService}
import scalikejdbc.*

/** Raw-заголовок топика хранится в БД без типографики; Topic.title (makeTitle) — только для отображения.
  * Заголовок из формы не должен сравниваться с типографированным значением и не должен писаться
  * в БД в типографированном виде (см. TopicService.modifyTopic и TopicDao.saveNewMessage).
  */
@ContextConfiguration(classes = Array(classOf[TopicIntegrationTestConfiguration], classOf[PekkoConfiguration]))
class TopicServiceIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var topicService: TopicService = scala.compiletime.uninitialized

  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var msgbaseDao: MsgbaseDao = scala.compiletime.uninitialized

  @Autowired
  var groupService: GroupService = scala.compiletime.uninitialized

  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private val RawTitle = """Тест заголовка "в кавычках" -- и тире"""

  private var groupId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    groupId = springDB.run:
      sql"select groupid from topics order by id limit 1".map(rs => rs.int(1)).single.apply().get

  private def user: User = userService.getUser("maxcom")

  private def createTopic(title: String): Int =
    val form = new AddTopicRequest(title = title, msg = "текст сообщения")
    form.group = groupService.getGroup(groupId)

    val topic = Topic.fromAddRequest(form, user, "127.0.0.1")

    springDB.localTx:
      val msgid = topicDao.saveNewMessage(topic, user, "test user agent", form.group)
      msgbaseDao.saveNewMessage(MessageText("текст сообщения", MarkupType.Markdown), msgid)
      msgid

  private def oldTitles(msgid: Int): List[String] =
    springDB.run:
      sql"select oldtitle from edit_info where msgid = $msgid and oldtitle is not null"
        .map(rs => rs.string("oldtitle")).list.apply()

  test("topic creation stores raw title without typography"):
    val id = createTopic(RawTitle)

    assertEquals(topicDao.getById(id).rawTitle, RawTitle)

  test("edit without title change keeps raw title in DB and records no title change"):
    val id = createTopic(RawTitle)
    val oldMsg = topicDao.getById(id)
    val form = new EditTopicRequest(msgid = oldMsg, title = RawTitle)
    val newMsg = Topic.fromEditRequest(groupService.getGroup(oldMsg.groupId), oldMsg, form, publish = false)
    val oldText = msgbaseDao.getMessageText(id)
    val newText = MessageText(oldText.text + " (дополнено)", oldText.markup)

    val changed = topicService.updateAndCommit(
      newMsg = newMsg,
      oldMsg = oldMsg,
      user = user,
      newTags = None,
      newText = newText,
      commit = false,
      publish = false,
      changeGroupId = None,
      bonus = 0,
      pollVariants = None,
      multiselect = false,
      editorBonus = Map.empty,
      images = Seq.empty)

    assert(changed, "изменился только текст")
    assertEquals(topicDao.getById(id).rawTitle, RawTitle)
    assertEquals(oldTitles(id), List.empty)

  test("edit stores new title as raw, oldtitle keeps raw"):
    val id = createTopic(RawTitle)
    val oldMsg = topicDao.getById(id)
    val newTitle = """Новый заголовок "проекта" -- v2"""
    val form = new EditTopicRequest(msgid = oldMsg, title = newTitle)
    val newMsg = Topic.fromEditRequest(groupService.getGroup(oldMsg.groupId), oldMsg, form, publish = false)

    val changed = topicService.updateAndCommit(
      newMsg = newMsg,
      oldMsg = oldMsg,
      user = user,
      newTags = None,
      newText = msgbaseDao.getMessageText(id),
      commit = false,
      publish = false,
      changeGroupId = None,
      bonus = 0,
      pollVariants = None,
      multiselect = false,
      editorBonus = Map.empty,
      images = Seq.empty)

    assert(changed, "изменился только заголовок")
    assertEquals(topicDao.getById(id).rawTitle, newTitle)
    assertEquals(oldTitles(id), List(RawTitle))
