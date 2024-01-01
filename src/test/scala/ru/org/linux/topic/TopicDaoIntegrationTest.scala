/*
 * Copyright 1998-2024 Linux.org.ru
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
import ru.org.linux.auth.IPBlockDao
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryService}
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.group.GroupDao
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.PollDao
import ru.org.linux.section.{SectionDao, SectionDaoImpl, SectionService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.{DeleteInfoDao, MsgbaseDao, UserAgentDao}
import ru.org.linux.topic.TopicDaoIntegrationTest.*
import ru.org.linux.user.{IgnoreListDao, UserDao, UserInvitesDao, UserLogDao, UserService}
import ru.org.linux.util.bbcode.LorCodeService

import javax.sql.DataSource

@RunWith (classOf[SpringJUnit4ClassRunner])
@ContextConfiguration (classes = Array (classOf[TopicDaoIntegrationTestConfiguration] ) )
class TopicDaoIntegrationTest {
  @Autowired
  var topicDao: TopicDao = _

  @Test
  def testLoadTopic(): Unit = {
    val topic = topicDao.getById(TestTopic)

    Assert.assertNotNull(topic)
    Assert.assertEquals(TestTopic, topic.id)
  }

  @Test
  def testNextPrev():Unit = {
    val topic = topicDao.getById(TestTopic)

    val nextTopic = topicDao.getNextMessage(topic, null)
    val prevTopic = topicDao.getPreviousMessage(topic, null)

    Assert.assertNotSame(topic.id, nextTopic.id)
    Assert.assertNotSame(topic.id, prevTopic.id)
  }
}

object TopicDaoIntegrationTest {
  val TestTopic = 1937347
}

@Configuration
@ImportResource (Array ("classpath:database.xml", "classpath:common.xml") )
class TopicDaoIntegrationTestConfiguration {
  @Bean
  def groupDao = new GroupDao()

  @Bean
  def sectionService(sectionDao: SectionDao) = new SectionService(sectionDao)

  @Bean
  def sectionDao(ds: DataSource) = new SectionDaoImpl(ds)

  @Bean
  def topicDao = new TopicDao()

  @Bean
  def userDao(userLogDao: UserLogDao, dataSource: DataSource) = new UserDao(userLogDao, dataSource)

  @Bean
  def userInvitesDao(ds: DataSource) = new UserInvitesDao(ds)

  @Bean
  def imageDao = new ImageDao()

  @Bean
  def ipBlockDao = new IPBlockDao()

  @Bean
  def imageService = Mockito.mock(classOf[ImageService])

  @Bean
  def ignoreListDao(ds: DataSource) = new IgnoreListDao(ds)

  @Bean
  def userService(siteConfig: SiteConfig, userDao: UserDao, ignoreListDao: IgnoreListDao,
                  userInvitesDao: UserInvitesDao, userLogDao: UserLogDao, deleteInfoDao: DeleteInfoDao,
                  IPBlockDao: IPBlockDao, userAgentDao: UserAgentDao, transactionManager: PlatformTransactionManager) =
    new UserService(siteConfig, userDao, ignoreListDao, userInvitesDao, userLogDao, deleteInfoDao, IPBlockDao,
      userAgentDao, transactionManager)

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
