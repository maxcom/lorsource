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

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static ru.org.linux.user.UserEventFilterEnum.*;

@Service
public class UserEventService {
  private static final Logger logger = LoggerFactory.getLogger(UserEventService.class);

  @Autowired
  private UserEventDao userEventDao;

  /**
   * Добавление уведомления об упоминании пользователей в комментарии.
   *
   * @param users     список пользователей. которых надо оповестить
   * @param topicId   идентификационный номер топика
   * @param commentId идентификационный номер комментария
   */
  public void addUserRefEvent(Iterable<User> users, int topicId, int commentId) {
    for (User user : users) {
      userEventDao.addEvent(
        REFERENCE.getType(),
        user.getId(),
        false,
        topicId,
        commentId,
        null
      );
    }
  }

  /**
   * Добавление уведомления об упоминании пользователей в топике.
   *
   * @param users   список пользователей. которых надо оповестить
   * @param topicId идентификационный номер топика
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void addUserRefEvent(Iterable<Integer> users, int topicId) {
    userEventDao.insertTopicNotification(topicId, users);

    for (int user : users) {
      userEventDao.addEvent(
        REFERENCE.getType(),
        user,
        false,
        topicId,
        null,
        null
      );
    }
  }

  public Set<Integer> getNotifiedUsers(int topicId) {
    return ImmutableSet.copyOf(userEventDao.getNotifiedUsers(topicId));
  }

  /**
   * Добавление уведомления об ответе на сообщение пользователя.
   *
   * @param parentAuthor
   * @param topicId
   * @param commentId
   */
  public void addReplyEvent(User parentAuthor, int topicId, int commentId) {
    userEventDao.addEvent(
      ANSWERS.getType(),
      parentAuthor.getId(),
      false,
      topicId,
      commentId,
      null
    );
  }

  /**
   * Добавление уведомления о назначении тега сообщению.
   *
   * @param userIdList  список ID пользователей, которых надо оповестить
   * @param topicId     идентификационный номер топика
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void addUserTagEvent(Iterable<Integer> userIdList, int topicId) {
    userEventDao.insertTopicNotification(topicId, userIdList);

    for (int userId : userIdList) {
      userEventDao.addEvent(
        TAG.getType(),
        userId,
        false,
        topicId,
        null,
        null
      );
    }
  }

  /**
   * Очистка старых уведомлений пользователей.
   *
   * @param maxEventsPerUser максимальное количество уведомлений для одного пользователя
   */
  public void cleanupOldEvents(final int maxEventsPerUser) {
    List<Integer> oldEventsList = userEventDao.getUserIdListByOldEvents(maxEventsPerUser);

    for (int userId : oldEventsList) {
      logger.info("Cleaning up messages for userid=" + userId);
      userEventDao.cleanupOldEvents(userId, maxEventsPerUser);
    }
  }

  /**
   * Получить список уведомлений для пользователя.
   *
   * @param user        пользователь
   * @param showPrivate включать ли приватные
   * @param topics      кол-во уведомлений
   * @param offset      сдвиг относительно начала
   * @param eventFilter тип уведомлений
   * @return список уведомлений
   */
  public List<UserEvent> getRepliesForUser(User user, boolean showPrivate, int topics, int offset,
                                           UserEventFilterEnum eventFilter) {
    String eventFilterType = null;
    if (eventFilter != ALL) {
      eventFilterType = eventFilter.getType();
    }
    return userEventDao.getRepliesForUser(user.getId(), showPrivate, topics, offset, eventFilterType);
  }

  /**
   * Сброс уведомлений.
   *
   * @param user пользователь которому сбрасываем
   * @param topId
   */
  public void resetUnreadReplies(User user, int topId) {
    userEventDao.resetUnreadReplies(user.getId(), topId);
  }

  /**
   * Удаление уведомлений, относящихся к удаленным топикам
   *
   * @param msgids идентификаторы топиков
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void processTopicDeleted(Collection<Integer> msgids) {
    userEventDao.recalcEventCount(userEventDao.deleteTopicEvents(msgids));
  }

  /**
   * Удаление уведомлений, относящихся к удаленным комментариям
   *
   * @param msgids идентификаторы комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void processCommentsDeleted(List<Integer> msgids) {
    userEventDao.recalcEventCount(userEventDao.deleteCommentEvents(msgids));
  }
}
