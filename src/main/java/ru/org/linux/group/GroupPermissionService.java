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

package ru.org.linux.group;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.PreparedTopic;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Service
public class GroupPermissionService {
  private static final int EDIT_SELF_ALWAYS_SCORE = 300;
  private static final Duration DELETE_PERIOD = Duration.standardHours(6);
  private static final Duration EDIT_PERIOD = DELETE_PERIOD.multipliedBy(2);
  private static final int CREATE_TAG_SCORE = 400;

  private SectionService sectionService;

  @Autowired
  public void setSectionService(SectionService sectionService) {
    this.sectionService = sectionService;
  }

  private int getEffectivePostscore(@Nonnull Group group) {
    Section section = sectionService.getSection(group.getSectionId());

    return Math.max(group.getTopicRestriction(), section.getTopicsRestriction());
  }

  public boolean isTopicPostingAllowed(@Nonnull Group group, @Nullable User currentUser) {
    int restriction = getEffectivePostscore(group);

    if (restriction == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (currentUser==null || currentUser.isAnonymous()) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (restriction==TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      return currentUser.isModerator();
    } else {
      return currentUser.getScore() >= restriction;
    }
  }

  public boolean isImagePostingAllowed(@Nonnull Section section, @Nullable User currentUser) {
    if (section.isImagepost()) {
      return true;
    }

    if (currentUser!=null && currentUser.isAdministrator()) {
      return section.isImageAllowed();
    }

    return false;
  }

  public String getPostScoreInfo(Group group) {
    int postscore = getEffectivePostscore(group);

    switch (postscore) {
      case TopicPermissionService.POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
      case 200:
      case 300:
      case 400:
      case 500:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(postscore, postscore, true);
      case TopicPermissionService.POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на добавление сообщений</b>: только для модераторов";
      case TopicPermissionService.POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей, score>=" + postscore;
    }
  }

  public boolean isDeletable(Topic topic, User user) {
    boolean perm = isDeletableByUser(topic, user);

    if (!perm && user.isModerator()) {
      perm = isDeletableByModerator(topic, user);
    }

    if (!perm) {
      return user.isAdministrator();
    }

    return perm;
  }

  /**
   * Проверка может ли пользователь удалить топик
   * @param user пользователь удаляющий сообщение
   * @return признак возможности удаления
   */
  private static boolean isDeletableByUser(Topic topic, User user) {
    if (topic.getUid() != user.getId()) {
      return false;
    }

    if (topic.isDraft()) {
      return true;
    }

    DateTime deleteDeadline = new DateTime(topic.getPostDate()).plus(DELETE_PERIOD);

    return (
        deleteDeadline.isAfterNow() &&
        topic.getCommentCount() == 0
    );
  }

  /**
   * Проверка, может ли модератор удалить топик
   * @param user пользователь удаляющий сообщение
   * @return признак возможности удаления
   */
  private boolean isDeletableByModerator(Topic topic, User user) {
    if(!user.isModerator()) {
      return false;
    }

    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    Timestamp monthDeltaTime = new Timestamp(calendar.getTimeInMillis());

    boolean ret = false;

    Section section = sectionService.getSection(topic.getSectionId());

    // Если раздел премодерируемый и топик не подтвержден удалять можно
    if(section.isPremoderated() && !topic.isCommited()) {
      ret = true;
    }

    // Если раздел премодерируемый, топик подтвержден и прошло меньше месяца с подтверждения удалять можно
    if(section.isPremoderated() && topic.isCommited() && topic.getPostDate().compareTo(monthDeltaTime) >= 0) {
      ret = true;
    }

    // Если раздел не премодерируем, удалять можно
    if(!section.isPremoderated()) {
      ret = true;
    }

    return ret;
  }

  /**
   * Можно ли редактировать сообщения полностью
   *
   * @param topic тема
   * @param by редактор
   * @return true если можно, false если нет
   */
  public boolean isEditable(@Nonnull PreparedTopic topic, @Nullable User by) {
    Topic message = topic.getMessage();
    Section section = topic.getSection();
    User author = topic.getAuthor();

    if (message.isDeleted()) {
      return false;
    }

    if (by==null || by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (message.isExpired()) {
      return false;
    }

    if (by.isAdministrator()) {
      return true;
    }

    if (!topic.isLorcode()) {
      return false;
    }

    if (by.isModerator()) {
      return true;
    }

    if (by.canCorrect() && section.isPremoderated()) {
      return true;
    }

    if (by.getId()==author.getId() && !message.isCommited()) {
      if (message.isSticky()) {
        return true;
      }

      if (section.isPremoderated()) {
        return true;
      }

      if (message.isDraft()) {
        return true;
      }

      if (author.getScore()>=EDIT_SELF_ALWAYS_SCORE) {
        return !message.isExpired();
      }

      DateTime editDeadline = new DateTime(message.getPostDate()).plus(EDIT_PERIOD);

      return editDeadline.isAfterNow();
    }

    return false;
  }

  /**
   * Можно ли редактировать теги сообщения
   *
   * @param topic тема
   * @param by редактор
   * @return true если можно, false если нет
   */
  public boolean isTagsEditable(@Nonnull PreparedTopic topic, @Nullable User by) {
    Topic message = topic.getMessage();
    Section section = topic.getSection();
    User author = topic.getAuthor();

    if (message.isDeleted()) {
      return false;
    }

    if (by==null || by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (by.isAdministrator()) {
      return true;
    }

    if (by.isModerator()) {
      return true;
    }

    if (by.canCorrect()) {
      return true;
    }

    if (by.getId()==author.getId() && !message.isCommited()) {
      if (message.isSticky()) {
        return true;
      }

      if (section.isPremoderated()) {
        return true;
      }

      if (author.getScore()>=EDIT_SELF_ALWAYS_SCORE) {
        return !message.isExpired();
      }

      DateTime editDeadline = new DateTime(message.getPostDate()).plus(EDIT_PERIOD);

      return editDeadline.isAfterNow();
    }

    return false;
  }

  public boolean canCreateTag(Section section, User user) {
    if (section.isPremoderated()) {
      return true;
    }

    return user!=null && user.getScore()>=CREATE_TAG_SCORE;
  }
}
