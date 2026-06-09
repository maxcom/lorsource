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
@ContextConfiguration(classes = Array(classOf[TopicTagDaoIntegrationTestConfiguration])) @Transactional
class TopicTagDaoIntegrationTest:

  @Autowired
  var topicTagDao: TopicTagDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testTopicId: Int = scala.compiletime.uninitialized
  private var testTagId: Int = scala.compiletime.uninitialized

  @Before
  def setUp(): Unit =
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get
    testTagId = springDB.run:
      sql"select min(id) from tags_values where counter > 0".map(rs => rs.int(1)).single.apply().get

  @Test
  def testAddAndDeleteTag(): Unit =
    val existingTagId = springDB.run:
      sql"""select tv.id from tags_values tv
            where not exists (select 1 from tags where tags.msgid = $testTopicId and tags.tagid = tv.id)
            limit 1""".map(rs => rs.int(1)).single.apply()

    if existingTagId.isDefined then
      val beforeAdd = topicTagDao.getTags(testTopicId)
      assertFalse("Tag should not be linked initially", beforeAdd.exists(_.id == existingTagId.get))

      springDB.localTx { topicTagDao.addTag(testTopicId, existingTagId.get) }
      val afterAdd = topicTagDao.getTags(testTopicId)
      assertTrue("Tag should be added", afterAdd.exists(_.id == existingTagId.get))

      springDB.localTx { topicTagDao.deleteTag(testTopicId, existingTagId.get) }
      val afterDelete = topicTagDao.getTags(testTopicId)
      assertFalse("Tag should be removed", afterDelete.exists(_.id == existingTagId.get))

  @Test
  def testGetTagsForTopic(): Unit =
    val tags = topicTagDao.getTags(testTopicId)
    assertNotNull("Should return tags for topic", tags)
    for tag <- tags do
      assertTrue("Tag name should not be empty", tag.name.nonEmpty)
      assertTrue("Tag id should be positive", tag.id > 0)

  @Test
  def testGetTagSections(): Unit =
    val tagId = springDB.run:
      sql"""select tags.tagid from tags
            join topics on tags.msgid = topics.id
            where not deleted and not draft
            limit 1""".map(rs => rs.int(1)).single.apply()

    if tagId.isDefined then
      val sections = topicTagDao.getTagSections(tagId.get)
      assertTrue("Sections should not be empty for used tag", sections.nonEmpty)
      for section <- sections do
        assertTrue("Section should be valid", section > 0)

  @Test
  def testGetTagsForMultipleTopics(): Unit =
    val topicIds = springDB.run:
      sql"select id from topics where not deleted limit 3".map(rs => rs.int(1)).list.apply()

    if topicIds.size >= 2 then
      val result = topicTagDao.getTags(topicIds)
      assertNotNull("Should return results for multiple topics", result)
      for (topicId, tagInfo) <- result do
        assertTrue("Topic id should be in the input list", topicIds.contains(topicId))
        assertTrue("Tag id should be positive", tagInfo.id > 0)

  @Test
  def testGetTagsForEmptyTopics(): Unit =
    val result = topicTagDao.getTags(Seq.empty)
    assertTrue("Should return empty for empty topics", result.isEmpty)

  @Test
  def testProcessTopicsByTag(): Unit =
    val processedIds = scala.collection.mutable.ListBuffer[Int]()
    topicTagDao.processTopicsByTag(testTagId, id => processedIds += id)

    if processedIds.nonEmpty then
      for id <- processedIds do
        assertTrue("Processed id should be positive", id > 0)

  @Test
  def testIncreaseCounterById(): Unit =
    val counterBefore = springDB.run:
      sql"select counter from tags_values where id=$testTagId".map(rs => rs.int("counter")).single.apply().get

    springDB.localTx { topicTagDao.increaseCounterById(testTagId, 1) }

    val counterAfter = springDB.run:
      sql"select counter from tags_values where id=$testTagId".map(rs => rs.int("counter")).single.apply().get

    assertEquals("Counter should increase by 1", counterBefore + 1, counterAfter)

  @Test
  def testReplaceAndGetCountReplacedTags(): Unit =
    val tagId1 = springDB.run:
      sql"select min(id) from tags_values".map(rs => rs.int(1)).single.apply().get
    val tagId2 = springDB.run:
      sql"select min(id) + 1 from tags_values".map(rs => rs.int(1)).single.apply().get

    if tagId1 != tagId2 then
      val count = topicTagDao.getCountReplacedTags(tagId1, tagId2)
      assertTrue("Count should be non-negative", count >= 0)

  @Test
  def testDeleteTagByTagId(): Unit =
    val existingTagId = springDB.run:
      sql"""select tv.id from tags_values tv
            where not exists (select 1 from tags where tags.msgid = $testTopicId and tags.tagid = tv.id)
            limit 1""".map(rs => rs.int(1)).single.apply()

    if existingTagId.isDefined then
      springDB.localTx { topicTagDao.addTag(testTopicId, existingTagId.get) }
      springDB.localTx { topicTagDao.deleteTag(testTopicId, existingTagId.get) }
      val afterDelete = topicTagDao.getTags(testTopicId)
      assertFalse("Tag should be removed after delete", afterDelete.exists(_.id == existingTagId.get))

end TopicTagDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class TopicTagDaoIntegrationTestConfiguration:

  @Bean
  def topicTagDao(springDB: SpringDB): TopicTagDao = new TopicTagDao(springDB)

end TopicTagDaoIntegrationTestConfiguration
