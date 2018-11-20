package ru.org.linux.topic;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import ru.org.linux.site.MessageNotFoundException;

/**
 * Created by bvn13 on 18.11.2018.
 * recreated from the same Scala test which is marked as Old and disabled from tests at all
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = TopicIntegrationTestConfiguration.class)
@ImportResource({"classpath:database.xml", "classpath:common.xml"})
public class TopicDoaIntegrationTest {

  private int TestTopic = 1937347;

  @Autowired
  private TopicDao topicDao;


  @Test
  public void testLoadTopic() throws MessageNotFoundException {
    Topic topic = topicDao.getById(TestTopic);

    Assert.assertNotNull(topic);
    Assert.assertEquals(TestTopic, topic.getId());
  }

  @Test
  public void  testNextPrev() throws MessageNotFoundException {
    Topic topic = topicDao.getById(TestTopic);

    Topic nextTopic = topicDao.getNextMessage(topic, null);
    Topic prevTopic = topicDao.getPreviousMessage(topic, null);

    Assert.assertNotSame(topic.getId(), nextTopic.getId());
    Assert.assertNotSame(topic.getId(), prevTopic.getId());
  }
}
