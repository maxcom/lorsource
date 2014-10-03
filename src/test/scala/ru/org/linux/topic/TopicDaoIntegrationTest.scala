package ru.org.linux.topic

import org.junit.runner.RunWith
import org.junit.{Assert, Test}
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryService}
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{SectionDao, SectionDaoImpl, SectionService}
import ru.org.linux.spring.dao.{DeleteInfoDao, MsgbaseDao}
import ru.org.linux.topic.TopicDaoIntegrationTest._
import ru.org.linux.user.{UserDao, UserLogDao}
import ru.org.linux.util.bbcode.LorCodeService

@RunWith (classOf[SpringJUnit4ClassRunner] )
@ContextConfiguration (classes = Array (classOf[TopicDaoIntegrationTestConfiguration] ) )
class TopicDaoIntegrationTest {
  @Autowired
  var topicDao : TopicDao = _

  @Test
  def testLoadTopic():Unit = {
    val topic = topicDao.getById(TestTopic)

    Assert.assertNotNull(topic)
    Assert.assertEquals(TestTopic, topic.getId)
  }

  @Test
  def testNextPrev():Unit = {
    val topic = topicDao.getById(TestTopic)

    val nextTopic = topicDao.getNextMessage(topic, null)
    val prevTopic = topicDao.getPreviousMessage(topic, null)

    Assert.assertNotSame(topic.getId, nextTopic.getId)
    Assert.assertNotSame(topic.getId, prevTopic.getId)
  }
}

object TopicDaoIntegrationTest {
  val TestTopic = 1937347
}

@Configuration
@ImportResource (Array ("classpath:database.xml") )
class TopicDaoIntegrationTestConfiguration {
  @Bean
  def groupDao = new GroupDao()

  @Bean
  def sectionService(sectionDao:SectionDao) = new SectionService(sectionDao)

  @Bean
  def sectionDao = new SectionDaoImpl()

  @Bean
  def topicDao = new TopicDao()

  @Bean
  def userDao = new UserDao()

  @Bean
  def userLogDao = Mockito.mock(classOf[UserLogDao])

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
}