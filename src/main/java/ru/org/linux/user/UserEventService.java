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

package ru.org.linux.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserEventService {
  private static final Log logger = LogFactory.getLog(UserEventService.class);

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private UserEventDao userEventDao;


  /**
   * @param events      список событий
   * @param readMessage возвращать ли отрендеренное содержимое уведомлений (используется только для RSS)
   * @param secure      является ли текущие соединение https
   * @return
   */
  public List<PreparedUserEvent> prepare(List<UserEvent> events, boolean readMessage, boolean secure) {
    List<PreparedUserEvent> prepared = new ArrayList<PreparedUserEvent>(events.size());

    for (UserEvent event : events) {
      String text;
      if (readMessage) {
        MessageText messageText;

        if (event.isComment()) {
          messageText = msgbaseDao.getMessageText(event.getCid());
        } else { // Топик
          messageText = msgbaseDao.getMessageText(event.getMsgid());
        }

        text = lorCodeService.prepareTextRSS(messageText.getText(), secure, messageText.isLorcode());
      } else {
        text = null;
      }

      User commentAuthor;

      if (event.isComment()) {
        try {
          commentAuthor = userDao.getUserCached(event.getCommentAuthor());
        } catch (UserNotFoundException e) {
          throw new RuntimeException(e);
        }
      } else {
        commentAuthor = null;
      }

      prepared.add(new PreparedUserEvent(event, text, commentAuthor));
    }

    return prepared;
  }

  /**
   * Добавление уведомления об упоминании пользователей в комментарии.
   *
   * @param users     список пользователей. которых надо оповестить
   * @param topicId   идентификационный номер топика
   * @param commentId идентификационный номер комментария
   */
  public void addUserRefEvent(User[] users, int topicId, int commentId) {
    for (User user : users) {
      userEventDao.addEvent(
        UserEventFilterEnum.REFERENCE.getType(),
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
  public void addUserRefEvent(User[] users, int topicId) {
    for (User user : users) {
      userEventDao.addEvent(
        UserEventFilterEnum.REFERENCE.getType(),
        user.getId(),
        false,
        topicId,
        null,
        null
      );
    }
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
      UserEventFilterEnum.ANSWERS.getType(),
      parentAuthor.getId(),
      false,
      topicId,
      commentId,
      null
    );
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
    if (eventFilter != UserEventFilterEnum.ALL) {
      eventFilterType = eventFilter.getType();
    }
    return userEventDao.getRepliesForUser(user.getId(), showPrivate, topics, offset, eventFilterType);
  }

  /**
   * Сброс уведомлений.
   *
   * @param user пользователь которому сбрасываем
   */
  public void resetUnreadReplies(User user) {
    userEventDao.resetUnreadReplies(user.getId());
  }
}
