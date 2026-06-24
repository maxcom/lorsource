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
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo, NonAuthorizedSession}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.TopicListRequest.CommitMode
import ru.org.linux.user.{Profile, UserDao, UserService}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[TopicListDaoIntegrationTestConfiguration])) @Transactional
class TopicListDaoIntegrationTest:

  @Autowired
  var topicListDao: TopicListDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  private lazy val anonymousSession: NonAuthorizedSession =
    NonAuthorizedSession(userDao.getUser(UserService.AnonymousUserId), ipBlockInfo = IpBlockInfo.apply("127.0.0.1"))

  @Test
  def testGetTopicsForumSection(): Unit =
    val dto = TopicListRequest(sections = Set(2), commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertTrue("Should return topics for forum section", topics.nonEmpty)
    assertTrue("Should return at most 10 topics", topics.size <= 10)
    topics.foreach { topic =>
      assertFalse("Topic should not be deleted", topic.deleted)
    }

  @Test
  def testGetTopicsWithAuthorizedSession(): Unit =
    val user = userDao.getUser(1)
    given AuthorizedSession(
      user,
      corrector = false,
      moderator = false,
      administrator = false,
      profile = Profile.DEFAULT,
      ipBlockInfo = IpBlockInfo("127.0.0.1"))
    val dto = TopicListRequest(sections = Set(2), commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)
    assertTrue("Should return topics for authorized user", topics.nonEmpty)

  @Test
  def testGetTopicsAllSections(): Unit =
    val dto = TopicListRequest(commitMode = CommitMode.CommittedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)
    assertTrue("Should return committed topics", topics.size <= 5)

  @Test
  def testGetTopicsUncommitted(): Unit =
    val dto = TopicListRequest(commitMode = CommitMode.UncommittedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetTopicsByGroup(): Unit =
    val dto = TopicListRequest(group = 126, commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetUserSections(): Unit =
    val user = userDao.getUser(1)
    val sections = topicListDao.getUserSections(user)
    assertTrue("maxcom should have sections", sections.nonEmpty)
    assertTrue("Sections should include forum (2)", sections.contains(2))

  @Test
  def testGetTopicsPostmoderatedOnly(): Unit =
    val dto = TopicListRequest(sections = Set(2), commitMode = CommitMode.PostmoderatedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetTopicsCommittedOnly(): Unit =
    val dto = TopicListRequest(sections = Set(1), commitMode = CommitMode.CommittedOnly, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertTrue("Should return committed topics in moderated section", topics.nonEmpty)
    topics.foreach { topic =>
      assertNotNull("Committed topic should have commitdate", topic.commitDate)
    }

  @Test
  def testGetTopicsByUser(): Unit =
    val dto = TopicListRequest(userId = 1, commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)
    topics.foreach { topic =>
      assertEquals("All topics should be by user 1", 1, topic.authorUserId)
    }

  @Test
  def testGetTopicsUserFavorites(): Unit =
    val dto = TopicListRequest(
      userId = 1,
      userFavs = true,
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Favorites topics should not be null", topics)
    assertTrue("Should return at most 10 favorites", topics.size <= 10)

  @Test
  def testGetTopicsUserWatches(): Unit =
    val dto = TopicListRequest(
      userId = 1,
      userFavs = true,
      userWatches = true,
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Watched topics should not be null", topics)

  @Test
  def testGetTopicsNotalks(): Unit =
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      notalks = true,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)
    topics.foreach { topic =>
      assertNotEquals("Talks group should be excluded", 8404, topic.groupId)
    }

  @Test
  def testGetTopicsTech(): Unit =
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      tech = true,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetTopicsWithOffset(): Unit =
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(5),
      offset = Some(0))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)
    assertTrue("Should return at most 5 topics", topics.size <= 5)

  @Test
  def testGetDeletedTopics(): Unit =
    val deleted = topicListDao.getDeletedTopics(sectionId = 0, skipBadReason = false)
    assertNotNull("Deleted topics should not be null", deleted)

  @Test
  def testGetDeletedTopicsWithSection(): Unit =
    val deleted = topicListDao.getDeletedTopics(sectionId = 2, skipBadReason = false)
    assertNotNull("Deleted topics should not be null", deleted)

  @Test
  def testGetDeletedTopicsSkipBadReason(): Unit =
    val deleted = topicListDao.getDeletedTopics(sectionId = 0, skipBadReason = true)
    assertNotNull("Deleted topics should not be null", deleted)

  @Test
  def testGetDeletedUserTopics(): Unit =
    val user = userDao.getUser(1)
    val deleted = topicListDao.getDeletedUserTopics(user, topics = 10)
    assertNotNull("Deleted user topics should not be null", deleted)
    assertTrue("Should return at most 10 topics", deleted.size <= 10)

end TopicListDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class TopicListDaoIntegrationTestConfiguration:

  @Bean
  def topicListDao(springDB: SpringDB): TopicListDao = new TopicListDao(springDB)

  @Bean
  def userDao(springDB: SpringDB): UserDao = new UserDao(springDB)

end TopicListDaoIntegrationTestConfiguration
