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

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import ru.org.linux.section.Section;
import ru.org.linux.user.User;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Service
public class TopicPermissionService {
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(500, 500);
      case POSTSCORE_MOD_AUTHOR:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore;
    }
  }

  public void checkCommentsAllowed(Topic topic, User user, Errors errors) {
    if (topic.isDeleted()) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению");
      return;
    }

    if (topic.isExpired()) {
      errors.reject(null, "Сообщение уже устарело");
      return;
    }

    if (!isCommentsAllowed(topic, user)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
  }
  
  public boolean isCommentsAllowed(Topic topic, User user) {
    if (user != null && user.isBlocked()) {
      return false;
    }

    if (topic.isDeleted() || topic.isExpired()) {
      return false;
    }

    int score = topic.getPostScore();

    if (score == POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (user == null || user.isAnonymous()) {
      return false;
    }

    if (user.isModerator()) {
      return true;
    }

    if (score == POSTSCORE_REGISTERED_ONLY) {
      return true;
    }

    if (score == POSTSCORE_MODERATORS_ONLY) {
      return false;
    }

    boolean isAuthor = user.getId() == topic.getUid();

    if (score == POSTSCORE_MOD_AUTHOR) {
      return isAuthor;
    }

    if (isAuthor) {
      return true;
    } else {
      return user.getScore() >= score;
    }
  }  
  
  /**
   * Проверка, может ли модератор удалить топик
   * @param user пользователь удаляющий сообщение
   * @param section местоположение топика
   * @return признак возможности удаления
   */
  public boolean isDeletableByModerator(Topic topic, User user, Section section) {
    // TODO убрать от сюда аргумент функции section
    if(!user.isModerator()) {
      return false;
    }
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    Timestamp monthDeltaTime = new Timestamp(calendar.getTimeInMillis());

    boolean ret = false;

    // Если раздел премодерируемый и топик не подтвержден удалять можно
    if(section.isPremoderated() && !topic.isCommited()) {
      ret = true;
    }

    // Если раздел премодерируемый, топик подтвержден и прошло меньше месяца с подтверждения удалять можно
    if(section.isPremoderated() && topic.isCommited() && topic.getPostdate().compareTo(monthDeltaTime) >= 0) {
      ret = true;
    }

    // Если раздел не премодерируем, удалять можно
    if(!section.isPremoderated()) {
      ret = true;
    }

    return ret;
  }
  
  /**
   * Проверка может ли пользователь удалить топик
   * @param user пользователь удаляющий сообщение
   * @return признак возможности удаления
   */
  public boolean isDeletableByUser(Topic topic, User user) {
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Timestamp hourDeltaTime = new Timestamp(calendar.getTimeInMillis());

    return (topic.getPostdate().compareTo(hourDeltaTime) >= 0 && topic.getUid() == user.getId());
  }
  
  public boolean isEditable(PreparedTopic topic, User by) {
    Topic message = topic.getMessage();
    Section section = topic.getSection();
    User author = topic.getAuthor();
    
    if (message.isDeleted()) {
      return false;
    }

    if (by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (message.isExpired()) {
      return by.isModerator() && section.isPremoderated();
    }

    if (by.isModerator()) {
      if (author.isModerator()) {
        return true;
      }

      return section.isPremoderated();
    }

    if (!topic.isLorcode()) {
      return false;
    }

    if (by.canCorrect() && section.isPremoderated()) {
      return true;
    }

    if (by.getId()==author.getId() && !message.isCommited()) {
      return message.isSticky() || section.isPremoderated() || (System.currentTimeMillis() - message.getPostdate().getTime()) < PreparedTopic.EDIT_PERIOD;
    }

    return false;
  }  
}
