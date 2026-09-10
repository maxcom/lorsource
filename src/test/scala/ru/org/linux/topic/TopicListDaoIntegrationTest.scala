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
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo, NonAuthorizedSession}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.TopicListRequest.CommitMode
import ru.org.linux.user.{Profile, UserDao, UserService}

@ContextConfiguration(classes = Array(classOf[TopicListDaoIntegrationTestConfiguration]))
class TopicListDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var topicListDao: TopicListDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  private lazy val anonymousSession: NonAuthorizedSession =
    NonAuthorizedSession(userDao.getUser(UserService.AnonymousUserId), ipBlockInfo = IpBlockInfo.apply("127.0.0.1"))

  test("getTopicsForumSection"):
    val dto = TopicListRequest(sections = Set(2), commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics.nonEmpty, "Should return topics for forum section")
    assert(topics.size <= 10, "Should return at most 10 topics")
    topics.foreach { topic =>
      assert(!topic.deleted, "Topic should not be deleted")
    }

  test("getTopicsWithAuthorizedSession"):
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
    assert(topics.nonEmpty, "Should return topics for authorized user")

  test("getTopicsAllSections"):
    val dto = TopicListRequest(commitMode = CommitMode.CommittedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")
    assert(topics.size <= 5, "Should return committed topics")

  test("getTopicsUncommitted"):
    val dto = TopicListRequest(commitMode = CommitMode.UncommittedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")

  test("getTopicsByGroup"):
    val dto = TopicListRequest(group = 126, commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")

  test("getUserSections"):
    val user = userDao.getUser(1)
    val sections = topicListDao.getUserSections(user)
    assert(sections.nonEmpty, "maxcom should have sections")
    assert(sections.contains(2), "Sections should include forum (2)")

  test("getTopicsPostmoderatedOnly"):
    val dto = TopicListRequest(sections = Set(2), commitMode = CommitMode.PostmoderatedOnly, limit = Some(5))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")

  test("getTopicsCommittedOnly"):
    val dto = TopicListRequest(sections = Set(1), commitMode = CommitMode.CommittedOnly, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics.nonEmpty, "Should return committed topics in moderated section")
    topics.foreach { topic =>
      assert(topic.commitDate != null, "Committed topic should have commitdate")
    }

  test("getTopicsByUser"):
    val dto = TopicListRequest(userId = 1, commitMode = CommitMode.CommittedAndPostmoderated, limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")
    topics.foreach { topic =>
      assertEquals(topic.authorUserId, 1, "All topics should be by user 1")
    }

  test("getTopicsUserFavorites"):
    val dto = TopicListRequest(
      userId = 1,
      userFavs = true,
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Favorites topics should not be null")
    assert(topics.size <= 10, "Should return at most 10 favorites")

  test("getTopicsUserWatches"):
    val dto = TopicListRequest(
      userId = 1,
      userFavs = true,
      userWatches = true,
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Watched topics should not be null")

  test("getTopicsNotalks"):
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      notalks = true,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")
    topics.foreach { topic =>
      assertNotEquals(topic.groupId, 8404, "Talks group should be excluded")
    }

  test("getTopicsTech"):
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      tech = true,
      limit = Some(10))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")

  test("getTopicsWithOffset"):
    val dto = TopicListRequest(
      sections = Set(2),
      commitMode = CommitMode.CommittedAndPostmoderated,
      limit = Some(5),
      offset = Some(0))

    val topics = topicListDao.getTopics(dto)(using anonymousSession)
    assert(topics != null, "Topics should not be null")
    assert(topics.size <= 5, "Should return at most 5 topics")

  test("getDeletedTopics"):
    val deleted = topicListDao.getDeletedTopics(sectionId = 0, skipBadReason = false)
    assert(deleted != null, "Deleted topics should not be null")

  test("getDeletedTopicsWithSection"):
    val deleted = topicListDao.getDeletedTopics(sectionId = 2, skipBadReason = false)
    assert(deleted != null, "Deleted topics should not be null")

  test("getDeletedTopicsSkipBadReason"):
    val deleted = topicListDao.getDeletedTopics(sectionId = 0, skipBadReason = true)
    assert(deleted != null, "Deleted topics should not be null")

  test("getDeletedUserTopics"):
    val user = userDao.getUser(1)
    val deleted = topicListDao.getDeletedUserTopics(user, topics = 10)
    assert(deleted != null, "Deleted user topics should not be null")
    assert(deleted.size <= 10, "Should return at most 10 topics")

end TopicListDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class TopicListDaoIntegrationTestConfiguration:

  @Bean
  def topicListDao(springDB: SpringDB): TopicListDao = new TopicListDao(springDB)

  @Bean
  def userDao(springDB: SpringDB): UserDao = new UserDao(springDB)

end TopicListDaoIntegrationTestConfiguration
