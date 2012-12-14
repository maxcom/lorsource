/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.topic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class TopicIntegrationTest {
  @Autowired
  TopicDao topicDao;

  @Autowired
  UserDao userDao;

  @Autowired
  TopicListDao topicListDao;

  @Test
  public void deletedTopicForUserNodashiTest() throws Exception {
    User nodashi = userDao.getUser("no-dashi");
    List<TopicListDao.DeletedTopicForUser> nodashiDeleted= topicListDao.getDeletedTopicsForUser(nodashi, 0, 0);

    assertEquals(2, nodashiDeleted.size());
    assertEquals(topicListDao.getCountDeletedTopicsForUser(nodashi), nodashiDeleted.size());

    TopicListDao.DeletedTopicForUser[] nodashiDeletedArray = nodashiDeleted.toArray(new TopicListDao.DeletedTopicForUser[nodashiDeleted.size()]);
    assertEquals(1948635, nodashiDeletedArray[0].getId());
    assertEquals(0, nodashiDeletedArray[0].getBonus());
    assertEquals(1, nodashiDeletedArray[0].getModeratorId());
    assertEquals(Timestamp.valueOf("2010-06-11 15:15:56.692342"), nodashiDeletedArray[0].getDate());

    assertEquals(1934709, nodashiDeletedArray[1].getId());
    assertEquals(Timestamp.valueOf("1970-01-01 00:00:00"), nodashiDeletedArray[1].getDate());
  }

  @Test
  public void deletedCommentForUserWithZeroDeletedTest() throws Exception {
    User user = userDao.getUser("waker");
    assertEquals(0, topicListDao.getCountDeletedTopicsForUser(user));
    assertEquals(0, topicListDao.getDeletedTopicsForUser(user, 0, 0).size());
  }

}
