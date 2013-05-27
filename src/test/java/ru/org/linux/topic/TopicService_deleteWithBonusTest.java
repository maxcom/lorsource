/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserEventService;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TopicService_deleteWithBonusTest {
  @MockitoAnnotations.Mock
  private UserDao userDao;

  @MockitoAnnotations.Mock
  private TopicDao topicDao;

  @MockitoAnnotations.Mock
  private DeleteInfoDao deleteInfoDao;

  @MockitoAnnotations.Mock
  private UserEventService userEventService;

  @MockitoAnnotations.Mock
  private User user;

  @MockitoAnnotations.Mock
  private Topic message;

  TopicService topicService;

  @Before
  public void setUp() {
    topicService = new TopicService(
            topicDao, null, null, null, null, userEventService, null, null, null, userDao, deleteInfoDao, null
    );
  }

  /**
   * Score меньше нуля.
   */
  @Test(expected = IllegalArgumentException.class)
  public void InvalidScoreMin() {
    // given

    // when
    topicService.deleteWithBonus(null, null, "reason", -1);
    // then
  }

  /**
   * Максимальное значение score превышено.
   */
  @Test(expected = IllegalArgumentException.class)
  public void InvalidScoreMax() {
    // given
    when(user.isModerator()).thenReturn(true);

    // when
    topicService.deleteWithBonus(null, null, "reason", 21);
    // then
  }

  /**
   * Модератор пытается удалить собственное сообщение со снятием score.
   */
  @Test
  public void userIsModeratorAndBonusNotZeroAndItsOwnTopic() {
    // given
    when(user.isModerator()).thenReturn(true);
    when(user.getId()).thenReturn(1234);
    // собственное сообщение
    when(message.getUid()).thenReturn(1234);
    when(message.getId()).thenReturn(11112234);

    // when
    topicService.deleteWithBonus(message, user, "reason", 20);

    // then
    verifyZeroInteractions(userDao);
    verify(topicDao).delete(eq(11112234));
    verify(deleteInfoDao).insert(eq(11112234), eq(user), eq("reason"), eq(0));
    verify(userEventService).processTopicDeleted(eq(11112234));
  }

  /**
   * Пользователь каким-то образом умудрился удалять собственное сообщение со снятием score
   */
  @Test
  public void userIsNotModeratorAndBonusNotZeroAndItsOwnTopic() {
    // given
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(1234);
    // собственное сообщение
    when(message.getUid()).thenReturn(1234);
    when(message.getId()).thenReturn(11112234);

    // when
    topicService.deleteWithBonus(message, user, "reason", 20);

    // then
    verifyZeroInteractions(userDao);
    verify(topicDao).delete(eq(11112234));
    verify(deleteInfoDao).insert(eq(11112234), eq(user), eq("reason"), eq(0));
    verify(userEventService).processTopicDeleted(eq(11112234));
  }

  /**
   * Модератор пытается удалить чужое сообщение со снятием score.
   */
  @Test
  public void userIsModeratorAndBonusNotZeroAndItsNotOwnTopic() {
    // given
    when(user.isModerator()).thenReturn(true);
    when(user.getId()).thenReturn(1234);
    // чужое сообщение
    when(message.getUid()).thenReturn(5678);
    when(message.getId()).thenReturn(11112234);

    // when
    topicService.deleteWithBonus(message, user, "reason", 20);

    // then
    verify(userDao).changeScore(eq(5678), eq(-20));
    verify(topicDao).delete(eq(11112234));
    verify(deleteInfoDao).insert(eq(11112234), eq(user), eq("reason"), eq(-20));
    verify(userEventService).processTopicDeleted(eq(11112234));
  }

  /**
   * Пользователь пытается удалить чужое сообщение со снятием score
   * FIXME: и у пользователя  получается удалить, хоть и без score!
   */
  @Test
  public void userIsNotModeratorAndBonusNotZeroAndItsNotOwnTopic() {
    // given
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(1234);
    // чужое сообщение
    when(message.getUid()).thenReturn(5678);
    when(message.getId()).thenReturn(11112234);

    // when
    topicService.deleteWithBonus(message, user, "reason", 20);

    // then
    verifyZeroInteractions(userDao);
    verify(topicDao).delete(eq(11112234));
    verify(deleteInfoDao).insert(eq(11112234), eq(user), eq("reason"), eq(0));
    verify(userEventService).processTopicDeleted(eq(11112234));
  }

}
