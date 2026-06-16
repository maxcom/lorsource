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

import org.junit.runner.RunWith
import org.junit.{Assert, Test}
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.auth.IpBlockDao
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryService}
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.group.{GroupDao, GroupService}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.msgbase.{DeleteInfoDao, MsgbaseDao, UserAgentDao}
import ru.org.linux.poll.PollDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{SectionDao, SectionDaoImpl, SectionService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDaoIntegrationTest.*
import ru.org.linux.user.{IgnoreListDao, ProfileDao, UserDao, UserInvitesDao, UserLogDao, UserService}
import org.springframework.security.crypto.password.PasswordEncoder
import ru.org.linux.auth.PasswordEncoderImpl
import ru.org.linux.util.bbcode.LorCodeService



@RunWith (classOf[SpringJUnit4ClassRunner])
@ContextConfiguration (classes = Array (classOf[TopicDaoIntegrationTestConfiguration] ) )
class TopicDaoIntegrationTest {
  @Autowired
  var topicDao: TopicDao = scala.compiletime.uninitialized

  @Autowired
  var sectionService: SectionService = scala.compiletime.uninitialized

  @Test
  def testLoadTopic(): Unit = {
    val topic = topicDao.getById(TestTopic)

    Assert.assertNotNull(topic)
    Assert.assertEquals(TestTopic, topic.id)
  }

  @Test
  def testNextPrev():Unit = {
    val topic = topicDao.getById(TestTopic)
    val scrollMode = sectionService.getScrollMode(topic.sectionId)

    val nextTopic = topicDao.getNextMessage(topic, null, scrollMode)
    val prevTopic = topicDao.getPreviousMessage(topic, null, scrollMode)

    Assert.assertTrue(nextTopic.isDefined)
    Assert.assertTrue(prevTopic.isDefined)
    Assert.assertNotSame(topic.id, nextTopic.get.id)
    Assert.assertNotSame(topic.id, prevTopic.get.id)
  }
}

object TopicDaoIntegrationTest {
  val TestTopic = 1937347
}

@Configuration
@ImportResource (Array ("classpath:database.xml", "classpath:common.xml") )
class TopicDaoIntegrationTestConfiguration {
  @Bean
  def passwordEncoder: PasswordEncoder = new PasswordEncoderImpl

  @Bean
  def groupDao(springDB: SpringDB) = new GroupDao(springDB)

  @Bean
  def groupService(groupDao: GroupDao) = new GroupService(groupDao)

  @Bean
  def sectionService(sectionDao: SectionDao) = new SectionService(sectionDao)

  @Bean
  def sectionDao(springDB: SpringDB) = new SectionDaoImpl(springDB)
  
  @Bean
  def topicDao(springDB: SpringDB) = new TopicDao(springDB)

  @Bean
  def userDao(springDB: SpringDB) = new UserDao(springDB)

  @Bean
  def userInvitesDao(springDB: SpringDB) = new UserInvitesDao(springDB)

  @Bean
  def imageDao(sectionService: SectionService, springDB: SpringDB) = new ImageDao(sectionService, springDB)

  @Bean
  def ipBlockDao(springDB: SpringDB) = new IpBlockDao(springDB)

  @Bean
  def imageService = Mockito.mock(classOf[ImageService])

  @Bean
  def ignoreListDao(springDB: SpringDB) = new IgnoreListDao(springDB)

  @Bean
  def profileDao(springDB: SpringDB) = new ProfileDao(springDB)

  @Bean
  def userService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao,
                   userInvitesDao: UserInvitesDao, userLogDao: UserLogDao, userAgentDao: UserAgentDao,
                   springDB: SpringDB, profileDao: ProfileDao, passwordEncoder: PasswordEncoder) =
    new UserService(siteConfig = siteConfig, userDao = userDao, ignoreListDao = ignoreListDao,
      userInvitesDao = userInvitesDao, userLogDao = userLogDao, userAgentDao = userAgentDao, profileDao = profileDao,
      springDB = springDB, passwordEncoder = passwordEncoder)

  @Bean
  def userLogDao = Mockito.mock(classOf[UserLogDao])

  @Bean
  def userAgentDao = Mockito.mock(classOf[UserAgentDao])

  @Bean
  def topicTagService = Mockito.mock(classOf[TopicTagService])

  @Bean
  def msgbaseDao = Mockito.mock(classOf[MsgbaseDao])

  @Bean
  def deleteInfoDao = Mockito.mock(classOf[DeleteInfoDao])

  @Bean
  def editHistoryService = Mockito.mock(classOf[EditHistoryService])

  @Bean
  def editHistoryDao = Mockito.mock(classOf[EditHistoryDao])

  @Bean
  def lorcodeService = Mockito.mock(classOf[LorCodeService])

  @Bean
  def textService = Mockito.mock(classOf[MessageTextService])

  @Bean
  def pollDao = Mockito.mock(classOf[PollDao])
}
