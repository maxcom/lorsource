/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UserEventDaoIntegrationTestConfiguration.class)
@Transactional
public class UserEventDaoIntegrationTest {
  private static final int TEST_TOPIC_ID = 98075;
  private static final int TEST_USER_ID = 32670;

  @Autowired
  private UserEventDao userEventDao;

  @Autowired
  private UserDao userDao;

  @Test
  public void testAdd() {
    createSimpleEvent();

    List<UserEvent> events = userEventDao.getRepliesForUser(TEST_USER_ID, true, 50, 0, null);

    assertEquals(1, events.size());
  }

  @Test
  public void testInsertTopicUserNotification() {
    userEventDao.insertTopicNotification(TEST_TOPIC_ID, ImmutableList.of(TEST_USER_ID));
  }

  @Test(expected = DuplicateKeyException.class)
  public void testInsertTopicUserNotificationDup() {
    userEventDao.insertTopicNotification(TEST_TOPIC_ID, ImmutableList.of(TEST_USER_ID));
    userEventDao.insertTopicNotification(TEST_TOPIC_ID, ImmutableList.of(TEST_USER_ID));
  }

  private void createSimpleEvent() {
    userEventDao.addEvent(
            UserEventFilterEnum.TAG.toString(),
            TEST_USER_ID,
            false,
            TEST_TOPIC_ID,
            null,
            null
    );
  }

  @Test
  public void testAddRemove() {
    createSimpleEvent();

    List<UserEvent> events = userEventDao.getRepliesForUser(TEST_USER_ID, true, 50, 0, null);

    assertEquals(1, events.size());

    userEventDao.deleteTopicEvents(ImmutableList.of(TEST_TOPIC_ID));

    List<UserEvent> eventsAfterDelete = userEventDao.getRepliesForUser(TEST_USER_ID, true, 50, 0, null);

    assertEquals(0, eventsAfterDelete.size());
  }

  @Test
  public void testRemoveSyntax() {
    userEventDao.deleteTopicEvents(ImmutableList.of(TEST_TOPIC_ID));
  }

  @Test
  public void testRecalc() {
    createSimpleEvent();
    assertEquals(1, userDao.getUser(TEST_USER_ID).getUnreadEvents());
    List<Integer> affected = userEventDao.deleteTopicEvents(ImmutableList.of(TEST_TOPIC_ID));
    assertEquals(1, affected.size());
    assertEquals(1, userDao.getUser(TEST_USER_ID).getUnreadEvents());
    userEventDao.recalcEventCount(ImmutableList.of(TEST_USER_ID));
    assertEquals(0, userDao.getUser(TEST_USER_ID).getUnreadEvents());
  }
}
