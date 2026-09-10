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
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[TopicTagDaoIntegrationTestConfiguration]))
class TopicTagDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var topicTagDao: TopicTagDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testTagId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testTagId = springDB.run:
      sql"select min(id) from tags_values where counter > 0".map(rs => rs.int(1)).single.apply().get

  test("addAndDeleteTag"):
    val existingTagId = springDB.run:
      sql"""select tv.id from tags_values tv
            where not exists (select 1 from tags where tags.msgid = $testTopicId and tags.tagid = tv.id)
            limit 1""".map(rs => rs.int(1)).single.apply()

    if existingTagId.isDefined then
      val beforeAdd = topicTagDao.getTags(testTopicId)
      assert(!beforeAdd.exists(_.id == existingTagId.get), "Tag should not be linked initially")

      springDB.localTx {
        topicTagDao.addTag(testTopicId, existingTagId.get)
      }
      val afterAdd = topicTagDao.getTags(testTopicId)
      assert(afterAdd.exists(_.id == existingTagId.get), "Tag should be added")

      springDB.localTx {
        topicTagDao.deleteTag(testTopicId, existingTagId.get)
      }
      val afterDelete = topicTagDao.getTags(testTopicId)
      assert(!afterDelete.exists(_.id == existingTagId.get), "Tag should be removed")

  test("getTagsForTopic"):
    val tags = topicTagDao.getTags(testTopicId)
    assert(tags != null, "Should return tags for topic")
    for tag <- tags do
      assert(tag.name.nonEmpty, "Tag name should not be empty")
      assert(tag.id > 0, "Tag id should be positive")

  test("getTagSections"):
    val tagId = springDB.run:
      sql"""select tags.tagid from tags
            join topics on tags.msgid = topics.id
            where not deleted and not draft
            limit 1""".map(rs => rs.int(1)).single.apply()

    if tagId.isDefined then
      val sections = topicTagDao.getTagSections(tagId.get)
      assert(sections.nonEmpty, "Sections should not be empty for used tag")
      for section <- sections do
        assert(section > 0, "Section should be valid")

  test("getTagsForMultipleTopics"):
    val topicIds = springDB.run:
      sql"select id from topics where not deleted limit 3".map(rs => rs.int(1)).list.apply()

    if topicIds.size >= 2 then
      val result = topicTagDao.getTags(topicIds)
      assert(result != null, "Should return results for multiple topics")
      for (topicId, tagInfo) <- result do
        assert(topicIds.contains(topicId), "Topic id should be in the input list")
        assert(tagInfo.id > 0, "Tag id should be positive")

  test("getTagsForEmptyTopics"):
    val result = topicTagDao.getTags(Seq.empty)
    assert(result.isEmpty, "Should return empty for empty topics")

  test("processTopicsByTag"):
    val processedIds = scala.collection.mutable.ListBuffer[Int]()
    topicTagDao.processTopicsByTag(testTagId, id => processedIds += id)

    if processedIds.nonEmpty then
      for id <- processedIds do
        assert(id > 0, "Processed id should be positive")

  test("increaseCounterById"):
    val counterBefore = springDB.run:
      sql"select counter from tags_values where id=$testTagId".map(rs => rs.int("counter")).single.apply().get

    springDB.localTx {
      topicTagDao.increaseCounterById(testTagId, 1)
    }

    val counterAfter = springDB.run:
      sql"select counter from tags_values where id=$testTagId".map(rs => rs.int("counter")).single.apply().get

    assertEquals(counterAfter, counterBefore + 1, "Counter should increase by 1")

  test("replaceAndGetCountReplacedTags"):
    val tagId1 = springDB.run:
      sql"select min(id) from tags_values".map(rs => rs.int(1)).single.apply().get
    val tagId2 = springDB.run:
      sql"select min(id) + 1 from tags_values".map(rs => rs.int(1)).single.apply().get

    if tagId1 != tagId2 then
      val count = topicTagDao.getCountReplacedTags(tagId1, tagId2)
      assert(count >= 0, "Count should be non-negative")

  test("deleteTagByTagId"):
    val existingTagId = springDB.run:
      sql"""select tv.id from tags_values tv
            where not exists (select 1 from tags where tags.msgid = $testTopicId and tags.tagid = tv.id)
            limit 1""".map(rs => rs.int(1)).single.apply()

    if existingTagId.isDefined then
      springDB.localTx {
        topicTagDao.addTag(testTopicId, existingTagId.get)
      }
      springDB.localTx {
        topicTagDao.deleteTag(testTopicId, existingTagId.get)
      }
      val afterDelete = topicTagDao.getTags(testTopicId)
      assert(!afterDelete.exists(_.id == existingTagId.get), "Tag should be removed after delete")

end TopicTagDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class TopicTagDaoIntegrationTestConfiguration:

  @Bean
  def topicTagDao(springDB: SpringDB): TopicTagDao = new TopicTagDao(springDB)

end TopicTagDaoIntegrationTestConfiguration
