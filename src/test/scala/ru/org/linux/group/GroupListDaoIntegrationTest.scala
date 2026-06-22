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
package ru.org.linux.group

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.auth.{AuthorizedSession, NonAuthorizedSession}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.Section
import ru.org.linux.tracker.TrackerFilterEnum
import ru.org.linux.user.{Profile, UserDao, UserService}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[GroupListDaoIntegrationTestConfiguration])) @Transactional
class GroupListDaoIntegrationTest:

  @Autowired
  var groupListDao: GroupListDao = scala.compiletime.uninitialized

  @Autowired
  var userDao: UserDao = scala.compiletime.uninitialized

  private lazy val anonymousSession: NonAuthorizedSession =
    NonAuthorizedSession(userDao.getUser(UserService.AnonymousUserId))

  @Autowired
  var groupDao: GroupDao = scala.compiletime.uninitialized

  @Test
  def testGetGroupTrackerTopicsNonAuthorized(): Unit =
    val topics = groupListDao.getGroupTrackerTopics(126, 0, None)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetGroupTrackerTopicsAuthorized(): Unit =
    val user = userDao.getUser(1)
    val session = AuthorizedSession(
      user,
      corrector = false,
      moderator = false,
      administrator = false,
      profile = Profile.DEFAULT)
    val topics = groupListDao.getGroupTrackerTopics(126, 0, None)(using session)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetGroupListTopicsNonAuthorized(): Unit =
    val topics =
      groupListDao.getGroupListTopics(126, 0, showIgnored = false, showDeleted = false, yearMonth = None, tagId = None)(
        using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetGroupListTopicsWithYearMonth(): Unit =
    val topics =
      groupListDao.getGroupListTopics(
        126,
        0,
        showIgnored = false,
        showDeleted = false,
        yearMonth = Some((2025, 1)),
        tagId = None)(using anonymousSession)
    assertNotNull("Topics should not be null", topics)

  @Test
  def testGetGroupStickyTopicsNonAuthorized(): Unit =
    val group = groupDao.getGroup(126)
    val topics = groupListDao.getGroupStickyTopics(group, None)(using anonymousSession)
    assertNotNull("Sticky topics should not be null", topics)

  @Test
  def testGetTrackerTopicsAll(): Unit =
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.ALL, 0)(using anonymousSession)
    assertNotNull("Tracker topics should not be null", topics)

  @Test
  def testGetTrackerTopicsMain(): Unit =
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.MAIN, 0)(using anonymousSession)
    assertNotNull("Tracker topics should not be null", topics)

  @Test
  def testGetTrackerTopicsNotalks(): Unit =
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.NOTALKS, 0)(using anonymousSession)
    assertNotNull("Tracker topics should not be null", topics)

  @Test
  def testGetTrackerTopicsTech(): Unit =
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.TECH, 0)(using anonymousSession)
    assertNotNull("Tracker topics should not be null", topics)

  @Test
  def testGetTrackerTopicsAuthorized(): Unit =
    val user = userDao.getUser(1)
    val session = AuthorizedSession(
      user,
      corrector = false,
      moderator = false,
      administrator = false,
      profile = Profile.DEFAULT)
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.MAIN, 0)(using session)
    assertNotNull("Tracker topics should not be null", topics)

  @Test
  def testTrackerTopicsResultsStructure(): Unit =
    val topics = groupListDao.getTrackerTopics(TrackerFilterEnum.ALL, 0)(using anonymousSession)
    topics.foreach { topic =>
      assertNotEquals("Topic ID should not be 0", 0, topic.topicId)
      assertNotEquals("Topic author should not be 0", 0, topic.topicAuthor)
      assertNotNull("Group title should not be null", topic.groupTitle)
      assertNotNull("Group URL name should not be null", topic.groupUrlName)
      assertTrue("Section should be positive", topic.section > 0)
    }

end GroupListDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class GroupListDaoIntegrationTestConfiguration:

  @Bean
  def groupListDao(springDB: SpringDB): GroupListDao = new GroupListDao(springDB)

  @Bean
  def groupDao(springDB: SpringDB): GroupDao = new GroupDao(springDB)

  @Bean
  def userDao(springDB: SpringDB): UserDao = new UserDao(springDB)

end GroupListDaoIntegrationTestConfiguration
