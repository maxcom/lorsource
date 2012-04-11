package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.PreparedTopic;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Service
public class GroupPermissionService {
  private SectionService sectionService;

  @Autowired
  public void setSectionService(SectionService sectionService) {
    this.sectionService = sectionService;
  }

  private int getEffectivePostscore(Group group) {
    Section section;

    try {
      section = sectionService.getSection(group.getSectionId());
    } catch (SectionNotFoundException e) {
      throw new RuntimeException("bad section", e);
    }

    return Math.max(group.getTopicRestriction(), section.getTopicsRestriction());
  }

  public boolean isTopicPostingAllowed(Group group, User currentUser) {
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

  public String getPostScoreInfo(Group group) {
    int postscore = getEffectivePostscore(group);

    switch (postscore) {
      case TopicPermissionService.POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(500, 500);
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
  private boolean isDeletableByUser(Topic topic, User user) {
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Timestamp hourDeltaTime = new Timestamp(calendar.getTimeInMillis());

    return (topic.getPostdate().compareTo(hourDeltaTime) >= 0 && topic.getUid() == user.getId());
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

    Section section;

    try {
      section = sectionService.getSection(topic.getSectionId());
    } catch (SectionNotFoundException e) {
      throw new RuntimeException(e);
    }

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
   * Можно ли редактировать сообщения полностью
   *
   * @param topic тема
   * @param by редактор
   * @return true если можно, false если нет
   */
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
      return false;
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

  /**
   * Можно ли редактировать теги сообщения
   *
   * @param topic тема
   * @param by редактор
   * @return true если можно, false если нет
   */
  public boolean isTagsEditable(PreparedTopic topic, User by) {
    Topic message = topic.getMessage();
    Section section = topic.getSection();
    User author = topic.getAuthor();

    if (message.isDeleted()) {
      return false;
    }

    if (by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (by.isAdministrator()) {
      return true;
    }

    if (by.isModerator()) {
      return true;
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
